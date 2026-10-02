package com.mining.minecom_server.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "sos_acknowledgments")
public class SOSAcknowledgment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "alert_id", nullable = false)
    private SOSAlert alert;

    // 🔑 CORRECTION : Utiliser UserEntity au lieu de User
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_sos_ack_user"))
    private UserEntity user;

    @Column(nullable = false)
    private Instant timestamp;

    public SOSAcknowledgment() {
        this.timestamp = Instant.now();
    }

    // Getters et Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public SOSAlert getAlert() { return alert; }
    public void setAlert(SOSAlert alert) { this.alert = alert; }

    public UserEntity getUser() { return user; }
    public void setUser(UserEntity user) { this.user = user; }

    public Instant getTimestamp() { return timestamp; }
    public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }
}