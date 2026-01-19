package com.mining.minecom_server.controller;

import com.mining.minecom_server.common.dto.UserDto;
import com.mining.minecom_server.common.dto.ConversationPreviewDto;
import com.mining.minecom_server.common.dto.LastMessageDto;
import com.mining.minecom_server.model.UserEntity;
import com.mining.minecom_server.model.MessageEntity;
import com.mining.minecom_server.repository.UserRepository;
import com.mining.minecom_server.repository.MessageRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserRepository userRepository;
    private final MessageRepository messageRepository;

    public UserController(UserRepository userRepository, MessageRepository messageRepository) {
        this.userRepository = userRepository;
        this.messageRepository = messageRepository;
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserDto> getUserById(@PathVariable Long id) {
        Optional<UserEntity> userOptional = userRepository.findById(id);

        if (userOptional.isEmpty()) {
            System.err.println("❌ User non trouvé pour ID: " + id);
            return ResponseEntity.notFound().build();
        }

        UserEntity user = userOptional.get();
        UserDto userDto = convertToDto(user);

        System.out.println("✅ User trouvé : " + userDto.getUsername() + " (ID: " + id + ")");
        return ResponseEntity.ok(userDto);
    }

    @GetMapping("/conversations")
    public ResponseEntity<List<ConversationPreviewDto>> getConversations(Authentication authentication) {
        String currentUsername = authentication.getName();
        UserEntity currentUser = userRepository.findByUsername(currentUsername)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        List<UserEntity> allUsers = userRepository.findAll();
        List<ConversationPreviewDto> conversations = new ArrayList<>();

        for (UserEntity user : allUsers) {
            // Ne pas inclure l'utilisateur connecté
            if (user.getId().equals(currentUser.getId())) {
                continue;
            }

            // Récupérer le dernier message
            List<MessageEntity> messages = messageRepository.findLastMessageBetween(currentUser.getId(), user.getId());

            LastMessageDto lastMessageDto = null;
            if (!messages.isEmpty()) {
                MessageEntity lastMessage = messages.get(0);
                boolean isSentByMe = lastMessage.getSender().getId().equals(currentUser.getId());

                lastMessageDto = new LastMessageDto(
                        lastMessage.getContent(),
                        lastMessage.getTimestamp(),
                        isSentByMe
                );
            }

            ConversationPreviewDto preview = new ConversationPreviewDto(
                    user.getId(),
                    user.getUsername(),
                    user.getIsOnline(),
                    lastMessageDto
            );

            conversations.add(preview);
        }

        return ResponseEntity.ok(conversations);
    }

    /**
     * Récupère la liste de tous les utilisateurs (contacts) avec leur statut en ligne.
     */
    @GetMapping("/contacts")
    public ResponseEntity<List<UserDto>> getAllContacts() {
        // 1. Récupérer TOUS les utilisateurs de la DB
        List<UserEntity> users = userRepository.findAll();

        // 2. Mapper les entités UserEntity vers les DTOs UserDto (qui incluent isOnline)
        List<UserDto> contactList = users.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());

        return ResponseEntity.ok(contactList);
    }

    // Méthode de mapping
    private UserDto convertToDto(UserEntity user) {
        return new UserDto(
                user.getId(),
                user.getUsername(),
                user.getIsOnline() != null ? user.getIsOnline() : false
        );
    }

    @GetMapping("/by-username/{username}")
    public ResponseEntity<UserDto> getUserByUsername(@PathVariable String username) {
        Optional<UserEntity> userOptional = userRepository.findByUsername(username);

        if (userOptional.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        UserEntity user = userOptional.get();
        UserDto userDto = convertToDto(user);

        return ResponseEntity.ok(userDto);
    }
}