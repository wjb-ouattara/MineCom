package com.mining.minecom_server.listener;

import com.mining.minecom_server.service.PresenceService;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.broker.BrokerAvailabilityEvent;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import java.security.Principal;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class WebSocketPresenceListener {

    private final PresenceService presenceService;

    // 🔑 STOCKER LES UTILISATEURS EN LIGNE
    private static final ConcurrentHashMap<String, String> onlineUsers = new ConcurrentHashMap<>();
    // Vous aurez besoin d'un service pour la présence
    public WebSocketPresenceListener(PresenceService presenceService) {
        this.presenceService = presenceService;

    }

    /**
     * Gère l'événement de connexion WebSocket/STOMP.
     * Met l'utilisateur en ligne et publie l'événement de présence.
     */
    @EventListener
    public void handleWebSocketConnectListener(SessionConnectEvent event) {
        // Le Principal est rempli par votre WebSocketAuthInterceptor
        Principal principal = event.getUser();

        if (principal != null) {
            String username = principal.getName();

            // 🔑 1. MISE À JOUR DANS LA BASE DE DONNÉES ET PUBLICATION
            // Le service doit gérer l'update DB et l'envoi du message /topic/public/presence
            presenceService.setUserOnline(username, true);

            System.out.println("PRESENCE LISTENER: Utilisateur " + username + " est EN LIGNE.");
        }
    }

    /**
     * Gère l'événement de déconnexion WebSocket/STOMP (fermeture de l'onglet/appli).
     * Met l'utilisateur hors ligne et publie l'événement.
     */
    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        Principal principal = event.getUser();

        if (principal != null) {
            String username = principal.getName();

            // 🔑 2. MISE À JOUR DANS LA BASE DE DONNÉES ET PUBLICATION
            presenceService.setUserOnline(username, false);

            System.out.println("PRESENCE LISTENER: Utilisateur " + username + " est HORS LIGNE.");
        }
    }
    // 🔑 MÉTHODE POUR OBTENIR LE NOMBRE D'UTILISATEURS EN LIGNE
    public static int getOnlineUsersCount() {
        return onlineUsers.size();
    }
}