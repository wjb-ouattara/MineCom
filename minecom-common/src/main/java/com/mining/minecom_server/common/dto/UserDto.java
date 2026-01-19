package com.mining.minecom_server.common.dto;

// Note: Utilisez le package de votre projet serveur

public class UserDto {
    private Long id;
    private String username;
    private Boolean isOnline;
    // Vous pouvez ajouter ici l'URL de l'avatar si vous en avez un.

    // Constructeur pour le mapping
    public UserDto(Long id, String username, Boolean isOnline) {
        this.id = id;
        this.username = username;
        this.isOnline = isOnline;
    }

    // Constructeur par défaut (souvent nécessaire pour Jackson)
    public UserDto() {
    }

    // Getters
    public Long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public Boolean getIsOnline() {
        return isOnline;
    }

    // Setters (si nécessaire, mais souvent omis pour les DTO de réponse)
    public void setId(Long id) {
        this.id = id;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setIsOnline(Boolean online) {
        isOnline = online;
    }
}