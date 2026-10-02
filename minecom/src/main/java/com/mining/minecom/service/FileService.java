package com.mining.minecom.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.stage.FileChooser;
import javafx.stage.Window;

import java.io.*;
import java.net.URI;
import java.net.http.*;
import java.nio.file.*;

public class FileService {

    private static final String SERVER_URL = "http://localhost:8080";
    private static final String UPLOAD_URL  = SERVER_URL + "/api/files/upload";

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper mapper = new ObjectMapper();

    // ================================================================
    // 🔑 OUVRIR LE SÉLECTEUR DE FICHIERS
    // ================================================================
    public File openImageChooser(Window window) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Choisir une image");
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Images", "*.jpg", "*.jpeg", "*.png", "*.gif", "*.webp")
        );
        return chooser.showOpenDialog(window);
    }

    public File openPdfChooser(Window window) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Choisir un document PDF");
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Documents PDF", "*.pdf")
        );
        return chooser.showOpenDialog(window);
    }

    // ================================================================
    // 🔑 UPLOAD vers le serveur (multipart)
    // ================================================================
    public FileUploadResult uploadFile(File file) throws Exception {
        String jwtToken = AuthService.getCurrentJwtToken();
        if (jwtToken == null) throw new Exception("Token JWT manquant");

        // Lire le fichier
        byte[] fileBytes = Files.readAllBytes(file.toPath());
        String mimeType  = detectMimeType(file);
        String boundary  = "MineCom_" + System.currentTimeMillis();

        // Construire le multipart body
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        // Part header
        String partHeader =
                "--" + boundary + "\r\n" +
                        "Content-Disposition: form-data; name=\"file\"; filename=\"" + file.getName() + "\"\r\n" +
                        "Content-Type: " + mimeType + "\r\n\r\n";
        baos.write(partHeader.getBytes());
        baos.write(fileBytes);
        baos.write(("\r\n--" + boundary + "--\r\n").getBytes());

        byte[] body = baos.toByteArray();

        System.out.println("📤 Upload: " + file.getName() +
                " (" + formatSize(file.length()) + ") → " + UPLOAD_URL);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(new URI(UPLOAD_URL))
                .header("Authorization", "Bearer " + jwtToken)
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .POST(HttpRequest.BodyPublishers.ofByteArray(body))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 200) {
            FileUploadResult result = mapper.readValue(response.body(), FileUploadResult.class);
            System.out.println("✅ Upload réussi → URL: " + result.getUrl());
            return result;
        } else {
            String errMsg = extractErrorMessage(response.body());
            throw new Exception("Erreur upload (" + response.statusCode() + "): " + errMsg);
        }
    }

    // ================================================================
    // 🔑 TÉLÉCHARGER un PDF pour l'ouvrir localement
    // ================================================================
    public File downloadPdfToTemp(String presignedUrl, String filename) throws Exception {
        System.out.println("📥 Téléchargement PDF: " + presignedUrl);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(new URI(presignedUrl))
                .GET()
                .build();

        HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());

        if (response.statusCode() == 200) {
            // Préfixe lisible (nom d'origine) pour la barre de titre du lecteur PDF
            String base = filename != null ? filename.replaceAll("(?i)\\.pdf$", "") : "document";
            base = base.replaceAll("[^a-zA-Z0-9._-]", "_");
            if (base.length() < 3) base = "minecom_" + base;
            File tempFile = File.createTempFile(base + "_", ".pdf");
            tempFile.deleteOnExit();
            Files.write(tempFile.toPath(), response.body());
            System.out.println("✅ PDF sauvegardé: " + tempFile.getAbsolutePath());
            return tempFile;
        } else {
            // 403 = URL présignée expirée/invalide (signature liée à l'hôte minio.url du serveur)
            throw new Exception("Erreur téléchargement PDF: HTTP " + response.statusCode());
        }
    }

    // ================================================================
    // UTILITAIRES
    // ================================================================
    private String detectMimeType(File file) {
        String name = file.getName().toLowerCase();
        if (name.endsWith(".pdf"))  return "application/pdf";
        if (name.endsWith(".png"))  return "image/png";
        if (name.endsWith(".gif"))  return "image/gif";
        if (name.endsWith(".webp")) return "image/webp";
        return "image/jpeg"; // .jpg, .jpeg par défaut
    }

    private String formatSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return (bytes / 1024) + " KB";
        return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
    }

    private String extractErrorMessage(String json) {
        try {
            var node = mapper.readTree(json);
            return node.has("error") ? node.get("error").asText() : json;
        } catch (Exception e) {
            return json;
        }
    }

    // ================================================================
    // 🔑 RÉSULTAT D'UPLOAD (correspond à la réponse serveur)
    // ================================================================
    public static class FileUploadResult {
        private String url;        // URL présignée MinIO (7 jours)
        private String objectName; // Chemin dans MinIO (ex: images/4_172839_photo.jpg)
        private String filename;   // Nom original
        private String type;       // image/jpeg ou application/pdf
        private String size;       // Taille en octets

        public FileUploadResult() {}

        public String getUrl()        { return url; }
        public String getObjectName() { return objectName; }
        public String getFilename()   { return filename; }
        public String getType()       { return type; }
        public String getSize()       { return size; }

        public void setUrl(String url)               { this.url = url; }
        public void setObjectName(String objectName) { this.objectName = objectName; }
        public void setFilename(String filename)     { this.filename = filename; }
        public void setType(String type)             { this.type = type; }
        public void setSize(String size)             { this.size = size; }

        public boolean isImage() {
            return type != null && type.startsWith("image/");
        }

        public boolean isPdf() {
            return "application/pdf".equals(type);
        }

        public long getSizeAsLong() {
            try { return Long.parseLong(size); } catch (Exception e) { return 0L; }
        }
    }
}