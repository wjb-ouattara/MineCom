package com.mining.minecom_server.common.dto;

import java.util.ArrayList;
import java.util.List;

/**
 * Création d'une équipe, ou ajout de membres (seul memberIds est alors utilisé).
 */
public class TeamRequest {
    private String name;
    private String description;
    private List<Long> memberIds = new ArrayList<>();

    public TeamRequest() {}

    public TeamRequest(String name, String description, List<Long> memberIds) {
        this.name = name;
        this.description = description;
        this.memberIds = memberIds;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public List<Long> getMemberIds() { return memberIds; }
    public void setMemberIds(List<Long> memberIds) { this.memberIds = memberIds; }
}
