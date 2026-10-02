package com.mining.minecom_server.filter;

import com.mining.minecom_server.util.JwtUtils;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class AuthTokenFilter extends OncePerRequestFilter {

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private UserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        // 🔑 DEBUG
        System.out.println("🔍 AuthTokenFilter - URL : " + request.getRequestURI());
        System.out.println("🔍 AuthTokenFilter - Method : " + request.getMethod());
        System.out.println("🔍 AuthTokenFilter - Authorization header : " +
                (request.getHeader("Authorization") != null ? "PRÉSENT" : "ABSENT"));

        String authHeader = request.getHeader("Authorization");

        // 🔑 CORRECTION : Structure if/else correcte
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            try {
                String jwt = parseJwt(request);

                if (jwt != null && jwtUtils.validateJwtToken(jwt)) {
                    String username = jwtUtils.getUserNameFromJwtToken(jwt);

                    UserDetails userDetails = userDetailsService.loadUserByUsername(username);

                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(
                                    userDetails,
                                    null,
                                    userDetails.getAuthorities()
                            );

                    authentication.setDetails(
                            new WebAuthenticationDetailsSource().buildDetails(request)
                    );

                    SecurityContextHolder.getContext().setAuthentication(authentication);
                    System.out.println("✅ Token valide pour : " + username); // 🔑 CORRECTION
                } else {
                    System.err.println("❌ Token JWT invalide ou null");
                }

            } catch (Exception e) {
                System.err.println("❌ Cannot set user authentication: " + e.getMessage());
            }

        } else {
            System.err.println("❌ Pas de token Bearer dans la requête pour : " +
                    request.getRequestURI());
        }

        // 🔑 IMPORTANT : Toujours appeler filterChain.doFilter() à la fin
        filterChain.doFilter(request, response);
    }

    private String parseJwt(HttpServletRequest request) {
        String headerAuth = request.getHeader("Authorization");

        if (StringUtils.hasText(headerAuth) && headerAuth.startsWith("Bearer ")) {
            return headerAuth.substring(7);
        }

        return null;
    }
}