// Dans com.mining.minecom_server.dto/MessageRequest.java

package com.mining.minecom_server.common.dto;

import com.mining.minecom_server.common.enums.MessageType;
import jakarta.validation.constraints.NotNull;

public class MessageRequest {

    // Destinataire : soit un utilisateur (receiverId), soit une équipe (teamId)
    private Long receiverId;
    private Long teamId;

    // Peut être vide pour un message IMAGE/FILE (le fichier est dans fileUrl)
    private String content;

    @NotNull
    private MessageType messageType; // Type (TEXT, IMAGE, ALERTE, etc.)

    private Long replyToId;
    private String replyToContent;
    private Long replyToSenderId;

    // Pièce jointe (image/PDF stocké dans MinIO)
    private String fileUrl;
    private String fileName;
    private String fileType;
    private Long fileSize;

    // Getters, Setters, et Constructeurs
    public Long getReceiverId() { return receiverId; }
    public void setReceiverId(Long receiverId) { this.receiverId = receiverId; }
    public Long getTeamId() { return teamId; }
    public void setTeamId(Long teamId) { this.teamId = teamId; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public MessageType getMessageType() { return messageType; }
    public void setMessageType(MessageType messageType) { this.messageType = messageType; }
    public Long getReplyToId() { return replyToId; }
    public void setReplyToId(Long replyToId) { this.replyToId = replyToId; }
    public String getReplyToContent() { return replyToContent; }
    public void setReplyToContent(String c) { this.replyToContent = c; }
    public Long getReplyToSenderId() { return replyToSenderId; }
    public void setReplyToSenderId(Long id) { this.replyToSenderId = id; }

    public String getFileUrl() { return fileUrl; }
    public void setFileUrl(String fileUrl) { this.fileUrl = fileUrl; }
    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }
    public String getFileType() { return fileType; }
    public void setFileType(String fileType) { this.fileType = fileType; }
    public Long getFileSize() { return fileSize; }
    public void setFileSize(Long fileSize) { this.fileSize = fileSize; }
}
