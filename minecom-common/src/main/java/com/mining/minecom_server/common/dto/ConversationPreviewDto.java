package com.mining.minecom_server.common.dto;

public class ConversationPreviewDto {
    private Long userId;
    private String username;
    private Boolean isOnline;
    private LastMessageDto lastMessage;

    public ConversationPreviewDto() {}

    public ConversationPreviewDto(Long userId, String username, Boolean isOnline, LastMessageDto lastMessage) {
        this.userId = userId;
        this.username = username;
        this.isOnline = isOnline;
        this.lastMessage = lastMessage;
    }

    // Getters et Setters
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public Boolean getIsOnline() { return isOnline; }
    public void setIsOnline(Boolean isOnline) { this.isOnline = isOnline; }
    public LastMessageDto getLastMessage() { return lastMessage; }
    public void setLastMessage(LastMessageDto lastMessage) { this.lastMessage = lastMessage; }
}