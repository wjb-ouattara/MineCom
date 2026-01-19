package com.mining.minecom_server.common.model;

import com.mining.minecom_server.common.enums.UserStatus;
import java.time.Instant;

/**
 * Version POJO de User (sans Spring Security ni JPA)
 * Pour le transfert de données entre client et serveur
 */
public class User {

    private Long id;
    private String username;
    private String password; // Note: Ne jamais transférer le mot de passe au client!

    // Constructeurs
    public User() {}

    public User(Long id, String username, String email, Instant createdAt, UserStatus status) {
        this.id = id;
        this.username = username;
    }

    // Getters et Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

}