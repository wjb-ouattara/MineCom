package com.mining.minecom_server.common.dto;

import java.time.Instant;

public class SOSAlertDto {
    private Long id;
    private Long senderId;
    private String senderUsername;
    private String location;
    private String description;
    private Instant timestamp;
    private Integer acknowledgedCount;
    private Integer totalOnlineUsers;

    // Constructeurs
    public SOSAlertDto() {}

    public SOSAlertDto(Long id, Long senderId, String senderUsername, String location,
                       String description, Instant timestamp) {
        this.id = id;
        this.senderId = senderId;
        this.senderUsername = senderUsername;
        this.location = location;
        this.description = description;
        this.timestamp = timestamp;
        this.acknowledgedCount = 0;
        this.totalOnlineUsers = 0;
    }

    // Getters et Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getSenderId() { return senderId; }
    public void setSenderId(Long senderId) { this.senderId = senderId; }

    public String getSenderUsername() { return senderUsername; }
    public void setSenderUsername(String senderUsername) { this.senderUsername = senderUsername; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Instant getTimestamp() { return timestamp; }
    public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }

    public Integer getAcknowledgedCount() { return acknowledgedCount; }
    public void setAcknowledgedCount(Integer acknowledgedCount) {
        this.acknowledgedCount = acknowledgedCount;
    }

    public Integer getTotalOnlineUsers() { return totalOnlineUsers; }
    public void setTotalOnlineUsers(Integer totalOnlineUsers) {
        this.totalOnlineUsers = totalOnlineUsers;
    }
}