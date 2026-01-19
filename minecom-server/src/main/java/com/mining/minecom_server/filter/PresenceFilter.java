// Dans com.mining.minecom_server.security.jwt/PresenceFilter.java

package com.mining.minecom_server.filter;

import com.mining.minecom_server.service.PresenceService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class PresenceFilter extends OncePerRequestFilter {

    private final PresenceService presenceService;

    public PresenceFilter(PresenceService presenceService) {
        this.presenceService = presenceService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        // 1. Laissez les autres filtres (dont le JWT Filter) faire leur travail
        filterChain.doFilter(request, response);

        // 2. VÉRIFICATION APRÈS le traitement du JWT
        // Le contexte de sécurité est rempli seulement si le JWT était valide et l'utilisateur authentifié.
        if (SecurityContextHolder.getContext().getAuthentication() != null &&
                SecurityContextHolder.getContext().getAuthentication().isAuthenticated()) {

            // Récupérer le nom de l'utilisateur authentifié
            String username = SecurityContextHolder.getContext().getAuthentication().getName();

            // Mettre à jour le statut en ligne (asynchrone, optionnel)
            // L'appel au service sera enveloppé dans la même transaction que la requête
            presenceService.setUserOnline(username,true);
        }
    }
}