package com.mining.minecom_server.controller;

import com.mining.minecom_server.common.dto.SOSAlertDto;
import com.mining.minecom_server.common.dto.SOSAcknowledgmentDto;
import com.mining.minecom_server.service.SOSAlertService;
import com.mining.minecom_server.model.UserEntity;
import com.mining.minecom_server.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/sos")
public class SOSAlertController {

    @Autowired
    private SOSAlertService sosAlertService;

    @Autowired
    private UserRepository userRepository;

    /**
     * 🚨 CRÉER UNE ALERTE SOS (avec récupération correcte du userId)
     */
    @PostMapping("/alert")
    public ResponseEntity<SOSAlertDto> createAlert(
            @RequestBody Map<String, String> alertData,  // ✅ Recevoir JSON
            Authentication authentication) {

        try {
            // ✅ Récupérer l'utilisateur connecté depuis le token JWT
            String username = authentication.getName();

            UserEntity user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé : " + username));

            Long userId = user.getId();

            String location = alertData.getOrDefault("location", "");
            String description = alertData.getOrDefault("description", "");

            System.out.println("🚨 Création alerte SOS par " + username + " (ID: " + userId + ")");
            System.out.println("   Location: " + location);
            System.out.println("   Description: " + description);

            SOSAlertDto alert = sosAlertService.createAlert(userId, location, description);

            return ResponseEntity.ok(alert);

        } catch (Exception e) {
            System.err.println("❌ Erreur création alerte SOS: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * ✅ ACCUSER RÉCEPTION D'UNE ALERTE
     */
    @PostMapping("/acknowledge/{alertId}")
    public ResponseEntity<SOSAcknowledgmentDto> acknowledgeAlert(
            @PathVariable Long alertId,
            Authentication authentication) {

        try {
            String username = authentication.getName();

            UserEntity user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé : " + username));

            Long userId = user.getId();

            System.out.println("✅ Accusé SOS reçu de " + username + " pour alerte #" + alertId);

            SOSAcknowledgmentDto ack = sosAlertService.acknowledgeAlert(alertId, userId);

            return ResponseEntity.ok(ack);

        } catch (Exception e) {
            System.err.println("❌ Erreur accusé SOS: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 📋 RÉCUPÉRER L'ALERTE ACTIVE
     */
    @GetMapping("/active")
    public ResponseEntity<SOSAlertDto> getActiveAlert(Authentication authentication) {
        SOSAlertDto alert = sosAlertService.getActiveAlert();

        if (alert != null) {
            // 🔑 NOUVEAU : Vérifier si l'utilisateur connecté a déjà accusé réception
            if (authentication != null) {
                String username = authentication.getName();
                UserEntity user = userRepository.findByUsername(username).orElse(null);
                if (user != null) {
                    boolean hasAcknowledged = sosAlertService.hasUserAcknowledged(alert.getId(), user.getId());
                    alert.setHasAcknowledged(hasAcknowledged);
                    System.out.println("🔍 Alerte active pour " + username + " - déjà accusé: " + hasAcknowledged);
                }
            }
            return ResponseEntity.ok(alert);
        }
        return ResponseEntity.noContent().build();
    }

    /**
     * 🔚 DÉSACTIVER UNE ALERTE
     */
    @PostMapping("/deactivate/{alertId}")
    public ResponseEntity<Void> deactivateAlert(
            @PathVariable Long alertId,
            Authentication authentication) {

        try {
            sosAlertService.deactivateAlert(alertId);
            System.out.println("🔚 Alerte SOS #" + alertId + " désactivée");
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            System.err.println("❌ Erreur désactivation alerte: " + e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }
}