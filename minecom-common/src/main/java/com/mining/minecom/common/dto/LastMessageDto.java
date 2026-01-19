// Dans com.mining.minecom.dto/LastMessageDto.java
package com.mining.minecom.common.dto;

import java.time.Instant;

public class LastMessageDto {
    private String content;
    private Instant timestamp;
    private boolean isSentByMe;

    public LastMessageDto() {}

    // Getters et Setters
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public Instant getTimestamp() { return timestamp; }
    public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }
    public boolean isSentByMe() { return isSentByMe; }
    public void setSentByMe(boolean sentByMe) { isSentByMe = sentByMe; }
}