// Dans com.mining.minecom.dto/ConversationPreviewDto.java
package com.mining.minecom.common.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.mining.minecom_server.common.dto.LastMessageDto;
import com.mining.minecom_server.common.dto.TeamDto;

public class ConversationPreviewDto {
    private Long userId;
    private String username;
    private Boolean isOnline;
    private LastMessageDto lastMessage;

    // Renseigné côté client quand l'élément de la liste représente une équipe
    @JsonIgnore
    private TeamDto team;

    public ConversationPreviewDto() {}

    /** Élément de liste représentant une équipe. */
    public static ConversationPreviewDto ofTeam(TeamDto team) {
        ConversationPreviewDto dto = new ConversationPreviewDto();
        dto.setTeam(team);
        return dto;
    }

    // Getters et Setters
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getUsername() { return team != null ? team.getName() : username; }
    public void setUsername(String username) { this.username = username; }
    public Boolean getIsOnline() { return isOnline; }
    public void setIsOnline(Boolean isOnline) { this.isOnline = isOnline; }
    public LastMessageDto getLastMessage() { return team != null ? team.getLastMessage() : lastMessage; }
    public void setLastMessage(LastMessageDto lastMessage) {
        if (team != null) team.setLastMessage(lastMessage);
        else this.lastMessage = lastMessage;
    }

    @JsonIgnore
    public TeamDto getTeam() { return team; }
    @JsonIgnore
    public void setTeam(TeamDto team) { this.team = team; }
    @JsonIgnore
    public boolean isTeamConversation() { return team != null; }
}
