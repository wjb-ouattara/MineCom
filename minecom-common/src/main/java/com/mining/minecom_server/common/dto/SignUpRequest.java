package com.mining.minecom_server.common.dto;

// DTO pour la requête d'inscription du client
public class SignUpRequest {

    private String username;
    private String password;
    private String phone;
    private String profilePictureUrl; // Le champ de la photo (URL/chemin)

    // Générez tous les Getters et Setters pour ces champs

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getProfilePictureUrl() { return profilePictureUrl; }
    public void setProfilePictureUrl(String profilePictureUrl) { this.profilePictureUrl = profilePictureUrl; }
}