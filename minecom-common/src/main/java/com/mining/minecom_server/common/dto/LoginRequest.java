package com.mining.minecom_server.common.dto;

// DTO pour la requête de connexion du client
public class LoginRequest {

    private String username;
    private String password;

    // Générez les Getters et Setters pour ces champs

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
}