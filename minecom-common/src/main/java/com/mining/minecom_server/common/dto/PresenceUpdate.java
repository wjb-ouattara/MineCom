// Dans com.mining.minecom_server.dto/PresenceUpdate.java

package com.mining.minecom_server.common.dto;

public class PresenceUpdate {
    private Long userId;
    private String username;
    private boolean isOnline;

    public PresenceUpdate(Long userId, String username, boolean isOnline) {
        this.userId = userId;
        this.username = username;
        this.isOnline = isOnline;
    }

    // Getters et Setters (nécessaires pour la sérialisation JSON)
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public boolean isOnline() { return isOnline; }
    public void setOnline(boolean online) { isOnline = online; }
}