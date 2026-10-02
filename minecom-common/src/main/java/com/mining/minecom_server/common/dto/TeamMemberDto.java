package com.mining.minecom_server.common.dto;

/**
 * Membre d'une équipe (partagé client / serveur).
 */
public class TeamMemberDto {
    private Long userId;
    private String username;
    private Boolean isOnline;
    private boolean admin;

    public TeamMemberDto() {}

    public TeamMemberDto(Long userId, String username, Boolean isOnline, boolean admin) {
        this.userId = userId;
        this.username = username;
        this.isOnline = isOnline;
        this.admin = admin;
    }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public Boolean getIsOnline() { return isOnline; }
    public void setIsOnline(Boolean isOnline) { this.isOnline = isOnline; }
    public boolean isAdmin() { return admin; }
    public void setAdmin(boolean admin) { this.admin = admin; }
}
