package com.mining.minecom.service;

import com.mining.minecom.common.dto.*;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.web.socket.client.WebSocketClient;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;
import org.springframework.web.socket.WebSocketHttpHeaders;

import java.lang.reflect.Type;
import javafx.application.Platform;

public class WebSocketService {

    private StompSession stompSession;
    // 🔑 Assurez-vous que cette URL est correcte (votre serveur doit être sur localhost:8080)
    private final String WS_URL = "ws://localhost:8080/ws";
    private final AuthService authService;
    private DashboardCallback callback;

    public interface DashboardCallback {
        void onMessageReceived(MessageResponse message);
        void onPresenceUpdate(PresenceUpdate update);
        void onConnectedSuccess();
        void onSOSAlertReceived(SOSAlertDto alert);
        void onSOSAcknowledgment(SOSAcknowledgmentDto ack);
    }

    // --- CONSTRUCTEUR ---
    public WebSocketService(AuthService authService) {
        this.authService = authService;
    }

    public void setCallback(DashboardCallback callback) {
        this.callback = callback;
    }

    // --- MÉTHODE CONNECT ---
    public void connect() {
        System.out.println("🔄 DÉBUT de la tentative de connexion WebSocket...");
        System.out.println("   URL cible: " + WS_URL);
        System.out.println("   Token JWT: " + (authService.getCurrentJwtToken() != null ? "PRÉSENT" : "ABSENT"));

        WebSocketClient client = new StandardWebSocketClient();
        WebSocketStompClient stompClient = new WebSocketStompClient(client);
        stompClient.setMessageConverter(new MappingJackson2MessageConverter());

        StompSessionHandlerAdapter sessionHandler = new SessionHandler();

        StompHeaders connectHeaders = new StompHeaders();
        String token = authService.getCurrentJwtToken();
        if (token != null) {
            connectHeaders.add("X-Auth-Token", token);
            System.out.println("   Header X-Auth-Token ajouté");
        }

        WebSocketHttpHeaders handshakeHeaders = new WebSocketHttpHeaders();

        System.out.println("🚀 Appel de connectAsync()...");

        try {
            stompClient.connectAsync(
                    WS_URL,
                    handshakeHeaders,
                    connectHeaders,
                    sessionHandler
            ).whenComplete((session, throwable) -> {
                System.out.println("⚡ whenComplete() APPELÉ");
                if (throwable != null) {
                    System.err.println("❌ ÉCHEC DE CONNEXION WEBSOCKET:");
                    System.err.println("   Type d'erreur: " + throwable.getClass().getName());
                    System.err.println("   Message: " + throwable.getMessage());
                    throwable.printStackTrace();
                } else {
                    System.out.println("✅ Connexion WebSocket RÉUSSIE dans whenComplete!");
                }
            });
            System.out.println("✓ connectAsync() appelé sans exception");
        } catch (Exception e) {
            System.err.println("💥 EXCEPTION lors de l'appel de connectAsync():");
            e.printStackTrace();
        }
    }
    // --- MÉTHODE SEND MESSAGE ---
    public void sendMessage(MessageRequest messageRequestDto) {
        // 🔑 Utilisation de la méthode isConnected() corrigée
        if (isConnected()) {
            // 🔑 CORRECTION : Chemin spécifique pour l'envoi de messages privés
            stompSession.send("/app/private-message", messageRequestDto);
            // 💡 IMPORTANT : Vérifiez que votre serveur écoute bien le @MessageMapping("/private-message")
        } else {
            System.err.println("Impossible d'envoyer le message : la session WebSocket n'est pas connectée.");
        }
    }

    /**
     * Vérifie si la connexion WebSocket STOMP est active.
     * @return true si la session STOMP est non nulle et connectée.
     */
    public boolean isConnected(){
        // 🔑 CORRECTION : Vérifie l'existence de l'objet ET son état de connexion
        return this.stompSession != null && this.stompSession.isConnected();
    }

    // --- GESTIONNAIRE DE SESSION (SessionHandler) ---
    private class SessionHandler extends StompSessionHandlerAdapter {

        @Override
        public void afterConnected(StompSession session, StompHeaders connectedHeaders) {
            stompSession = session; // 🔑 Stockage critique de la session
            System.out.println("WebSocket: Connecté à la session STOMP. Session ID: " + session.getSessionId());

            subscribeToPrivateMessages();
            subscribeToPresence();
            subscribeToSOSAlerts();
            subscribeToSOSAcknowledgments();
            if (callback != null) {
                Platform.runLater(() -> callback.onConnectedSuccess());
            }
        }

        @Override
        public void handleTransportError(StompSession session, Throwable exception) {
            System.err.println("ERREUR CRITIQUE DE CONNEXION : Le transport a échoué.");
            System.err.println("Cause détaillée : " + exception.getMessage());
            // Si l'erreur est critique (ex: serveur éteint), marquer la session comme nulle
            stompSession = null;
        }

        @Override
        public void handleException(StompSession session, org.springframework.messaging.simp.stomp.StompCommand command, StompHeaders headers, byte[] payload, Throwable exception) {
            System.err.println("WebSocket: Erreur lors de la gestion de la trame STOMP. " + exception.getMessage());
        }

        // Gère les messages non attendus par les abonnements
        @Override
        public void handleFrame(StompHeaders headers, Object payload) {
            System.out.println("WebSocket: Trame STOMP inattendue reçue.");
        }
    }

    // --- MÉTHODE SUBSCRIBE TO PRESENCE ---
    private void subscribeToPresence() {
        // ... (Logique inchangée : ABONNEMENT À /topic/public/presence)
        String destination = "/topic/public/presence";
        stompSession.subscribe(destination, new StompSessionHandlerAdapter() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return PresenceUpdate.class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                PresenceUpdate update = (PresenceUpdate) payload;
                if (callback != null) {
                    Platform.runLater(() -> callback.onPresenceUpdate(update));
                }
            }
        });
        System.out.println("WebSocket: Abonné aux mises à jour de présence: " + destination);
    }

    // S'abonner aux alertes SOS
    private void subscribeToSOSAlerts() {
        String destination = "/topic/public/sos-alert";
        stompSession.subscribe(destination, new StompSessionHandlerAdapter() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return SOSAlertDto.class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                SOSAlertDto alert = (SOSAlertDto) payload;
                if (callback != null) {
                    Platform.runLater(() -> callback.onSOSAlertReceived(alert));
                }
            }
        });
        System.out.println("WebSocket: Abonné aux alertes SOS: " + destination);
    }

    // 🔑 NOUVELLE MÉTHODE : S'abonner aux accusés de réception SOS
    private void subscribeToSOSAcknowledgments() {
        String destination = "/user/queue/sos-acknowledgment";
        stompSession.subscribe(destination, new StompSessionHandlerAdapter() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return SOSAcknowledgmentDto.class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                SOSAcknowledgmentDto ack = (SOSAcknowledgmentDto) payload;
                if (callback != null) {
                    Platform.runLater(() -> callback.onSOSAcknowledgment(ack));
                }
            }
        });
        System.out.println("WebSocket: Abonné aux accusés SOS: " + destination);
    }

    // --- MÉTHODE SUBSCRIBE TO PRIVATE MESSAGES ---
    private void subscribeToPrivateMessages() {
        // ... (Logique inchangée : ABONNEMENT À /user/queue/messages)
        String destination = "/user/queue/messages";
        stompSession.subscribe(destination, new StompSessionHandlerAdapter() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return MessageResponse.class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                MessageResponse message = (MessageResponse) payload;
                if (callback != null) {
                    Platform.runLater(() -> callback.onMessageReceived(message));
                }
            }
        });
        System.out.println("WebSocket: Abonné à la file d'attente privée: " + destination);
    }

    // --- MÉTHODE DISCONNECT ---
    public void disconnect() {
        if (stompSession != null && stompSession.isConnected()) {
            stompSession.disconnect();
            System.out.println("WebSocket: Session STOMP déconnectée.");
        }
    }
}