package com.mining.minecom_server.service;

import io.minio.*;
import io.minio.errors.*;
import io.minio.http.Method;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.concurrent.TimeUnit;

@Service
public class StorageService {

    @Autowired
    private MinioClient minioClient;

    @Value("${minio.bucket}")
    private String bucket;

    @Value("${minio.url}")
    private String minioUrl;

    // ================================================================
    // 🔑 UPLOAD : Valider + nommer + stocker
    // ================================================================
    public StorageResult uploadFile(MultipartFile file, Long userId) throws Exception {

        // 1. Validation du fichier
        validateFile(file);

        // 2. Nom unique professionnel : userId_timestamp_filename
        String originalFilename = sanitizeFilename(file.getOriginalFilename());
        String uniqueFilename = userId + "_" + System.currentTimeMillis() + "_" + originalFilename;

        // 3. Organiser par type dans des dossiers
        String folder = getFolder(file.getContentType());
        String objectName = folder + "/" + uniqueFilename;

        // 4. Vérifier/créer le bucket
        ensureBucketExists();

        // 5. Upload vers MinIO
        try (InputStream inputStream = file.getInputStream()) {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucket)
                            .object(objectName)
                            .stream(inputStream, file.getSize(), -1)
                            .contentType(file.getContentType())
                            .build()
            );
        }

        // 6. Générer l'URL d'accès (présignée ou publique)
        String fileUrl = generateUrl(objectName);

        System.out.println("✅ Fichier uploadé dans MinIO : " + objectName);

        return new StorageResult(
                fileUrl,
                objectName,
                file.getOriginalFilename(),
                file.getContentType(),
                file.getSize()
        );
    }

    // ================================================================
    // 🔑 SUPPRESSION d'un fichier dans MinIO
    // ================================================================
    public void deleteFile(String objectName) {
        try {
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(bucket)
                            .object(objectName)
                            .build()
            );
            System.out.println("🗑️ Fichier supprimé de MinIO : " + objectName);
        } catch (Exception e) {
            System.err.println("⚠️ Erreur suppression MinIO : " + e.getMessage());
        }
    }

    // ================================================================
    // 🔑 TÉLÉCHARGEMENT : Retourner le stream du fichier
    // ================================================================
    public InputStream downloadFile(String objectName) throws Exception {
        return minioClient.getObject(
                GetObjectArgs.builder()
                        .bucket(bucket)
                        .object(objectName)
                        .build()
        );
    }

    // ================================================================
    // 🔑 RÉFÉRENCE STABLE ↔ URL D'ACCÈS
    // ================================================================

    /**
     * Ramène une URL présignée (ou une clé déjà nettoyée) à la clé de l'objet
     * dans le bucket, ex: "images/4_1712345678_photo.jpg".
     * C'est cette clé qu'on persiste : une URL présignée expire après 7 jours.
     */
    public String toObjectName(String urlOrObjectName) {
        if (urlOrObjectName == null || urlOrObjectName.isBlank()) return null;
        if (!urlOrObjectName.startsWith("http://") && !urlOrObjectName.startsWith("https://")) {
            return urlOrObjectName;
        }
        try {
            String path = java.net.URI.create(urlOrObjectName).getPath(); // déjà décodé
            String prefix = "/" + bucket + "/";
            if (path != null && path.startsWith(prefix)) {
                return path.substring(prefix.length());
            }
        } catch (IllegalArgumentException e) {
            System.err.println("⚠️ URL de fichier invalide : " + urlOrObjectName);
        }
        return urlOrObjectName;
    }

    /**
     * Génère une URL présignée fraîche pour une clé persistée.
     * Accepte aussi les anciennes lignes qui contiennent une URL complète.
     */
    public String resolveFileUrl(String storedValue) {
        String objectName = toObjectName(storedValue);
        if (objectName == null) return null;
        try {
            return generateUrl(objectName);
        } catch (Exception e) {
            System.err.println("⚠️ Impossible de générer l'URL pour " + objectName + " : " + e.getMessage());
            return storedValue;
        }
    }

    // ================================================================
    // VALIDATION PROFESSIONNELLE
    // ================================================================
    private void validateFile(MultipartFile file) throws Exception {
        if (file == null || file.isEmpty()) {
            throw new Exception("Fichier vide");
        }

        // Taille max : 20MB
        long maxSize = 20 * 1024 * 1024;
        if (file.getSize() > maxSize) {
            throw new Exception("Fichier trop volumineux. Max: 20MB. Votre fichier: " +
                    formatSize(file.getSize()));
        }

        // Types autorisés uniquement
        String contentType = file.getContentType();
        if (!isAllowedType(contentType)) {
            throw new Exception("Type de fichier non autorisé: " + contentType +
                    ". Types acceptés: images (JPG, PNG, GIF, WebP) et PDF");
        }

        System.out.println("✅ Validation OK - Fichier: " + file.getOriginalFilename() +
                " | Type: " + contentType + " | Taille: " + formatSize(file.getSize()));
    }

    private boolean isAllowedType(String contentType) {
        if (contentType == null) return false;
        return contentType.equals("image/jpeg") ||
                contentType.equals("image/png")  ||
                contentType.equals("image/gif")  ||
                contentType.equals("image/webp") ||
                contentType.equals("application/pdf");
    }

    // Organiser par dossier selon le type
    private String getFolder(String contentType) {
        if (contentType != null && contentType.startsWith("image/")) return "images";
        if (contentType != null && contentType.equals("application/pdf")) return "documents";
        return "files";
    }

    // Nettoyer le nom de fichier (supprimer les caractères dangereux)
    private String sanitizeFilename(String filename) {
        if (filename == null) return "file";
        return filename.replaceAll("[^a-zA-Z0-9._-]", "_").toLowerCase();
    }

    // Générer URL d'accès (présignée 7 jours)
    private String generateUrl(String objectName) throws Exception {
        return minioClient.getPresignedObjectUrl(
                GetPresignedObjectUrlArgs.builder()
                        .method(Method.GET)
                        .bucket(bucket)
                        .object(objectName)
                        .expiry(7, TimeUnit.DAYS)
                        .build()
        );
    }

    // Vérifier/créer le bucket
    private void ensureBucketExists() throws Exception {
        boolean exists = minioClient.bucketExists(
                BucketExistsArgs.builder().bucket(bucket).build()
        );
        if (!exists) {
            minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
            System.out.println("✅ Bucket créé : " + bucket);
        }
    }

    private String formatSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return (bytes / 1024) + " KB";
        return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
    }

    // ================================================================
    // RÉSULTAT D'UPLOAD
    // ================================================================
    public static class StorageResult {
        private final String url;
        private final String objectName; // chemin dans MinIO (pour suppression)
        private final String originalFilename;
        private final String contentType;
        private final long size;

        public StorageResult(String url, String objectName, String originalFilename,
                             String contentType, long size) {
            this.url = url;
            this.objectName = objectName;
            this.originalFilename = originalFilename;
            this.contentType = contentType;
            this.size = size;
        }

        public String getUrl() { return url; }
        public String getObjectName() { return objectName; }
        public String getOriginalFilename() { return originalFilename; }
        public String getContentType() { return contentType; }
        public long getSize() { return size; }
    }
}