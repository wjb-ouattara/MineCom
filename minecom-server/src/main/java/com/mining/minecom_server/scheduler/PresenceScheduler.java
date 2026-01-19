// Dans com.mining.minecom_server.scheduler/PresenceScheduler.java

package com.mining.minecom_server.scheduler;

import com.mining.minecom_server.repository.UserRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Component
public class PresenceScheduler {

    private final UserRepository userRepository;

    // Définissez la période d'inactivité maximale. 
    // Ici, 5 minutes.
    private static final int INACTIVITY_THRESHOLD_MINUTES = 5;

    public PresenceScheduler(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Scheduled(fixedRate = 60000) // 60000 ms = 1 minute
    @Transactional
    public void checkInactiveUsersOptimized() {
        Instant threshold = Instant.now().minus(INACTIVITY_THRESHOLD_MINUTES, ChronoUnit.MINUTES);

        int count = userRepository.setInactiveUsersOffline(threshold);

        if (count > 0) {
            System.out.println("SCHEDULER OPTIMISÉ: " + count + " utilisateur(s) mis hors ligne pour inactivité.");
        }
    }
}