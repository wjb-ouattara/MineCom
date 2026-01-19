package com.mining.minecom_server.repository;

import com.mining.minecom_server.model.SOSAlert;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SOSAlertRepository extends JpaRepository<SOSAlert, Long> {
    List<SOSAlert> findByIsActiveTrueOrderByTimestampDesc();
    Optional<SOSAlert> findTopByIsActiveTrueOrderByTimestampDesc();
}