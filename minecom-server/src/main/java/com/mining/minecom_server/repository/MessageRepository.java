package com.mining.minecom_server.repository;

import com.mining.minecom_server.model.MessageEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface MessageRepository extends JpaRepository<MessageEntity, Long> {

    /**
     * Récupère l'historique des messages entre deux utilisateurs.
     * La requête utilise OR pour couvrir les messages envoyés dans les deux sens.
     */
    @Query("SELECT m FROM MessageEntity m WHERE " +
            "(m.sender.id = :user1Id AND m.receiver.id = :user2Id) OR " +
            "(m.sender.id = :user2Id AND m.receiver.id = :user1Id) " +
            "ORDER BY m.timestamp ASC")
    List<MessageEntity> findConversation(@Param("user1Id") Long user1Id, @Param("user2Id") Long user2Id);

    // 🔑 NOUVELLE MÉTHODE : Trouver le dernier message entre deux utilisateurs
    @Query("SELECT m FROM MessageEntity m WHERE " +
            "(m.sender.id = :userId1 AND m.receiver.id = :userId2) OR " +
            "(m.sender.id = :userId2 AND m.receiver.id = :userId1) " +
            "ORDER BY m.timestamp DESC")
    List<MessageEntity> findLastMessageBetween(@Param("userId1") Long userId1, @Param("userId2") Long userId2);
}