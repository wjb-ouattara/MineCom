package com.mining.minecom_server.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Les messages d'équipe n'ont pas de destinataire individuel : receiver_id doit accepter NULL.
 * ddl-auto=update ne modifie pas les contraintes des colonnes existantes, d'où cette
 * migration (idempotente) au démarrage.
 */
@Component
public class TeamSchemaMigration implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;

    public TeamSchemaMigration(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            jdbcTemplate.execute("ALTER TABLE message ALTER COLUMN receiver_id DROP NOT NULL");
        } catch (Exception e) {
            System.err.println("⚠️ Migration receiver_id ignorée : " + e.getMessage());
        }
    }
}
