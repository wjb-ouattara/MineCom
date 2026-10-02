package com.mining.minecom.service;

import com.mining.minecom.common.dto.SOSAlertDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class SOSAlertService {

    private static final String BASE_API_URL = "http://localhost:8080/api/sos";
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper mapper;

    public SOSAlertService() {
        this.mapper = new ObjectMapper();
        this.mapper.registerModule(new JavaTimeModule());
    }

    public SOSAlertDto sendSOSAlert(String location, String description) {
        String jwtToken = AuthService.getCurrentJwtToken();

        if (jwtToken == null) {
            System.err.println("SOSAlertService: Aucun token JWT disponible.");
            return null;
        }

        try {
            // 🔑 CORRECTION : Envoyer un body JSON au lieu de query params
            String url = BASE_API_URL + "/alert";

            // Construire le body JSON
            String jsonBody = String.format(
                    "{\"location\": \"%s\", \"description\": \"%s\"}",
                    location != null ? location.replace("\"", "\\\"") : "",
                    description != null ? description.replace("\"", "\\\"") : ""
            );

            System.out.println("🔗 URL SOS : " + url);
            System.out.println("📦 Body JSON : " + jsonBody);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(new URI(url))
                    .header("Authorization", "Bearer " + jwtToken)
                    .header("Content-Type", "application/json")
                    // 🔑 CORRECTION : Envoyer le JSON dans le body
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();

            HttpResponse<String> response = httpClient.send(request,
                    HttpResponse.BodyHandlers.ofString());

            System.out.println("📥 Status : " + response.statusCode());
            System.out.println("📥 Réponse : " + response.body());

            if (response.statusCode() == 200) {
                SOSAlertDto alert = mapper.readValue(response.body(), SOSAlertDto.class);
                System.out.println("🚨 Alerte SOS envoyée : ID=" + alert.getId());
                return alert;
            } else {
                System.err.println("Erreur SOS: Code: " + response.statusCode());
                System.err.println("Réponse: " + response.body());
                return null;
            }

        } catch (Exception e) {
            System.err.println("Erreur lors de l'envoi SOS: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    public void acknowledgeAlert(Long alertId) {
        String jwtToken = AuthService.getCurrentJwtToken();

        if (jwtToken == null) {
            System.err.println("SOSAlertService: Aucun token JWT disponible.");
            return;
        }

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(new URI(BASE_API_URL + "/acknowledge/" + alertId))
                    .header("Authorization", "Bearer " + jwtToken)
                    .POST(HttpRequest.BodyPublishers.noBody())
                    .build();

            HttpResponse<String> response = httpClient.send(request,
                    HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                System.out.println("✅ Accusé de réception SOS envoyé pour alerte ID=" + alertId);
            }

        } catch (Exception e) {
            System.err.println("Erreur lors de l'accusé SOS: " + e.getMessage());
            e.printStackTrace();
        }
    }
}