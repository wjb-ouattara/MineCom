package com.mining.minecom_server.common.dto;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Équipe (groupe de discussion privé) — partagé client / serveur.
 * Seuls les membres reçoivent ce DTO : le serveur ne l'expose jamais aux autres utilisateurs.
 */
public class TeamDto {
    private Long id;
    private String name;
    private String description;
    private Long createdById;
    private Instant createdAt;
    private List<TeamMemberDto> members = new ArrayList<>();
    private LastMessageDto lastMessage;

    public TeamDto() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Long getCreatedById() { return createdById; }
    public void setCreatedById(Long createdById) { this.createdById = createdById; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public List<TeamMemberDto> getMembers() { return members; }
    public void setMembers(List<TeamMemberDto> members) { this.members = members; }
    public LastMessageDto getLastMessage() { return lastMessage; }
    public void setLastMessage(LastMessageDto lastMessage) { this.lastMessage = lastMessage; }
}
