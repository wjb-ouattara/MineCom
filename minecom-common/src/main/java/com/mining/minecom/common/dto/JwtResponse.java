package com.mining.minecom.common.dto;

public class JwtResponse {
    private String token;
    private Long userId;      // ✅ AJOUTER CE CHAMP
    private String username;

    // Constructeur par défaut (requis pour Jackson)
    public JwtResponse() {}

    // Constructeur complet
    public JwtResponse(String token, Long userId, String username) {
        this.token = token;
        this.userId = userId;
        this.username = username;
    }

    // Getters et Setters
    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    @Override
    public String toString() {
        return "JwtResponse{" +
                "token='" + token + '\'' +
                ", userId=" + userId +
                ", username='" + username + '\'' +
                '}';
    }
}