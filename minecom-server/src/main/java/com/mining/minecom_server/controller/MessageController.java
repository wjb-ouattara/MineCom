package com.mining.minecom_server.controller;

import com.mining.minecom_server.common.dto.MessageRequest;
import com.mining.minecom_server.common.dto.MessageResponse;
import com.mining.minecom_server.model.MessageEntity;
import com.mining.minecom_server.service.MessageService;
import java.security.Principal;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/messages")
public class MessageController {

    private final MessageService messageService;
    private final SimpMessagingTemplate messagingTemplate;

    public MessageController(MessageService messageService, SimpMessagingTemplate messagingTemplate) {
        this.messageService = messageService;
        this.messagingTemplate = messagingTemplate;
    }

    @MessageMapping("/private-message")
    public void handlePrivateMessage(MessageRequest request, Principal principal) {
        String senderUsername = principal.getName();

        System.out.println("📨 Message WebSocket reçu de " + senderUsername + " pour receiverId=" + request.getReceiverId());

        // 1. Sauvegarder le message dans la DB
        MessageEntity savedMessage = messageService.sendMessage(senderUsername, request);

        // 2. Créer la réponse - Conversion MessageEntity vers MessageResponse
        MessageResponse response = convertToResponse(savedMessage);

        // 3. Envoyer au destinataire via WebSocket
        messagingTemplate.convertAndSendToUser(
                String.valueOf(request.getReceiverId()),
                "/queue/messages",
                response
        );

        System.out.println("✅ Message sauvegardé (ID: " + savedMessage.getId() + ") et envoyé au destinataire");
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
     * Convertit une MessageEntity en MessageResponse
     */
    private MessageResponse convertToResponse(MessageEntity entity) {
        MessageResponse response = new MessageResponse();
        response.setMessageId(entity.getId());
        response.setSenderId(entity.getSender().getId());
        response.setReceiverId(entity.getReceiver().getId());
        response.setContent(entity.getContent());
        response.setTimestamp(entity.getTimestamp());
        response.setMessageType(entity.getMessageType());
        response.setStatus(entity.getStatus());
        return response;
    }
}