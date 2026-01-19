package com.mining.minecom_server.common.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class JwtResponse {
    private String token;
    private Long userId;
    private String username;

    public JwtResponse(){}
    public JwtResponse(String token, String username, Long userId) {
        this.token = token;
        this.username = username;
        this.userId= userId;
    }

    // Getters et Setters (Omis ici pour la concision)
    // Assurez-vous de les inclure dans votre code réel.
    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public Long getUserId() { return userId;}

    public void setUserId(Long userId) {
        this.userId = userId;
    }
}