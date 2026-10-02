package com.mining.minecom_server.common.dto;

public class MessageReadNotification {
    private Long senderId;
    private Long readerId;
    private String readerUsername;

    public MessageReadNotification() {}
    public MessageReadNotification(Long senderId, Long readerId, String readerUsername) {
        this.senderId = senderId;
        this.readerId = readerId;
        this.readerUsername = readerUsername;
    }

    public Long getSenderId() { return senderId; }
    public void setSenderId(Long senderId) { this.senderId = senderId; }
    public Long getReaderId() { return readerId; }
    public void setReaderId(Long readerId) { this.readerId = readerId; }
    public String getReaderUsername() { return readerUsername; }
    public void setReaderUsername(String u) { this.readerUsername = u; }
}