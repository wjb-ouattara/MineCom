package com.mining.minecom_server.service;

import com.mining.minecom_server.common.dto.SOSAlertDto;
import com.mining.minecom_server.common.dto.SOSAcknowledgmentDto;
import com.mining.minecom_server.model.SOSAlert;
import com.mining.minecom_server.model.SOSAcknowledgment;
import com.mining.minecom_server.model.UserEntity;
import com.mining.minecom_server.repository.SOSAlertRepository;
import com.mining.minecom_server.repository.SOSAcknowledgmentRepository;
import com.mining.minecom_server.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import com.mining.minecom_server.listener.WebSocketPresenceListener;

import java.time.Instant;
import java.util.Optional;

@Service
public class SOSAlertService {

    @Autowired
    private SOSAlertRepository sosAlertRepository;

    @Autowired
    private SOSAcknowledgmentRepository acknowledgmentRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    private WebSocketPresenceListener webSocketPresenceListener;

    public SOSAlertDto createAlert(Long senderId, String location, String description) {
        UserEntity sender = userRepository.findById(senderId)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        SOSAlert alert = new SOSAlert();
        alert.setSender(sender);
        alert.setLocation(location != null ? location : "Non spécifié");
        alert.setDescription(description != null ? description : "");
        alert.setTimestamp(Instant.now());
        alert.setIsActive(true);

        SOSAlert savedAlert = sosAlertRepository.save(alert);

        // Convertir en DTO
        SOSAlertDto alertDto = convertToDto(savedAlert);

        // Compter les utilisateurs en ligne
        int onlineCount = webSocketPresenceListener.getOnlineUsersCount();
        alertDto.setTotalOnlineUsers(onlineCount);
        alertDto.setAcknowledgedCount(0);

        // Diffuser à TOUS les utilisateurs via WebSocket
        messagingTemplate.convertAndSend("/topic/public/sos-alert", alertDto);

        System.out.println("🚨 ALERTE SOS ENVOYÉE : ID=" + savedAlert.getId() +
                " par " + sender.getUsername() +
                " à " + onlineCount + " utilisateurs en ligne");

        return alertDto;
    }

    public SOSAcknowledgmentDto acknowledgeAlert(Long alertId, Long userId) {
        SOSAlert alert = sosAlertRepository.findById(alertId)
                .orElseThrow(() -> new RuntimeException("Alerte non trouvée"));

        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        // Vérifier si déjà accusé
        Optional<SOSAcknowledgment> existing = acknowledgmentRepository.findByAlertAndUser(alert, user);
        if (existing.isPresent()) {
            return new SOSAcknowledgmentDto(alertId, userId, user.getUsername());
        }

        // Créer l'accusé de réception
        SOSAcknowledgment ack = new SOSAcknowledgment();
        ack.setAlert(alert);
        ack.setUser(user);
        ack.setTimestamp(Instant.now());

        acknowledgmentRepository.save(ack);

        SOSAcknowledgmentDto ackDto = new SOSAcknowledgmentDto(alertId, userId, user.getUsername());

        // Notifier le créateur de l'alerte
        messagingTemplate.convertAndSendToUser(
                alert.getSender().getUsername(),
                "/queue/sos-acknowledgment",
                ackDto
        );

        System.out.println("✅ Accusé de réception SOS : " + user.getUsername() +
                " pour alerte ID=" + alertId);

        return ackDto;
    }

    public SOSAlertDto getActiveAlert() {
        Optional<SOSAlert> alert = sosAlertRepository.findTopByIsActiveTrueOrderByTimestampDesc();

        if (alert.isPresent()) {
            SOSAlertDto dto = convertToDto(alert.get());
            Long ackCount = acknowledgmentRepository.countByAlert(alert.get());
            dto.setAcknowledgedCount(ackCount.intValue());
            dto.setTotalOnlineUsers(webSocketPresenceListener.getOnlineUsersCount());
            return dto;
        }

        return null;
    }

    public void deactivateAlert(Long alertId) {
        SOSAlert alert = sosAlertRepository.findById(alertId)
                .orElseThrow(() -> new RuntimeException("Alerte non trouvée"));

        alert.setIsActive(false);
        sosAlertRepository.save(alert);

        // Notifier tous les utilisateurs que l'alerte est terminée
        messagingTemplate.convertAndSend("/topic/public/sos-deactivated", alertId);

        System.out.println("🔚 Alerte SOS désactivée : ID=" + alertId);
    }

    private SOSAlertDto convertToDto(SOSAlert alert) {
        return new SOSAlertDto(
                alert.getId(),
                alert.getSender().getId(),
                alert.getSender().getUsername(),
                alert.getLocation(),
                alert.getDescription(),
                alert.getTimestamp()
        );
    }

    // Ajoutez cette méthode dans SOSAlertService.java (serveur)

    public boolean hasUserAcknowledged(Long alertId, Long userId) {
        try {
            SOSAlert alert = sosAlertRepository.findById(alertId).orElse(null);
            if (alert == null) return false;

            UserEntity user = userRepository.findById(userId).orElse(null);
            if (user == null) return false;

            return acknowledgmentRepository.findByAlertAndUser(alert, user).isPresent();
        } catch (Exception e) {
            System.err.println("Erreur hasUserAcknowledged: " + e.getMessage());
            return false;
        }
    }
}