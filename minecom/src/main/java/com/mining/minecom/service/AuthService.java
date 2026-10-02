package com.mining.minecom.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mining.minecom.common.dto.JwtResponse;
import com.mining.minecom.common.dto.UserDto;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;


public class AuthService {

    private static final String BASE_API_URL = "http://localhost:8080/api/";

    // 🔑 CLÉ : STOCKAGE STATIQUE DU TOKEN JWT
    private static String currentJwtToken = null;
    private static String currentUsername = null;
    private static Long currentUserId = null;

    // Pour l'accès aux endpoints sécurisés
    public static String getCurrentJwtToken() {
        return currentJwtToken;
    }
    public static String getCurrentUsername() {
        return currentUsername;
    }
    public static Long getCurrentUserId() {
        return currentUserId;
    }
    public static void setCurrentUsername(String username) {
        currentUsername = username;
    }
    public static void setCurrentUserId(Long userId) {
        currentUserId = userId;
    }

    public boolean login(String username, String password) throws Exception {
        HttpClient client = HttpClient.newHttpClient();
        ObjectMapper mapper = new ObjectMapper();

        // 1. DTO d'entrée (LoginRequest non créé, donc on utilise un String)
        String jsonInput = String.format("{\"username\": \"%s\", \"password\": \"%s\"}", username, password);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_API_URL + "auth/login"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonInput))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 200) {

            JwtResponse jwtResponse = mapper.readValue(response.body(), JwtResponse.class);

            currentJwtToken = jwtResponse.getToken();

            System.out.println("DEBUG CLIENT: Connexion réussie. Token stocké.");


            // Récupération des infos utilisateur
            HttpRequest userRequest = HttpRequest.newBuilder()
                    .uri(URI.create(BASE_API_URL + "users/by-username/" + username))
                    .header("Authorization", "Bearer " + currentJwtToken)
                    .GET()
                    .build();


            HttpResponse<String> userResponse =
                    client.send(userRequest, HttpResponse.BodyHandlers.ofString());


            if(userResponse.statusCode() == 200){

                UserDto user = mapper.readValue(userResponse.body(), UserDto.class);


                currentUsername = user.getUsername();
                currentUserId = user.getId();


                System.out.println(
                        "✅ User chargé : "
                                + currentUsername
                                + " ID="
                                + currentUserId
                );

            }else{

                System.err.println(
                        "Impossible de récupérer user : "
                                + userResponse.statusCode()
                );

            }


            return true;
        }
        else {
            // Statut 401 ou 400
            System.err.println("Échec de la connexion. Statut : " + response.statusCode());
            // Vous pouvez lancer une exception ou retourner false
            return false;
        }
    }

    // =========================================================================
    // B. LOGIQUE D'INSCRIPTION (sans token)
    // =========================================================================

    public boolean signUp(String username, String password, String phone, String profilePictureUrl) throws Exception {
        HttpClient client = HttpClient.newHttpClient();
        ObjectMapper mapper = new ObjectMapper();

        // Créez le corps JSON pour l'inscription
        // Assurez-vous que votre modèle UserClient ou DTO correspond à votre modèle UserRequest côté serveur
        String jsonInput = String.format(
                "{\"username\": \"%s\", \"password\": \"%s\", \"phone\": \"%s\", \"profilePictureUrl\": \"%s\"}",
                username, password, phone, profilePictureUrl != null ? profilePictureUrl : ""
        );

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_API_URL + "auth/signup"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonInput))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        // 201 Created ou 200 OK pour le succès, 409 Conflict ou 400 Bad Request pour l'échec
        return response.statusCode() == 200 || response.statusCode() == 201;
    }

    // =========================================================================
    // C. NOUVELLE MÉTHODE POUR TESTER L'ACCÈS PROTÉGÉ
    // =========================================================================

    public String accessProtectedEndpoint() throws Exception {
        if (currentJwtToken == null) {
            return "Non authentifié.";
        }

        HttpClient client = HttpClient.newHttpClient();
        String authorizationHeader = "Bearer " + currentJwtToken;

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_API_URL + "test/protected"))
                .header("Authorization", authorizationHeader)
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 200) {
            return "SUCCÈS : " + response.body();
        } else {
            return "ÉCHEC (" + response.statusCode() + "): " + response.body();
        }
    }

    // Pour la déconnection de l'utilisateur
    public void logout() {
        if (currentJwtToken == null) {
            return; // Déjà déconnecté
        }

        // Nous utilisons l'ancien token pour informer le serveur de la déconnexion
        String targetUrl = BASE_API_URL + "auth/logout";

        try {
            HttpClient client = HttpClient.newHttpClient();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(targetUrl))
                    .header("Authorization", "Bearer " + currentJwtToken)
                    .POST(HttpRequest.BodyPublishers.noBody()) // POST sans corps
                    .build();

            client.send(request, HttpResponse.BodyHandlers.ofString());

            // Réinitialiser le token côté client, même si l'appel échoue.
            currentJwtToken = null;
            currentUsername = null;
            currentUserId = null;
            System.out.println("LOGOUT CLIENT: Token local effacé et déconnexion serveur initiée.");

        } catch (Exception e) {
            System.err.println("Erreur lors de l'appel de déconnexion au serveur : " + e.getMessage());
            // Nous continuons la déconnexion côté client même en cas d'erreur réseau
            currentJwtToken = null;
        }
    }
}