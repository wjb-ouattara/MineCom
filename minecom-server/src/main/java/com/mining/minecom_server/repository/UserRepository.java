package com.mining.minecom_server.repository;

import com.mining.minecom_server.model.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<UserEntity, Long> {

    Optional<UserEntity> findByUsername(String username);


    /**
     * Met à jour le statut hors ligne pour les utilisateurs inactifs
     * CORRECTION: User -> UserEntity
     */
    @Modifying
    @Transactional
    @Query("UPDATE UserEntity u SET u.isOnline = false " +
            "WHERE u.isOnline = true AND u.lastActivity < :threshold")
    int setInactiveUsersOffline(@Param("threshold") Instant threshold);
}