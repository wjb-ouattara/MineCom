package com.mining.minecom_server.controller;

import com.mining.minecom_server.model.UserEntity;
import com.mining.minecom_server.repository.UserRepository;
import com.mining.minecom_server.service.StorageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/files")
public class FileController {

    @Autowired
    private StorageService storageService;

    // UPLOAD : Valider + nommer + stocker dans MinIO + retourner URL
    @PostMapping("/upload")
    public ResponseEntity<Map<String, String>> uploadFile(
            @RequestParam("file") MultipartFile file,
            Authentication authentication) {

        try {
            // Extraire l'ID utilisateur depuis le token
            Long userId = getUserId(authentication.getName());

            // Uploader dans MinIO
            StorageService.StorageResult result = storageService.uploadFile(file, userId);

            // Retourner les infos du fichier
            Map<String, String> response = new HashMap<>();
            response.put("url", result.getUrl());
            response.put("objectName", result.getObjectName()); // Pour suppression future
            response.put("filename", result.getOriginalFilename());
            response.put("type", result.getContentType());
            response.put("size", String.valueOf(result.getSize()));

            System.out.println("✅ Upload réussi pour " + authentication.getName() +
                    " : " + result.getOriginalFilename());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.err.println("❌ Erreur upload : " + e.getMessage());
            Map<String, String> error = new HashMap<>();

            // MinIO injoignable : ce n'est pas la faute du fichier → 503 avec un message clair
            if (isConnectionFailure(e)) {
                error.put("error", "Service de stockage (MinIO) indisponible. "
                        + "Démarrez-le : docker compose -f infra/docker-compose.yml up -d minio");
                return ResponseEntity.status(503).body(error);
            }

            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    private boolean isConnectionFailure(Throwable e) {
        for (Throwable t = e; t != null; t = t.getCause()) {
            if (t instanceof java.net.ConnectException) return true;
        }
        return false;
    }

    //  PROXY : Servir les fichiers via l'API (avec auth JWT)
    // Utilisé uniquement si MinIO n'est pas accessible directement par le client
    @GetMapping("/proxy/{folder}/{filename:.+}")
    public ResponseEntity<InputStreamResource> proxyFile(
            @PathVariable String folder,
            @PathVariable String filename,
            Authentication authentication) {
        try {
            String objectName = folder + "/" + filename;
            var stream = storageService.downloadFile(objectName);

            // Déterminer le type MIME
            String contentType = filename.endsWith(".pdf") ? "application/pdf" : "image/jpeg";

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filename + "\"")
                    .body(new InputStreamResource(stream));

        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    // Récupérer l'ID user depuis le username (simplifiée)
    @Autowired
    private UserRepository userRepository;

    private Long getUserId(String username) {
        return userRepository.findByUsername(username)
                .map(UserEntity::getId)
                .orElseThrow(() -> new RuntimeException("User non trouvé"));
    }
}