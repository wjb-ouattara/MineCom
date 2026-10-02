// Dans com.mining.minecom.dto/MessageRequest.java

package com.mining.minecom.common.dto;

import com.mining.minecom.common.enums.MessageType;

// Note: Jackson a besoin des getters/setters et d'un constructeur par défaut/paramétré
public class MessageRequest {

    private Long receiverId;
    private Long teamId; // renseigné à la place de receiverId pour un message d'équipe
    private String content;
    private MessageType messageType;
    private Long replyToId;
    private String replyToContent;
    private Long replyToSenderId;
    private String fileUrl;
    private String fileName;
    private String fileType;
    private Long fileSize;


    // Constructeur pour l'envoi
    public MessageRequest(Long receiverId, String content, MessageType messageType) {
        this.receiverId = receiverId;
        this.content = content;
        this.messageType = messageType;
    }

    // Nécessaire pour Jackson (si utilisé)
    public MessageRequest() {}

    // Getters et Setters
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
    public void setReplyToContent(String replyToContent) { this.replyToContent = replyToContent; }

    public Long getReplyToSenderId() { return replyToSenderId; }
    public void setReplyToSenderId(Long replyToSenderId) { this.replyToSenderId = replyToSenderId; }

    public String getFileUrl() { return fileUrl; }
    public void setFileUrl(String fileUrl) { this.fileUrl = fileUrl; }
    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }
    public String getFileType() { return fileType; }
    public void setFileType(String fileType) { this.fileType = fileType; }
    public Long getFileSize() { return fileSize; }
    public void setFileSize(Long fileSize) { this.fileSize = fileSize; }
}