package com.mining.minecom_server.model;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "sos_alerts")
public class SOSAlert {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 🔑 CORRECTION : Référencer la bonne table app_user
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "sender_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_sos_alert_sender"))
    private UserEntity sender;

    @Column(nullable = false)
    private String location;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private Instant timestamp;

    @Column(nullable = false)
    private Boolean isActive = true;

    @OneToMany(mappedBy = "alert", cascade = CascadeType.ALL)
    private Set<SOSAcknowledgment> acknowledgments = new HashSet<>();

    public SOSAlert() {
        this.timestamp = Instant.now();
    }

    // Getters et Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public UserEntity getSender() { return sender; }
    public void setSender(UserEntity sender) { this.sender = sender; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Instant getTimestamp() { return timestamp; }
    public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }

    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean active) { isActive = active; }

    public Set<SOSAcknowledgment> getAcknowledgments() { return acknowledgments; }
    public void setAcknowledgments(Set<SOSAcknowledgment> acknowledgments) {
        this.acknowledgments = acknowledgments;
    }
}