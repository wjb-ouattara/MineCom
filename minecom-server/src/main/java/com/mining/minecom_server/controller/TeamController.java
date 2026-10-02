package com.mining.minecom_server.controller;

import com.mining.minecom_server.common.dto.TeamDto;
import com.mining.minecom_server.common.dto.TeamRequest;
import com.mining.minecom_server.service.TeamService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Équipes (groupes privés). Les vérifications d'appartenance sont faites dans TeamService :
 * un non-membre reçoit 404, un membre non-admin 403 sur les actions de gestion.
 */
@RestController
@RequestMapping("/api/teams")
public class TeamController {

    private final TeamService teamService;

    public TeamController(TeamService teamService) {
        this.teamService = teamService;
    }

    /** Équipes de l'utilisateur connecté (uniquement celles dont il est membre). */
    @GetMapping
    public ResponseEntity<List<TeamDto>> getMyTeams(Authentication authentication) {
        return ResponseEntity.ok(teamService.getTeamsOf(authentication.getName()));
    }

    @PostMapping
    public ResponseEntity<TeamDto> createTeam(Authentication authentication, @RequestBody TeamRequest request) {
        return ResponseEntity.ok(teamService.createTeam(authentication.getName(), request));
    }

    @GetMapping("/{teamId}")
    public ResponseEntity<TeamDto> getTeam(Authentication authentication, @PathVariable Long teamId) {
        return ResponseEntity.ok(teamService.getTeam(authentication.getName(), teamId));
    }

    /** Ajouter des membres (admin). Body : { "memberIds": [..] } */
    @PostMapping("/{teamId}/members")
    public ResponseEntity<TeamDto> addMembers(Authentication authentication,
                                              @PathVariable Long teamId,
                                              @RequestBody TeamRequest request) {
        return ResponseEntity.ok(teamService.addMembers(authentication.getName(), teamId, request.getMemberIds()));
    }

    /** Retirer un membre (admin) ou quitter l'équipe (userId = soi-même). */
    @DeleteMapping("/{teamId}/members/{userId}")
    public ResponseEntity<Void> removeMember(Authentication authentication,
                                             @PathVariable Long teamId,
                                             @PathVariable Long userId) {
        teamService.removeMember(authentication.getName(), teamId, userId);
        return ResponseEntity.ok().build();
    }
}
