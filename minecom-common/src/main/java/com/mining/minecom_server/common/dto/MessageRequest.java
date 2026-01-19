// Dans com.mining.minecom_server.dto/MessageRequest.java

package com.mining.minecom_server.common.dto;

import com.mining.minecom_server.common.enums.MessageType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class MessageRequest {

    @NotNull
    private Long receiverId; // L'ID de l'utilisateur destinataire

    @NotBlank
    private String content; // Le contenu du message (texte ou URL de fichier)

    @NotNull
    private MessageType messageType; // Type (TEXT, IMAGE, ALERTE, etc.)

    // Getters, Setters, et Constructeurs
    public Long getReceiverId() { return receiverId; }
    public void setReceiverId(Long receiverId) { this.receiverId = receiverId; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public MessageType getMessageType() { return messageType; }
    public void setMessageType(MessageType messageType) { this.messageType = messageType; }
}