package com.mining.minecom_server.common.dto;

import com.mining.minecom_server.common.enums.MessageStatus;
import com.mining.minecom_server.common.enums.MessageType;

import java.time.Instant;

/**
 * DTO pour les réponses de messages
 */
public class MessageResponse {

    private Long messageId;
    private Long senderId;
    private Long receiverId;
    private String content;
    private Instant timestamp;
    private MessageType messageType;
    private MessageStatus status;

    // Constructeur par défaut (nécessaire pour la désérialisation par Jackson)
    public MessageResponse() {
    }

    // Constructeur complet pour faciliter la création
    public MessageResponse(Long messageId, Long senderId, Long receiverId,
                           String content, Instant timestamp,
                           MessageType messageType, MessageStatus status) {
        this.messageId = messageId;
        this.senderId = senderId;
        this.receiverId = receiverId;
        this.content = content;
        this.timestamp = timestamp;
        this.messageType = messageType;
        this.status = status;
    }

    // --- GETTERS ET SETTERS ---

    public Long getMessageId() {
        return messageId;
    }

    public void setMessageId(Long messageId) {
        this.messageId = messageId;
    }

    public Long getSenderId() {
        return senderId;
    }

    public void setSenderId(Long senderId) {
        this.senderId = senderId;
    }

    public Long getReceiverId() {
        return receiverId;
    }

    public void setReceiverId(Long receiverId) {
        this.receiverId = receiverId;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

    public MessageType getMessageType() {
        return messageType;
    }

    public void setMessageType(MessageType messageType) {
        this.messageType = messageType;
    }

    public MessageStatus getStatus() {
        return status;
    }

    public void setStatus(MessageStatus status) {
        this.status = status;
    }
}