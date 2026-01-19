package com.mining.minecom_server.common.dto;

import java.time.Instant;

public class LastMessageDto {
    private String content;
    private Instant timestamp;
    private boolean isSentByMe;

    public LastMessageDto() {}

    public LastMessageDto(String content, Instant timestamp, boolean isSentByMe) {
        this.content = content;
        this.timestamp = timestamp;
        this.isSentByMe = isSentByMe;
    }

    // Getters et Setters
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public Instant getTimestamp() { return timestamp; }
    public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }
    public boolean isSentByMe() { return isSentByMe; }
    public void setSentByMe(boolean sentByMe) { isSentByMe = sentByMe; }
}