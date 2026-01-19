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

    public MessageService(MessageRepository messageRepository, UserRepository userRepository) {
        this.messageRepository = messageRepository;
        this.userRepository = userRepository;
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

        UserEntity receiver = userRepository.findById(request.getReceiverId())
                .orElseThrow(() -> new UsernameNotFoundException("Destinataire non trouvé avec ID : " + request.getReceiverId()));

        // 2. Créer l'entité MessageEntity
        MessageEntity message = new MessageEntity();
        message.setSender(sender);
        message.setReceiver(receiver);
        message.setContent(request.getContent());
        message.setMessageType(request.getMessageType());
        message.setTimestamp(Instant.now());
        // Le statut par défaut est SENT

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