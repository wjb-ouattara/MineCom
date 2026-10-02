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
    private Long teamId;          // non null pour un message d'équipe
    private String senderUsername;
    private String content;
    private Instant timestamp;
    private MessageType messageType;
    private MessageStatus status;
    private Long replyToId;
    private String replyToContent;
    private Long replyToSenderId;
    private Boolean isEdited = false;
    private Boolean isDeleted = false;
    private String fileUrl;
    private String fileName;
    private String fileType;
    private Long fileSize;

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

    public Long getReplyToId() { return replyToId; }
    public void setReplyToId(Long replyToId) { this.replyToId = replyToId; }
    public String getReplyToContent() { return replyToContent; }
    public void setReplyToContent(String c) { this.replyToContent = c; }
    public Long getReplyToSenderId() { return replyToSenderId; }
    public void setReplyToSenderId(Long id) { this.replyToSenderId = id; }
    public Boolean getIsEdited() { return isEdited; }
    public void setIsEdited(Boolean isEdited) { this.isEdited = isEdited; }
    public Boolean getIsDeleted() { return isDeleted; }
    public void setIsDeleted(Boolean isDeleted) { this.isDeleted = isDeleted; }

    public Long getTeamId() { return teamId; }
    public void setTeamId(Long teamId) { this.teamId = teamId; }
    public String getSenderUsername() { return senderUsername; }
    public void setSenderUsername(String senderUsername) { this.senderUsername = senderUsername; }

    public String getFileUrl() { return fileUrl; }
    public void setFileUrl(String fileUrl) { this.fileUrl = fileUrl; }
    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }
    public String getFileType() { return fileType; }
    public void setFileType(String fileType) { this.fileType = fileType; }
    public Long getFileSize() { return fileSize; }
    public void setFileSize(Long fileSize) { this.fileSize = fileSize; }
}