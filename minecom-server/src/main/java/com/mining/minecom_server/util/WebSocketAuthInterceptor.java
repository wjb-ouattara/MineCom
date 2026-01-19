package com.mining.minecom_server.util;

import com.mining.minecom_server.util.JwtUtils;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;

import java.security.Principal;

@Component
public class WebSocketAuthInterceptor implements ChannelInterceptor {

    private final JwtUtils jwtUtils;
    private final UserDetailsService userDetailsService;

    public WebSocketAuthInterceptor(JwtUtils jwtUtils, UserDetailsService userDetailsService) {
        this.jwtUtils = jwtUtils;
        this.userDetailsService = userDetailsService;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor != null) {
            // 🔑 Pour CONNECT : Extraire le JWT du header et créer le Principal
            if (StompCommand.CONNECT.equals(accessor.getCommand())) {
                String jwt = accessor.getFirstNativeHeader("X-Auth-Token");

                if (jwt != null && jwtUtils.validateJwtToken(jwt)) {
                    String username = jwtUtils.getUserNameFromJwtToken(jwt);
                    UserDetails userDetails = userDetailsService.loadUserByUsername(username);

                    UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                            userDetails, null, userDetails.getAuthorities());

                    accessor.setUser(authentication);
                    System.out.println("WebSocket AUTH: Utilisateur " + username + " authentifié.");
                } else {
                    System.err.println("WebSocket AUTH ÉCHEC: Token invalide ou manquant.");
                }
            }
            // 🔑 Pour TOUS les autres messages : Récupérer le Principal de la session
            else if (accessor.getUser() == null && accessor.getSessionAttributes() != null) {
                // Essayer de récupérer l'authentification depuis la session WebSocket
                Object auth = accessor.getSessionAttributes().get("SPRING_SECURITY_CONTEXT");
                if (auth != null) {
                    accessor.setUser((Principal) auth);
                }
            }
        }

        return message;
    }
}