package com.mining.minecom_server.repository;

import com.mining.minecom_server.model.SOSAcknowledgment;
import com.mining.minecom_server.model.SOSAlert;
import com.mining.minecom_server.model.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SOSAcknowledgmentRepository extends JpaRepository<SOSAcknowledgment, Long> {
    Optional<SOSAcknowledgment> findByAlertAndUser(SOSAlert alert, UserEntity user);
    Long countByAlert(SOSAlert alert);
}