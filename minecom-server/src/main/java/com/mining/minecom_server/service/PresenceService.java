package com.mining.minecom_server.service;

import com.mining.minecom_server.common.dto.PresenceUpdate;
import com.mining.minecom_server.model.UserEntity;
import com.mining.minecom_server.repository.UserRepository;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

@Service
public class PresenceService {

    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;

    public PresenceService(UserRepository userRepository, SimpMessagingTemplate messagingTemplate) {
        this.userRepository = userRepository;
        this.messagingTemplate = messagingTemplate;
    }

    /**
     * Met à jour le statut en ligne et l'heure de la dernière activité de l'utilisateur.
     * Cette méthode est principalement utilisée par le PresenceFilter (requêtes HTTP/REST)
     */
    @Transactional
    public void updatePresence(String username) {
        userRepository.findByUsername(username).ifPresent(user -> {
            boolean statusChanged = false;

            // 1. Mise à jour de la dernière activité
            user.setLastActivity(Instant.now());

            // 2. Si l'utilisateur était hors ligne, le mettre en ligne
            if (user.getIsOnline() == null || !user.getIsOnline()) {
                user.setIsOnline(true);
                statusChanged = true;
            }

            // Nous ne faisons pas de broadcast ici pour éviter les doubles envois avec la connexion STOMP.
            if (statusChanged) {
                System.out.println("PRESENCE UPDATE HTTP: L'utilisateur " + username + " est maintenant EN LIGNE.");
            }
        });
    }

    /**
     * 🔑 MÉTHODE CLÉ pour la gestion STOMP (appelée par WebSocketPresenceListener).
     * Met à jour le statut en ligne dans la DB et publie l'événement de présence.
     * * @param username Le nom de l'utilisateur.
     * @param isOnline Vrai pour connexion, Faux pour déconnexion.
     */
    @Transactional
    public void setUserOnline(String username, boolean isOnline) {
        // Recherche de l'utilisateur
        Optional<UserEntity> userOptional = userRepository.findByUsername(username);

        userOptional.ifPresent(user -> {

            // Vérifie si le statut change réellement pour éviter les mises à jour et broadcasts inutiles
            if (user.getIsOnline() != null && user.getIsOnline().equals(isOnline)) {
                System.out.println("PRESENCE STOMP: Statut inchangé pour " + username);
                return;
            }

            // Mise à jour de l'état dans l'entité
            user.setIsOnline(isOnline);
            user.setLastActivity(Instant.now());

            // Si vous n'utilisez pas JpaRepository, ajoutez user.setUpdated(Instant.now())

            System.out.println("PRESENCE STOMP: Mise à jour DB pour " + username + " -> " + (isOnline ? "EN LIGNE" : "HORS LIGNE"));

            // Publication de l'événement pour les autres clients
            broadcastPresenceUpdate(user.getId(), user.getUsername(), isOnline);
        });
    }

    /**
     * Publie l'événement de présence sur le canal public.
     */
    private void broadcastPresenceUpdate(Long userId, String username, boolean isOnline) {
        PresenceUpdate update = new PresenceUpdate(userId, username, isOnline);

        // Envoi vers la destination /topic/public/presence
        messagingTemplate.convertAndSend("/topic/public/presence", update);
    }

}