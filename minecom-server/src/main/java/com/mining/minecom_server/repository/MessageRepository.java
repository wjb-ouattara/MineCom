package com.mining.minecom_server.repository;

import com.mining.minecom_server.model.MessageEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

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

    /** Historique d'une équipe. */
    @Query("SELECT m FROM MessageEntity m WHERE m.team.id = :teamId ORDER BY m.timestamp ASC")
    List<MessageEntity> findByTeamId(@Param("teamId") Long teamId);

    /** Messages d'une équipe du plus récent au plus ancien (aperçu du dernier message). */
    @Query("SELECT m FROM MessageEntity m WHERE m.team.id = :teamId ORDER BY m.timestamp DESC")
    List<MessageEntity> findLatestByTeamId(@Param("teamId") Long teamId,
                                           org.springframework.data.domain.Pageable pageable);

    @Modifying
    @Transactional
    @Query("UPDATE MessageEntity m SET m.status = 'READ' " +
            "WHERE m.sender.id = :senderId " +
            "AND m.receiver.id = :receiverId " +
            "AND m.status <> 'READ'")
    void markMessagesAsRead(@Param("senderId") Long senderId,
                            @Param("receiverId") Long receiverId);
}