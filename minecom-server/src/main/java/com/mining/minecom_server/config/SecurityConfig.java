package com.mining.minecom_server.config;

import com.mining.minecom_server.filter.PresenceFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import com.mining.minecom_server.filter.AuthTokenFilter;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final AuthTokenFilter authTokenFilter;
    private final PresenceFilter presenceFilter; // <-- Injection

    public SecurityConfig(AuthTokenFilter authTokenFilter, PresenceFilter presenceFilter) {
        this.authTokenFilter = authTokenFilter;
        this.presenceFilter = presenceFilter; // <-- Initialisation
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, AuthTokenFilter authTokenFilter) throws Exception {
        http
                // 1. Désactiver la protection CSRF
                .csrf(AbstractHttpConfigurer::disable)

                // 2. Définir les autorisations d'accès aux requêtes
                .authorizeHttpRequests(auth -> auth
                        // Autoriser l'accès sans authentification aux endpoints d'AUTH
                        .requestMatchers("/api/auth/**").permitAll()

                        // 🔑 Autoriser le HANDSHAKE WebSocket
                        .requestMatchers("/ws/**").permitAll()

                        // Toutes les autres requêtes nécessitent une authentification
                        .anyRequest().authenticated()
                );

        // 3. Désactiver la connexion basique HTTP pour éviter le mot de passe généré
        http.httpBasic(AbstractHttpConfigurer::disable);

        // 4. Le reste du code utilise maintenant le paramètre 'authTokenFilter'
        http.addFilterBefore(authTokenFilter, UsernamePasswordAuthenticationFilter.class);
        http.addFilterAfter(presenceFilter, AuthTokenFilter.class);

        return http.build();
    }
}