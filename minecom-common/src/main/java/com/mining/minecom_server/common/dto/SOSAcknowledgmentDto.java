package com.mining.minecom_server.common.dto;

import java.time.Instant;

public class SOSAcknowledgmentDto {
    private Long alertId;
    private Long userId;
    private String username;
    private Instant timestamp;

    public SOSAcknowledgmentDto() {}

    public SOSAcknowledgmentDto(Long alertId, Long userId, String username) {
        this.alertId = alertId;
        this.userId = userId;
        this.username = username;
        this.timestamp = Instant.now();
    }

    // Getters et Setters
    public Long getAlertId() { return alertId; }
    public void setAlertId(Long alertId) { this.alertId = alertId; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public Instant getTimestamp() { return timestamp; }
    public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }
}