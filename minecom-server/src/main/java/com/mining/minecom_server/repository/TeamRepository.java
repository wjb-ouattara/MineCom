package com.mining.minecom_server.repository;

import com.mining.minecom_server.model.TeamEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TeamRepository extends JpaRepository<TeamEntity, Long> {

    /** Équipes dont l'utilisateur est membre. */
    @Query("SELECT DISTINCT t FROM TeamEntity t JOIN t.members m WHERE m.user.id = :userId")
    List<TeamEntity> findByMemberId(@Param("userId") Long userId);
}
