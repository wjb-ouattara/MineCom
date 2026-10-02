package com.mining.minecom_server.controller;

import com.mining.minecom_server.common.dto.MessageReadNotification;
import com.mining.minecom_server.common.dto.MessageRequest;
import com.mining.minecom_server.common.dto.MessageResponse;
import com.mining.minecom_server.model.MessageEntity;
import com.mining.minecom_server.model.UserEntity;
import com.mining.minecom_server.repository.MessageRepository;
import com.mining.minecom_server.repository.UserRepository;
import com.mining.minecom_server.service.MessageService;
import com.mining.minecom_server.service.StorageService;
import com.mining.minecom_server.service.TeamService;
import com.mining.minecom_server.model.TeamEntity;
import java.security.Principal;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/messages")
public class MessageController {

    private final MessageService messageService;
    private final SimpMessagingTemplate messagingTemplate;
    private final UserRepository userRepository;
    private final MessageRepository messageRepository;
    private final StorageService storageService;
    private final TeamService teamService;


    // 🔑 Constructeur mis à jour
    public MessageController(MessageService messageService,
                             SimpMessagingTemplate messagingTemplate,
                             UserRepository userRepository,
                             MessageRepository messageRepository,
                             StorageService storageService,
                             TeamService teamService) {
        this.messageService = messageService;
        this.messagingTemplate = messagingTemplate;
        this.userRepository = userRepository;
        this.messageRepository = messageRepository;
        this.storageService = storageService;
        this.teamService = teamService;
    }

    @MessageMapping("/private-message")
    public void handlePrivateMessage(MessageRequest request, Principal principal) {
        if (request.getTeamId() != null) {
            handleTeamMessage(request, principal);
            return;
        }
        String senderUsername = principal.getName();

        System.out.println("📨 Message de " + senderUsername + " → receiverId=" + request.getReceiverId());

        // 1. Sauvegarder
        MessageEntity savedMessage = messageService.sendMessage(senderUsername, request);
        MessageResponse response = convertToResponse(savedMessage);

        // 🔑 FIX : Trouver le USERNAME du destinataire (pas l'ID)
        userRepository.findById(request.getReceiverId())
                .ifPresent(receiver -> {
                    messagingTemplate.convertAndSendToUser(
                            receiver.getUsername(), // ← "Innocent" au lieu de "4"
                            "/queue/messages",
                            response
                    );
                    System.out.println("✅ Message envoyé à : " + receiver.getUsername());
                });
    }

    /**
     * Message d'équipe : enregistré puis remis uniquement aux membres de l'équipe, chacun sur
     * sa file privée (/user/queue/team-messages). Aucun topic partagé : un non-membre ne peut
     * pas s'abonner pour écouter.
     */
    @MessageMapping("/team-message")
    public void handleTeamMessage(MessageRequest request, Principal principal) {
        String senderUsername = principal.getName();
        if (request.getTeamId() == null) return;

        MessageEntity savedMessage;
        try {
            savedMessage = messageService.sendMessage(senderUsername, request);
        } catch (Exception e) {
            // Non-membre ou équipe inexistante : message refusé
            System.err.println("⛔ Message d'équipe refusé pour " + senderUsername
                    + " → teamId=" + request.getTeamId() + " : " + e.getMessage());
            return;
        }
        MessageResponse response = convertToResponse(savedMessage);

        for (String member : teamService.memberUsernames(savedMessage.getTeam())) {
            if (!member.equals(senderUsername)) {
                messagingTemplate.convertAndSendToUser(member, "/queue/team-messages", response);
            }
        }
        System.out.println("👥 Message d'équipe " + savedMessage.getTeam().getId() + " diffusé par " + senderUsername);
    }

    /** Diffuse une mise à jour (édition/suppression) aux participants du message. */
    private void broadcastUpdate(MessageEntity msg, String destination, MessageResponse response) {
        TeamEntity team = msg.getTeam();
        if (team != null) {
            for (String member : teamService.memberUsernames(team)) {
                messagingTemplate.convertAndSendToUser(member, destination, response);
            }
        } else {
            messagingTemplate.convertAndSendToUser(msg.getReceiver().getUsername(), destination, response);
            messagingTemplate.convertAndSendToUser(msg.getSender().getUsername(), destination, response);
        }
    }

    /**
     * Endpoint pour envoyer un nouveau message.
     * @param authentication Fourni par Spring Security (contexte JWT).
     * @param request Le DTO du message.
     */
    @PostMapping
    public ResponseEntity<MessageResponse> sendMessage(
            Authentication authentication,
            @Valid @RequestBody MessageRequest request) {

        String senderUsername = authentication.getName();

        MessageEntity savedMessage = messageService.sendMessage(senderUsername, request);

        // Mapper MessageEntity vers MessageResponse
        MessageResponse response = convertToResponse(savedMessage);

        return ResponseEntity.ok(response);
    }

    /**
     * Endpoint pour récupérer l'historique de conversation avec un partenaire.
     * @param partnerId L'ID de l'utilisateur avec qui l'on veut parler.
     */
    @GetMapping("/{partnerId}")
    public ResponseEntity<List<MessageResponse>> getConversation(
            Authentication authentication,
            @PathVariable Long partnerId) {

        String currentUsername = authentication.getName();

        List<MessageEntity> conversation = messageService.getConversation(currentUsername, partnerId);

        // Mapper List<MessageEntity> vers List<MessageResponse>
        List<MessageResponse> responseList = conversation.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());

        return ResponseEntity.ok(responseList);
    }

    /**
     * Historique d'une équipe : 404 si l'utilisateur n'en est pas membre.
     */
    @GetMapping("/team/{teamId}")
    public ResponseEntity<List<MessageResponse>> getTeamConversation(
            Authentication authentication,
            @PathVariable Long teamId) {
        List<MessageResponse> responseList = teamService.getTeamMessages(authentication.getName(), teamId)
                .stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responseList);
    }

    // Ajoutez cet endpoint pour marquer comme lu
    @PostMapping("/read/{senderId}")
    public ResponseEntity<Void> markAsRead(
            @PathVariable Long senderId,
            Authentication authentication) {
        try {
            String username = authentication.getName();
            UserEntity reader = userRepository.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            // Marquer tous les messages de senderId → reader comme lus
            messageRepository.markMessagesAsRead(senderId, reader.getId());

            // Notifier le sender via WebSocket
            messagingTemplate.convertAndSendToUser(
                    userRepository.findById(senderId)
                            .map(UserEntity::getUsername).orElse(""),
                    "/queue/message-read",
                    new MessageReadNotification(senderId, reader.getId(), username)
            );

            System.out.println("✅ Messages de " + senderId + " marqués lus par " + username);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            System.err.println("❌ Erreur markAsRead: " + e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Convertit une MessageEntity en MessageResponse
     */
    private MessageResponse convertToResponse(MessageEntity entity) {
        MessageResponse response = new MessageResponse();
        response.setMessageId(entity.getId());
        response.setSenderId(entity.getSender().getId());
        response.setReceiverId(entity.getReceiver() != null ? entity.getReceiver().getId() : null);
        response.setTeamId(entity.getTeam() != null ? entity.getTeam().getId() : null);
        response.setSenderUsername(entity.getSender().getUsername());
        response.setContent(entity.getContent());
        response.setTimestamp(entity.getTimestamp());
        response.setMessageType(entity.getMessageType());
        response.setStatus(entity.getStatus());

        response.setReplyToId(entity.getReplyToId());
        response.setReplyToContent(entity.getReplyToContent());
        response.setReplyToSenderId(entity.getReplyToSenderId());
        response.setIsEdited(entity.getIsEdited());
        response.setIsDeleted(entity.getIsDeleted());

        // Pièce jointe : URL présignée régénérée à chaque lecture (valide 7 jours)
        if (entity.getFileUrl() != null && !Boolean.TRUE.equals(entity.getIsDeleted())) {
            response.setFileUrl(storageService.resolveFileUrl(entity.getFileUrl()));
            response.setFileName(entity.getFileName());
            response.setFileType(entity.getFileType());
            response.setFileSize(entity.getFileSize());
        }
        return response;
    }

    // 🔑 MODIFIER un message
    @PutMapping("/{messageId}")
    public ResponseEntity<MessageResponse> editMessage(
            @PathVariable Long messageId,
            @RequestBody Map<String, String> body,
            Authentication authentication) {
        try {
            String username = authentication.getName();
            MessageEntity msg = messageRepository.findById(messageId)
                    .orElseThrow(() -> new RuntimeException("Message non trouvé"));

            if (!msg.getSender().getUsername().equals(username)) {
                return ResponseEntity.status(403).build();
            }

            msg.setContent(body.get("content"));
            messageRepository.save(msg);

            MessageResponse response = convertToResponse(msg);

            // 🔑 Diffuser au destinataire (ou aux membres de l'équipe) et à l'expéditeur
            broadcastUpdate(msg, "/queue/message-edited", response);

            System.out.println(" Message " + messageId + " modifié et diffusé");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }

    // SUPPRIMER un message
    @DeleteMapping("/{messageId}")
    public ResponseEntity<Void> deleteMessage(
            @PathVariable Long messageId,
            Authentication authentication) {
        try {
            String username = authentication.getName();
            MessageEntity msg = messageRepository.findById(messageId)
                    .orElseThrow(() -> new RuntimeException("Message non trouvé"));

            if (!msg.getSender().getUsername().equals(username)) {
                return ResponseEntity.status(403).build();
            }

            // 🔑 Soft delete (comme WhatsApp) au lieu de vraie suppression
            msg.setIsDeleted(true);
            msg.setContent("");
            messageRepository.save(msg);

            MessageResponse response = convertToResponse(msg);

            broadcastUpdate(msg, "/queue/message-deleted", response);

            System.out.println("🗑️ Message " + messageId + " supprimé (soft) et diffusé");
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }
}