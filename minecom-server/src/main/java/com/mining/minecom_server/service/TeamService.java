package com.mining.minecom_server.service;

import com.mining.minecom_server.common.dto.LastMessageDto;
import com.mining.minecom_server.common.dto.TeamDto;
import com.mining.minecom_server.common.dto.TeamMemberDto;
import com.mining.minecom_server.common.dto.TeamRequest;
import com.mining.minecom_server.model.MessageEntity;
import com.mining.minecom_server.model.TeamEntity;
import com.mining.minecom_server.model.TeamMember;
import com.mining.minecom_server.model.UserEntity;
import com.mining.minecom_server.repository.MessageRepository;
import com.mining.minecom_server.repository.TeamMemberRepository;
import com.mining.minecom_server.repository.TeamRepository;
import com.mining.minecom_server.repository.UserRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;

/**
 * Gestion des équipes. Toute lecture/écriture passe par une vérification d'appartenance :
 * un utilisateur hors de l'équipe ne peut ni voir l'équipe, ni ses membres, ni ses messages.
 */
@Service
public class TeamService {

    private final TeamRepository teamRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final UserRepository userRepository;
    private final MessageRepository messageRepository;
    private final SimpMessagingTemplate messagingTemplate;

    public TeamService(TeamRepository teamRepository,
                       TeamMemberRepository teamMemberRepository,
                       UserRepository userRepository,
                       MessageRepository messageRepository,
                       SimpMessagingTemplate messagingTemplate) {
        this.teamRepository = teamRepository;
        this.teamMemberRepository = teamMemberRepository;
        this.userRepository = userRepository;
        this.messageRepository = messageRepository;
        this.messagingTemplate = messagingTemplate;
    }

    // ================================================================
    // ACCÈS
    // ================================================================

    public UserEntity getUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Utilisateur non trouvé : " + username));
    }

    public boolean isMember(Long teamId, Long userId) {
        return teamMemberRepository.existsByTeamIdAndUserId(teamId, userId);
    }

    /**
     * Retourne l'équipe si l'utilisateur en est membre. Sinon 404 : on ne révèle même pas
     * l'existence de l'équipe à un non-membre.
     */
    public TeamEntity requireMembership(Long teamId, Long userId) {
        if (!isMember(teamId, userId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Équipe introuvable");
        }
        return teamRepository.findById(teamId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Équipe introuvable"));
    }

    private TeamEntity requireAdmin(Long teamId, Long userId) {
        TeamEntity team = requireMembership(teamId, userId);
        boolean admin = team.getMembers().stream()
                .anyMatch(m -> m.getUser().getId().equals(userId) && m.isAdmin());
        if (!admin) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Réservé aux administrateurs de l'équipe");
        }
        return team;
    }

    /** Noms d'utilisateur des membres (destinataires des diffusions WebSocket). */
    public List<String> memberUsernames(TeamEntity team) {
        return team.getMembers().stream().map(m -> m.getUser().getUsername()).toList();
    }

    // ================================================================
    // LECTURE
    // ================================================================

    @Transactional(readOnly = true)
    public List<TeamDto> getTeamsOf(String username) {
        UserEntity user = getUser(username);
        return teamRepository.findByMemberId(user.getId()).stream()
                .map(t -> toDto(t, user.getId()))
                .sorted(Comparator.comparing(
                        (TeamDto d) -> d.getLastMessage() != null ? d.getLastMessage().getTimestamp() : d.getCreatedAt(),
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
    }

    @Transactional(readOnly = true)
    public TeamDto getTeam(String username, Long teamId) {
        UserEntity user = getUser(username);
        return toDto(requireMembership(teamId, user.getId()), user.getId());
    }

    @Transactional(readOnly = true)
    public List<MessageEntity> getTeamMessages(String username, Long teamId) {
        UserEntity user = getUser(username);
        requireMembership(teamId, user.getId());
        return messageRepository.findByTeamId(teamId);
    }

    // ================================================================
    // ÉCRITURE
    // ================================================================

    @Transactional
    public TeamDto createTeam(String creatorUsername, TeamRequest request) {
        UserEntity creator = getUser(creatorUsername);

        String name = request.getName() != null ? request.getName().trim() : "";
        if (name.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le nom de l'équipe est obligatoire");
        }
        if (name.length() > 80) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nom trop long (80 caractères max)");
        }

        TeamEntity team = new TeamEntity();
        team.setName(name);
        String description = request.getDescription() != null ? request.getDescription().trim() : "";
        team.setDescription(description.isEmpty() ? null
                : description.substring(0, Math.min(255, description.length())));
        team.setCreatedBy(creator);

        // Le créateur est administrateur
        team.getMembers().add(new TeamMember(team, creator, true));
        for (UserEntity user : resolveUsers(request.getMemberIds())) {
            if (!user.getId().equals(creator.getId())) {
                team.getMembers().add(new TeamMember(team, user, false));
            }
        }

        TeamEntity saved = teamRepository.save(team);
        System.out.println("👥 Équipe '" + saved.getName() + "' créée par " + creatorUsername
                + " (" + saved.getMembers().size() + " membres)");

        notifyTeamUpdated(saved);
        return toDto(saved, creator.getId());
    }

    @Transactional
    public TeamDto addMembers(String username, Long teamId, List<Long> userIds) {
        UserEntity actor = getUser(username);
        TeamEntity team = requireAdmin(teamId, actor.getId());

        Set<Long> existing = new HashSet<>();
        team.getMembers().forEach(m -> existing.add(m.getUser().getId()));

        for (UserEntity user : resolveUsers(userIds)) {
            if (existing.add(user.getId())) {
                team.getMembers().add(new TeamMember(team, user, false));
            }
        }

        TeamEntity saved = teamRepository.save(team);
        notifyTeamUpdated(saved);
        return toDto(saved, actor.getId());
    }

    /**
     * Retire un membre. Un admin peut retirer n'importe qui ; tout membre peut se retirer
     * lui-même (quitter l'équipe). L'équipe est supprimée quand il ne reste plus personne.
     */
    @Transactional
    public void removeMember(String username, Long teamId, Long userId) {
        UserEntity actor = getUser(username);
        TeamEntity team = actor.getId().equals(userId)
                ? requireMembership(teamId, actor.getId())
                : requireAdmin(teamId, actor.getId());

        TeamMember removed = team.getMembers().stream()
                .filter(m -> m.getUser().getId().equals(userId))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Membre introuvable"));
        String removedUsername = removed.getUser().getUsername();
        team.getMembers().remove(removed);

        TeamDto removedEvent = new TeamDto();
        removedEvent.setId(teamId);
        removedEvent.setName(team.getName());

        if (team.getMembers().isEmpty()) {
            // Plus personne : on supprime l'équipe et son historique
            messageRepository.deleteAll(messageRepository.findByTeamId(teamId));
            teamRepository.delete(team);
            System.out.println("🗑️ Équipe " + teamId + " supprimée (plus aucun membre)");
        } else {
            // Une équipe garde toujours au moins un admin
            if (team.getMembers().stream().noneMatch(TeamMember::isAdmin)) {
                team.getMembers().get(0).setAdmin(true);
            }
            teamRepository.save(team);
            notifyTeamUpdated(team);
        }

        // L'ancien membre retire l'équipe de sa liste
        messagingTemplate.convertAndSendToUser(removedUsername, "/queue/team-removed", removedEvent);
    }

    // ================================================================
    // UTILITAIRES
    // ================================================================

    private List<UserEntity> resolveUsers(List<Long> ids) {
        if (ids == null || ids.isEmpty()) return List.of();
        return userRepository.findAllById(new LinkedHashSet<>(ids));
    }

    /** Envoie la nouvelle version de l'équipe à chacun de ses membres (et à eux seuls). */
    private void notifyTeamUpdated(TeamEntity team) {
        for (TeamMember member : team.getMembers()) {
            UserEntity user = member.getUser();
            messagingTemplate.convertAndSendToUser(user.getUsername(), "/queue/team-updated",
                    toDto(team, user.getId()));
        }
    }

    public TeamDto toDto(TeamEntity team, Long viewerId) {
        TeamDto dto = new TeamDto();
        dto.setId(team.getId());
        dto.setName(team.getName());
        dto.setDescription(team.getDescription());
        dto.setCreatedById(team.getCreatedBy().getId());
        dto.setCreatedAt(team.getCreatedAt());

        List<TeamMemberDto> members = new ArrayList<>();
        for (TeamMember m : team.getMembers()) {
            UserEntity u = m.getUser();
            members.add(new TeamMemberDto(u.getId(), u.getUsername(),
                    Boolean.TRUE.equals(u.getIsOnline()), m.isAdmin()));
        }
        members.sort(Comparator.comparing(TeamMemberDto::isAdmin).reversed()
                .thenComparing(TeamMemberDto::getUsername, String.CASE_INSENSITIVE_ORDER));
        dto.setMembers(members);

        if (team.getId() != null) {
            List<MessageEntity> last = messageRepository.findLatestByTeamId(team.getId(), PageRequest.of(0, 1));
            if (!last.isEmpty()) {
                MessageEntity msg = last.get(0);
                boolean mine = msg.getSender().getId().equals(viewerId);
                dto.setLastMessage(new LastMessageDto(previewText(msg, mine), msg.getTimestamp(), mine));
            }
        }
        return dto;
    }

    /** Aperçu du dernier message, préfixé par l'auteur (comme dans un groupe WhatsApp). */
    private String previewText(MessageEntity msg, boolean mine) {
        String text;
        if (Boolean.TRUE.equals(msg.getIsDeleted())) {
            text = "🚫 Message supprimé";
        } else if (msg.getFileType() != null && msg.getFileType().startsWith("image/")) {
            text = "📷 Photo";
        } else if (msg.getFileUrl() != null) {
            text = "📄 " + (msg.getFileName() != null ? msg.getFileName() : "Document");
        } else {
            text = msg.getContent() != null ? msg.getContent() : "";
        }
        return mine ? text : msg.getSender().getUsername() + ": " + text;
    }
}
