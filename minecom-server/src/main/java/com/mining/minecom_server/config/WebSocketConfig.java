// Dans com.mining.minecom_server.config/WebSocketConfig.java

package com.mining.minecom_server.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import com.mining.minecom_server.util.WebSocketAuthInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
/** import org.springframework.messaging.simp.config.ChannelInterceptorRegistration; **/

@Configuration
@EnableWebSocketMessageBroker // Active le traitement des messages STOMP via WebSockets
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    /**
     * Enregistre un endpoint (point d'accès) pour les clients WebSockets.
     * C'est l'URL à laquelle le client JavaFX se connectera initialement.
     */
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Le client se connectera à ws://<serveur>:<port>/ws
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*"); // Configurez ceci en production !
    }

    /**
     * Configure le broker de messages (le routeur de messages).
     */
    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // 1. Préfixes de destination gérés par le broker en mémoire.
        // /topic : diffusion à tous.
        // /queue : files privées. convertAndSendToUser(user, "/queue/x") est résolu
        //          par UserDestinationMessageHandler en "/queue/x-user{sessionId}" :
        //          sans "/queue" ici, le broker ignore ces messages (et les SUBSCRIBE
        //          à /user/queue/...) en silence.
        config.enableSimpleBroker("/topic", "/queue");

        // 2. Préfixe pour les messages point-à-point (système de file d'attente utilisateur)
        // Il est utilisé pour les messages privés. Spring préfixera automatiquement 
        // l'ID de l'utilisateur pour acheminer le message au bon destinataire.
        config.setUserDestinationPrefix("/user");

        // 3. Préfixes des applications (où les messages sont ENVOYÉS)
        // Les clients enverront des messages à des destinations commençant par /app (ex: /app/chat)
        config.setApplicationDestinationPrefixes("/app");
    }
    @Autowired
    private WebSocketAuthInterceptor authInterceptor;

    @Override
    public void configureClientInboundChannel(org.springframework.messaging.simp.config.ChannelRegistration registration) {
        registration.interceptors(authInterceptor);
    }
}