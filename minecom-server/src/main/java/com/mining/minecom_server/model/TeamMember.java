package com.mining.minecom_server.model;

import jakarta.persistence.*;

import java.time.Instant;

/**
 * Appartenance d'un utilisateur à une équipe. Les admins gèrent les membres.
 */
@Entity
@Table(name = "team_member",
        uniqueConstraints = @UniqueConstraint(name = "uk_team_member", columnNames = {"team_id", "user_id"}))
public class TeamMember {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id", nullable = false)
    private TeamEntity team;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @Column(name = "is_admin", nullable = false)
    private boolean admin = false;

    @Column(name = "joined_at", nullable = false)
    private Instant joinedAt = Instant.now();

    public TeamMember() {}

    public TeamMember(TeamEntity team, UserEntity user, boolean admin) {
        this.team = team;
        this.user = user;
        this.admin = admin;
    }

    public Long getId() { return id; }
    public TeamEntity getTeam() { return team; }
    public void setTeam(TeamEntity team) { this.team = team; }
    public UserEntity getUser() { return user; }
    public void setUser(UserEntity user) { this.user = user; }
    public boolean isAdmin() { return admin; }
    public void setAdmin(boolean admin) { this.admin = admin; }
    public Instant getJoinedAt() { return joinedAt; }
}
