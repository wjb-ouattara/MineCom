package com.mining.minecom_server.model;

import com.mining.minecom_server.common.enums.MessageStatus;
import com.mining.minecom_server.common.enums.MessageType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

/**
 * Entité JPA Message pour la base de données
 */
@Setter
@Getter
@Entity
@Table(name = "message")
public class MessageEntity {

    // Getters et Setters
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "sender_id", nullable = false)
    private UserEntity sender;

    // Null pour un message d'équipe (le destinataire est alors 'team')
    @ManyToOne
    @JoinColumn(name = "receiver_id")
    private UserEntity receiver;

    @ManyToOne
    @JoinColumn(name = "team_id")
    private TeamEntity team;

    @Column(name = "content", columnDefinition = "TEXT", nullable = false)
    private String content;

    @Column(name = "timestamp", nullable = false)
    private Instant timestamp = Instant.now();

    @Enumerated(EnumType.STRING)
    @Column(name = "message_type", length = 20)
    private MessageType messageType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20)
    private MessageStatus status = MessageStatus.SENT;

    @Column(name = "reply_to_id")
    private Long replyToId;

    @Column(name = "reply_to_content", columnDefinition = "TEXT")
    private String replyToContent;

    @Column(name = "reply_to_sender_id")
    private Long replyToSenderId;

    @Column(name = "is_edited")
    private Boolean isEdited = false;

    @Column(name = "is_deleted")
    private Boolean isDeleted = false;

    @Column(name = "file_url")
    private String fileUrl;

    @Column(name = "file_name")
    private String fileName;

    @Column(name = "file_type")
    private String fileType;

    @Column(name = "file_size")
    private Long fileSize;

    // Constructeurs
    public MessageEntity() {}

    public MessageEntity(UserEntity sender, UserEntity receiver, String content,
                         MessageType messageType) {
        this.sender = sender;
        this.receiver = receiver;
        this.content = content;
        this.messageType = messageType;
    }

    // Getters/Setters
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

    public String getFileUrl() { return fileUrl; }
    public void setFileUrl(String u) { this.fileUrl = u; }
    public String getFileName() { return fileName; }
    public void setFileName(String n) { this.fileName = n; }
    public String getFileType() { return fileType; }
    public void setFileType(String t) { this.fileType = t; }
    public Long getFileSize() { return fileSize; }
    public void setFileSize(Long s) { this.fileSize = s; }

}