package com.mining.minecom.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.mining.minecom.common.dto.MessageResponse;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Collections;
import java.util.List;

public class MessageService {

    private static final String BASE_API_URL = "http://localhost:8080/api/messages";
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper mapper;

    public MessageService() {
        this.mapper = new ObjectMapper();
        this.mapper.registerModule(new JavaTimeModule()); // Pour gérer Instant
    }

    /**
     * Récupère l'historique de conversation avec un utilisateur
     */
    public List<MessageResponse> getConversationHistory(Long partnerId) {
        return fetchHistory(BASE_API_URL + "/" + partnerId);
    }

    /**
     * Récupère l'historique d'une équipe (vide si l'utilisateur n'en est pas membre)
     */
    public List<MessageResponse> getTeamHistory(Long teamId) {
        return fetchHistory(BASE_API_URL + "/team/" + teamId);
    }

    private List<MessageResponse> fetchHistory(String url) {
        String jwtToken = AuthService.getCurrentJwtToken();

        if (jwtToken == null) {
            System.err.println("MessageService: Aucun token JWT disponible.");
            return Collections.emptyList();
        }

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(new URI(url))
                    .header("Authorization", "Bearer " + jwtToken)
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                return mapper.readValue(response.body(), new TypeReference<List<MessageResponse>>() {});
            } else {
                System.err.println("Erreur lors de la récupération de l'historique. Code: " + response.statusCode());
                return Collections.emptyList();
            }

        } catch (Exception e) {
            System.err.println("Erreur lors de la requête d'historique: " + e.getMessage());
            e.printStackTrace();
            return Collections.emptyList();
        }
    }
}