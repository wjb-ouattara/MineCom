package com.mining.minecom.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.mining.minecom_server.common.dto.TeamDto;
import com.mining.minecom_server.common.dto.TeamRequest;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Collections;
import java.util.List;

/**
 * Appels REST pour les équipes (groupes privés).
 * Les méthodes d'écriture lèvent une exception avec un message lisible en cas d'échec.
 */
public class TeamService {

    private static final String BASE_API_URL = "http://localhost:8080/api/teams";
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper mapper;

    public TeamService() {
        this.mapper = new ObjectMapper();
        this.mapper.registerModule(new JavaTimeModule());
    }

    /** Équipes dont l'utilisateur connecté est membre. */
    public List<TeamDto> getMyTeams() {
        try {
            HttpResponse<String> response = httpClient.send(authorized(BASE_API_URL).GET().build(),
                    HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                return mapper.readValue(response.body(), new TypeReference<List<TeamDto>>() {});
            }
            System.err.println("Erreur TeamService.getMyTeams: Code " + response.statusCode());
        } catch (Exception e) {
            System.err.println("Erreur chargement des équipes: " + e.getMessage());
        }
        return Collections.emptyList();
    }

    public TeamDto createTeam(String name, String description, List<Long> memberIds) throws Exception {
        String body = mapper.writeValueAsString(new TeamRequest(name, description, memberIds));
        return send(authorized(BASE_API_URL)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body)), "créer l'équipe");
    }

    public TeamDto addMembers(Long teamId, List<Long> memberIds) throws Exception {
        String body = mapper.writeValueAsString(new TeamRequest(null, null, memberIds));
        return send(authorized(BASE_API_URL + "/" + teamId + "/members")
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body)), "ajouter les membres");
    }

    /** Retirer un membre (admin) ou quitter l'équipe (userId = utilisateur connecté). */
    public void removeMember(Long teamId, Long userId) throws Exception {
        HttpResponse<String> response = httpClient.send(
                authorized(BASE_API_URL + "/" + teamId + "/members/" + userId).DELETE().build(),
                HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new Exception(errorMessage(response.statusCode(), "retirer ce membre"));
        }
    }

    private TeamDto send(HttpRequest.Builder builder, String action) throws Exception {
        HttpResponse<String> response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new Exception(errorMessage(response.statusCode(), action));
        }
        return mapper.readValue(response.body(), TeamDto.class);
    }

    private String errorMessage(int status, String action) {
        return switch (status) {
            case 400 -> "Impossible de " + action + " : données invalides (nom obligatoire).";
            case 403 -> "Impossible de " + action + " : action réservée aux administrateurs de l'équipe.";
            case 404 -> "Impossible de " + action + " : équipe introuvable.";
            default -> "Impossible de " + action + " (code " + status + ").";
        };
    }

    private HttpRequest.Builder authorized(String url) {
        return HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Authorization", "Bearer " + AuthService.getCurrentJwtToken());
    }
}
