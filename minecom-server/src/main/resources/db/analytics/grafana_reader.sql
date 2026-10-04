-- =====================================================================
-- MineCom : utilisateur PostgreSQL en lecture seule pour Grafana.
-- À exécuter par le propriétaire des tables (DB_USERNAME), base "minecom".
-- Rejouable sans erreur.
--
-- Le mot de passe n'est pas versionné : il vient de GRAFANA_DB_PASSWORD (.env.grafana)
-- et se passe à psql avec -v grafana_password="$GRAFANA_DB_PASSWORD".
-- =====================================================================

-- Rend le mot de passe accessible au bloc DO (psql n'interpole pas dans $$...$$)
SELECT set_config('minecom.grafana_password', :'grafana_password', false);

-- Création du rôle uniquement s'il n'existe pas déjà
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'grafana_reader') THEN
        EXECUTE format('CREATE USER grafana_reader WITH PASSWORD %L',
                       current_setting('minecom.grafana_password'));
    END IF;
END
$$;

-- Accès en lecture aux tables et vues existantes
GRANT CONNECT ON DATABASE minecom TO grafana_reader;
GRANT USAGE ON SCHEMA public TO grafana_reader;
GRANT SELECT ON ALL TABLES IN SCHEMA public TO grafana_reader;

-- Pas d'accès direct aux tables utilisateurs (hachages de mots de passe).
-- Les vues restent lisibles : elles s'exécutent avec les droits de leur propriétaire.
REVOKE SELECT ON app_user, app_users FROM grafana_reader;

-- Lecture automatique des futures tables/vues créées par le rôle courant
-- (celui qui exécute ce script, donc celui utilisé par Hibernate)
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT SELECT ON TABLES TO grafana_reader;
