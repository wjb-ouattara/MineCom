package com.mining.minecom.model;

// Cette classe représente ce que le client envoie au serveur
public class UserCredentials {
    private String username;
    private String password;
    private String phone;
    private String profilePictureUrl;

    // Constructeur pour l'inscription
    public UserCredentials(String username, String password, String phone, String profilePictureUrl) {
        this.username = username;
        this.password = password;
        this.phone = phone;
        this.profilePictureUrl = profilePictureUrl;
    }

    // Constructeur pour la connexion (sans phone ni url)
    public UserCredentials(String username, String password) {
        this(username, password, null, null);
    }

    // Jackson a besoin des Getters pour sérialiser l'objet en JSON
    public String getUsername() { return username; }
    public String getPassword() { return password; }
    public String getPhone() { return phone; }
    public String getProfilePictureUrl() { return profilePictureUrl; }

    // Les Setters ne sont pas strictement nécessaires ici car nous utilisons des constructeurs
}