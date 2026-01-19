package com.mining.minecom.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.mining.minecom.common.dto.ConversationPreviewDto;
import com.mining.minecom.common.dto.UserDto;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Collections;
import java.util.List;

public class UserService {

    // L'URL de base doit correspondre à la racine des API REST
    private static final String BASE_API_URL = "http://localhost:8080/api/users";
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper mapper ;

    public UserService() {
        this.mapper = new ObjectMapper();
        // 🔑 ENREGISTRER LE MODULE POUR LES DATES JAVA 8
        this.mapper.registerModule(new JavaTimeModule());
    }
    public UserDto getCurrentUser(Long userId) {
        String jwtToken = AuthService.getCurrentJwtToken();

        if (jwtToken == null) {
            System.err.println("UserService: Aucun token JWT disponible.");
            return null;
        }

        try {
            // 🔑 Utiliser le même port que BASE_API_URL (8080 au lieu de 8082)
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(new URI(BASE_API_URL + "/" + userId))
                    .header("Authorization", "Bearer " + jwtToken)
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                UserDto user = mapper.readValue(response.body(), UserDto.class);
                System.out.println("✅ User récupéré: " + user.getUsername());
                return user;
            } else {
                System.err.println("Erreur UserService: Code: " + response.statusCode());
                System.err.println("Réponse: " + response.body());
                return null;
            }

        } catch (Exception e) {
            System.err.println("Erreur lors de la récupération du user: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    // Récupérer les conversations avec derniers messages
    public List<ConversationPreviewDto> getConversations() {
        String jwtToken = AuthService.getCurrentJwtToken();

        if (jwtToken == null) {
            System.err.println("UserService: Aucun token JWT disponible.");
            return Collections.emptyList();
        }

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(new URI(BASE_API_URL + "/conversations"))
                    .header("Authorization", "Bearer " + jwtToken)
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                return mapper.readValue(response.body(), new TypeReference<List<ConversationPreviewDto>>() {});
            } else {
                System.err.println("Erreur UserService: Code: " + response.statusCode());
                System.err.println("Réponse: " + response.body());
                return Collections.emptyList();
            }

        } catch (Exception e) {
            System.err.println("Erreur lors de la requête: " + e.getMessage());
            e.printStackTrace();
            return Collections.emptyList();
        }
    }
    /**
     * Récupère la liste de tous les contacts (utilisateurs) depuis l'API sécurisée.
     * @return Une liste de UserDto incluant le statut en ligne.
     */
    public List<UserDto> getContacts() {
        String jwtToken = AuthService.getCurrentJwtToken();

        if (jwtToken == null) {
            System.err.println("UserService: Aucun token JWT disponible. Impossible de charger les contacts.");
            return Collections.emptyList();
        }

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(new URI(BASE_API_URL + "/contacts")) // Endpoint créé côté SERVEUR
                    .header("Authorization", "Bearer " + jwtToken) // 🔑 EN-TÊTE JWT OBLIGATOIRE
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                // Désérialiser la liste des DTOs (nous avons besoin du TypeReference pour les listes)
                return mapper.readValue(response.body(), new TypeReference<List<UserDto>>() {});
            } else {
                System.err.println("Erreur UserService: Impossible de récupérer les contacts. Code: " + response.statusCode());
                System.err.println("Réponse du serveur: " + response.body());
                return Collections.emptyList();
            }

        } catch (Exception e) {
            System.err.println("Erreur lors de la requête des contacts: " + e.getMessage());
            return Collections.emptyList();
        }
    }
    public UserDto getUserByUsername(String username) {
        String jwtToken = AuthService.getCurrentJwtToken();

        if (jwtToken == null) {
            System.err.println("UserService: Aucun token JWT disponible.");
            return null;
        }

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(new URI(BASE_API_URL + "/by-username/" + username))
                    .header("Authorization", "Bearer " + jwtToken)
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                UserDto user = mapper.readValue(response.body(), UserDto.class);
                System.out.println("✅ User récupéré: " + user.getUsername());
                return user;
            } else {
                System.err.println("Erreur UserService: Code: " + response.statusCode());
                return null;
            }

        } catch (Exception e) {
            System.err.println("Erreur lors de la récupération du user: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
}