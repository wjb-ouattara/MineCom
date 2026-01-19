// Dans com.mining.minecom.dto/PresenceUpdate.java

package com.mining.minecom.common.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class PresenceUpdate {
    private Long userId;
    private String username;
    private boolean isOnline;

    public PresenceUpdate() { /* Nécessaire pour Jackson */ }

    @JsonCreator
    public PresenceUpdate(
            @JsonProperty("userId") Long userId,
            @JsonProperty("username") String username,
            @JsonProperty("online") boolean isOnline) {
        this.userId = userId;
        this.username = username;
        this.isOnline = isOnline;
    }

    // Getters et Setters
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    // Le getter booléen est conventionnellement 'isOnline()'
    public boolean isOnline() { return isOnline; }
    public void setOnline(boolean online) { isOnline = online; }
}