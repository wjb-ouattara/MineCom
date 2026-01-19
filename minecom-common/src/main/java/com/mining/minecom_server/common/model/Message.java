package com.mining.minecom_server.common.model;

import com.mining.minecom_server.common.enums.MessageStatus;
import com.mining.minecom_server.common.enums.MessageType;
import java.time.Instant;

/**
 * Version POJO de Message (sans annotations JPA)
 * Pour le transfert de données entre client et serveur
 */
public class Message {

    private Long id;
    private User sender;        // Objet User complet
    private User receiver;      // Objet User complet
    private String content;
    private Instant timestamp = Instant.now();
    private MessageType messageType;
    private MessageStatus status = MessageStatus.SENT;

    // Constructeurs
    public Message(Long id, User sender, User receiver, String content,
                   Instant timestamp, MessageType messageType, MessageStatus status) {
        this.id = id;
        this.sender = sender;
        this.receiver = receiver;
        this.content = content;
        this.timestamp = timestamp;
        this.messageType = messageType;
        this.status = status;
    }

    // Getters et Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getSender() { return sender; }
    public void setSender(User sender) { this.sender = sender; }

    public User getReceiver() { return receiver; }
    public void setReceiver(User receiver) { this.receiver = receiver; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public Instant getTimestamp() { return timestamp; }
    public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }

    public MessageType getMessageType() { return messageType; }
    public void setMessageType(MessageType messageType) { this.messageType = messageType; }

    public MessageStatus getStatus() { return status; }
    public void setStatus(MessageStatus status) { this.status = status; }
}