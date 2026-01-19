// Dans com.mining.minecom.dto.UserDto.java (CLIENT)

package com.mining.minecom.common.dto;

public class UserDto {
    private Long id;
    private String username;
    private Boolean isOnline;

    // Constructeur par défaut (nécessaire pour Jackson)
    public UserDto() {
    }

    // Constructeur pour les mises à jour de présence
    public UserDto(Long id, String username, Boolean isOnline) {
        this.id = id;
        this.username = username;
        this.isOnline = isOnline;
    }

    // Getters et Setters (nécessaires pour Jackson et l'accès dans le contrôleur)
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public Boolean getIsOnline() {
        return isOnline;
    }

    public void setIsOnline(Boolean online) {
        isOnline = online;
    }
}