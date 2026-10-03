-- =====================================================================
-- MineCom : vues analytiques pour Grafana (lecture seule)
-- Schéma géré par Hibernate (ddl-auto=update), sans Flyway/Liquibase :
-- ce script s'exécute manuellement et peut être rejoué (CREATE OR REPLACE).
-- Il ne modifie aucune table ni aucune donnée.
--
-- Hypothèses :
--   * message supprimé = message.is_deleted = true (soft delete) ;
--     is_deleted NULL (anciennes lignes) est traité comme non supprimé.
--   * jour = date_trunc('day') dans le fuseau de la session PostgreSQL.
--   * catégorie : TEXT, IMAGE, FILE = 'FORMAT' (forme du message) ;
--     tout autre type (INCIDENT, URGENT, ALERTE, MAINTENANCE, SECURITE,
--     DEMANDE_DE_SUPPORT, et tout futur type) = 'METIER'.
--   * les nouvelles colonnes sont ajoutées en fin de vue pour rester
--     compatibles avec CREATE OR REPLACE VIEW.
-- =====================================================================


-- Volume de messages par heure et par type, classé métier / format.
CREATE OR REPLACE VIEW v_messages_par_type_heure AS
SELECT
    date_trunc('hour', m."timestamp") AS "time",
    m.message_type                    AS type_message,
    count(*)                          AS nb_messages,
    CASE WHEN m.message_type IN ('TEXT', 'IMAGE', 'FILE') THEN 'FORMAT' ELSE 'METIER' END AS categorie
FROM message m
WHERE NOT COALESCE(m.is_deleted, false)
GROUP BY 1, 2;


-- Activité quotidienne de chaque utilisateur : messages envoyés, dont incidents et urgences.
CREATE OR REPLACE VIEW v_activite_utilisateurs AS
SELECT
    date_trunc('day', m."timestamp")                    AS "time",
    u.id                                                AS user_id,
    u.username                                          AS username,
    count(*)                                            AS nb_messages,
    count(*) FILTER (WHERE m.message_type = 'INCIDENT') AS nb_incidents,
    count(*) FILTER (WHERE m.message_type = 'URGENT')   AS nb_urgents,
    count(*) FILTER (WHERE m.message_type IN ('INCIDENT', 'URGENT')) AS nb_incidents_urgents
FROM message m
JOIN app_user u ON u.id = m.sender_id
WHERE NOT COALESCE(m.is_deleted, false)
GROUP BY 1, 2, 3;


-- Réactivité face aux SOS : délai entre le déclenchement et le premier acquittement.
CREATE OR REPLACE VIEW v_temps_reponse_sos AS
SELECT
    a.id                                   AS sos_id,
    a."timestamp"                          AS "time",
    a.sender_id                            AS emetteur_id,
    u.username                             AS emetteur,
    a.location                             AS localisation,
    ack.premier_acquittement,
    EXTRACT(EPOCH FROM ack.premier_acquittement - a."timestamp")::numeric(12, 1) AS delai_secondes,
    COALESCE(ack.nb_acquittements, 0)      AS nb_acquittements,
    CASE WHEN ack.premier_acquittement IS NULL THEN 'EN ATTENTE' ELSE 'ACQUITTÉ' END AS statut
FROM sos_alerts a
JOIN app_user u ON u.id = a.sender_id
LEFT JOIN (
    SELECT alert_id,
           min("timestamp") AS premier_acquittement,
           count(*)         AS nb_acquittements
    FROM sos_acknowledgments
    GROUP BY alert_id
) ack ON ack.alert_id = a.id;


-- Tableau de bord quotidien : volumes par criticité, SOS et réactivité, utilisateurs actifs.
CREATE OR REPLACE VIEW v_kpi_jour AS
WITH msg AS (
    SELECT
        date_trunc('day', "timestamp")                    AS jour,
        count(*)                                          AS total_messages,
        count(*) FILTER (WHERE message_type = 'INCIDENT') AS total_incident,
        count(*) FILTER (WHERE message_type = 'URGENT')   AS total_urgent,
        count(*) FILTER (WHERE message_type = 'ALERTE')   AS total_alerte,
        count(*) FILTER (WHERE message_type NOT IN ('TEXT', 'IMAGE', 'FILE')) AS total_metier,
        count(*) FILTER (WHERE message_type IN ('TEXT', 'IMAGE', 'FILE'))     AS total_format
    FROM message
    WHERE NOT COALESCE(is_deleted, false)
    GROUP BY 1
),
sos AS (
    SELECT
        date_trunc('day', "time")                         AS jour,
        count(*)                                          AS nb_sos,
        count(*) FILTER (WHERE statut = 'EN ATTENTE')     AS nb_sos_non_acquittes,
        avg(delai_secondes)::numeric(12, 1)               AS delai_moyen_acquittement_s
    FROM v_temps_reponse_sos
    GROUP BY 1
),
actifs AS (
    -- Utilisateur actif = a envoyé au moins un message ou un SOS ce jour-là
    SELECT jour, count(DISTINCT user_id) AS nb_utilisateurs_actifs
    FROM (
        SELECT date_trunc('day', "timestamp") AS jour, sender_id AS user_id
        FROM message WHERE NOT COALESCE(is_deleted, false)
        UNION
        SELECT date_trunc('day', "timestamp"), sender_id FROM sos_alerts
    ) t
    GROUP BY jour
),
jours AS (
    SELECT jour FROM msg UNION SELECT jour FROM sos
)
SELECT
    j.jour                                  AS "time",
    COALESCE(m.total_messages, 0)           AS total_messages,
    COALESCE(m.total_incident, 0)           AS total_incident,
    COALESCE(m.total_urgent, 0)             AS total_urgent,
    COALESCE(m.total_alerte, 0)             AS total_alerte,
    COALESCE(s.nb_sos, 0)                   AS nb_sos,
    COALESCE(s.nb_sos_non_acquittes, 0)     AS nb_sos_non_acquittes,
    COALESCE(a.nb_utilisateurs_actifs, 0)   AS nb_utilisateurs_actifs,
    s.delai_moyen_acquittement_s,
    COALESCE(m.total_metier, 0)             AS total_metier,
    COALESCE(m.total_format, 0)             AS total_format
FROM jours j
LEFT JOIN msg    m ON m.jour = j.jour
LEFT JOIN sos    s ON s.jour = j.jour
LEFT JOIN actifs a ON a.jour = j.jour;
