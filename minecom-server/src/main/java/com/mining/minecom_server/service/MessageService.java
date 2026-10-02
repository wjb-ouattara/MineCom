package com.mining.minecom_server.service;

import com.mining.minecom_server.common.dto.MessageRequest;
import com.mining.minecom_server.model.MessageEntity;
import com.mining.minecom_server.model.UserEntity;
import com.mining.minecom_server.repository.MessageRepository;
import com.mining.minecom_server.repository.UserRepository;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
public class MessageService {

    private final MessageRepository messageRepository;
    private final UserRepository userRepository;
    private final StorageService storageService;
    private final TeamService teamService;

    public MessageService(MessageRepository messageRepository, UserRepository userRepository,
                          StorageService storageService, TeamService teamService) {
        this.messageRepository = messageRepository;
        this.userRepository = userRepository;
        this.storageService = storageService;
        this.teamService = teamService;
    }

    /**
     * Enregistre un nouveau message dans la base de données.
     * @param senderUsername Le nom de l'utilisateur actuellement connecté (expéditeur).
     * @param request Le DTO contenant l'ID du destinataire et le contenu.
     * @return Le message sauvegardé.
     */
    @Transactional
    public MessageEntity sendMessage(String senderUsername, MessageRequest request) {

        // 1. Trouver l'expéditeur et le destinataire
        UserEntity sender = userRepository.findByUsername(senderUsername)
                .orElseThrow(() -> new UsernameNotFoundException("Expéditeur non trouvé : " + senderUsername));

        // 2. Créer l'entité MessageEntity
        MessageEntity message = new MessageEntity();
        message.setSender(sender);

        if (request.getTeamId() != null) {
            // Message d'équipe : seul un membre peut écrire dans l'équipe
            message.setTeam(teamService.requireMembership(request.getTeamId(), sender.getId()));
        } else {
            if (request.getReceiverId() == null) {
                throw new IllegalArgumentException("Destinataire manquant (receiverId ou teamId)");
            }
            UserEntity receiver = userRepository.findById(request.getReceiverId())
                    .orElseThrow(() -> new UsernameNotFoundException("Destinataire non trouvé avec ID : " + request.getReceiverId()));
            message.setReceiver(receiver);
        }
        message.setContent(request.getContent() != null ? request.getContent() : "");
        message.setMessageType(request.getMessageType());
        message.setTimestamp(Instant.now());

        if (request.getReplyToId() != null) {
            message.setReplyToId(request.getReplyToId());
            message.setReplyToContent(request.getReplyToContent());
            message.setReplyToSenderId(request.getReplyToSenderId());
        }

        // Pièce jointe : on persiste la clé MinIO, pas l'URL présignée (qui expire)
        if (request.getFileUrl() != null && !request.getFileUrl().isBlank()) {
            message.setFileUrl(storageService.toObjectName(request.getFileUrl()));
            message.setFileName(request.getFileName());
            message.setFileType(request.getFileType());
            message.setFileSize(request.getFileSize());
        }

        // 3. Sauvegarder
        return messageRepository.save(message);
    }

    /**
     * Récupère la conversation complète entre l'utilisateur actuel et un partenaire.
     */
    public List<MessageEntity> getConversation(String currentUsername, Long partnerId) {
        UserEntity currentUser = userRepository.findByUsername(currentUsername)
                .orElseThrow(() -> new UsernameNotFoundException("Utilisateur actuel non trouvé : " + currentUsername));

        return messageRepository.findConversation(currentUser.getId(), partnerId);
    }
}