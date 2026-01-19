package com.mining.minecom.common.dto;

import java.time.Instant;

public class SOSAcknowledgmentDto {
    private Long alertId;
    private Long userId;
    private String username;
    private Instant timestamp;

    public SOSAcknowledgmentDto() {}

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