// Dans com.mining.minecom.dto/MessageRequest.java

package com.mining.minecom.common.dto;

import com.mining.minecom.common.enums.MessageType;

// Note: Jackson a besoin des getters/setters et d'un constructeur par défaut/paramétré
public class MessageRequest {

    private Long receiverId;
    private String content;
    private MessageType messageType;

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
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public MessageType getMessageType() { return messageType; }
    public void setMessageType(MessageType messageType) { this.messageType = messageType; }
}