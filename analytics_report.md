# Rapport : vues analytiques MineCom pour Grafana

## 1. Fichiers créés

| Fichier | Rôle |
|---|---|
| `minecom-server/src/main/resources/db/analytics/analytics_views.sql` | Les 4 vues analytiques (`CREATE OR REPLACE VIEW`, peut être relancé) |
| `minecom-server/src/main/resources/db/analytics/grafana_reader.sql` | Utilisateur PostgreSQL en lecture seule `grafana_reader` |
| `analytics_report.md` | Ce rapport |

Aucun code Java, aucune entité, aucune table et aucune donnée existante n'a été modifié.

## 2. Gestion du schéma

- Hibernate `spring.jpa.hibernate.ddl-auto=update`, **ni Flyway ni Liquibase**.
- Les scripts sont donc dans `db/analytics/` et **s'exécutent à la main** (Hibernate ne les lance pas).
- `PhysicalNamingStrategyStandardImpl` : les colonnes sans `@Column(name=…)` gardent le nom Java en minuscules (ex. `SOSAlert.isActive` → `sos_alerts.isactive`).

## 3. Tables utilisées (noms réels, vérifiés via `information_schema`)

| Table | Colonnes utiles |
|---|---|
| `message` | `id`, `sender_id`, `receiver_id`, `team_id`, `timestamp` (timestamptz), `message_type`, `status`, `is_deleted`, `is_edited` |
| `app_user` | `id`, `username`, `is_online`, `last_activity`, `last_seen` |
| `sos_alerts` | `id`, `sender_id`, `timestamp`, `location`, `description`, `isactive` |
| `sos_acknowledgments` | `id`, `alert_id`, `user_id`, `timestamp` |

`MessageType` (identique dans `minecom-common`, packages `com.mining.minecom` et `com.mining.minecom_server`) :
`TEXT, INCIDENT, URGENT, ALERTE, MAINTENANCE, SECURITE, IMAGE, FILE, DEMANDE_DE_SUPPORT`.

## 4. Vues et colonnes

### `v_messages_par_type_heure`
Nombre de messages par heure et par type, avec leur catégorie métier ou format.

| Colonne | Type | Description |
|---|---|---|
| `time` | timestamptz | Heure (`date_trunc('hour')`) |
| `type_message` | varchar | Valeur de `MessageType` |
| `nb_messages` | bigint | Nombre de messages non supprimés |
| `categorie` | text | `METIER` ou `FORMAT` |

### `v_activite_utilisateurs`
Activité quotidienne de chaque utilisateur.

| Colonne | Type | Description |
|---|---|---|
| `time` | timestamptz | Jour |
| `user_id` | bigint | Id de l'expéditeur |
| `username` | varchar | Nom de l'utilisateur |
| `nb_messages` | bigint | Messages envoyés |
| `nb_incidents` | bigint | Messages `INCIDENT` |
| `nb_urgents` | bigint | Messages `URGENT` |
| `nb_incidents_urgents` | bigint | `INCIDENT` + `URGENT` |

### `v_temps_reponse_sos`
Une ligne par alerte SOS. Le `LEFT JOIN` garde aussi les SOS non acquittés.

| Colonne | Type | Description |
|---|---|---|
| `sos_id` | bigint | Id de l'alerte |
| `time` | timestamptz | Déclenchement |
| `emetteur_id` | bigint | Id de l'émetteur |
| `emetteur` | varchar | Nom de l'émetteur |
| `localisation` | varchar | `sos_alerts.location` |
| `premier_acquittement` | timestamptz | Premier acquittement (NULL si aucun) |
| `delai_secondes` | numeric(12,1) | Délai jusqu'au premier acquittement |
| `nb_acquittements` | bigint | Nombre total d'acquittements |
| `statut` | text | `ACQUITTÉ` / `EN ATTENTE` |

### `v_kpi_jour`
Indicateurs clés par jour.

| Colonne | Type | Description |
|---|---|---|
| `time` | timestamptz | Jour |
| `total_messages` | bigint | Messages non supprimés |
| `total_incident` | bigint | Messages `INCIDENT` |
| `total_urgent` | bigint | Messages `URGENT` |
| `total_alerte` | bigint | Messages `ALERTE` |
| `nb_sos` | bigint | SOS déclenchés ce jour |
| `nb_sos_non_acquittes` | bigint | Parmi eux, ceux sans aucun acquittement |
| `nb_utilisateurs_actifs` | bigint | Utilisateurs distincts ayant envoyé un message ou un SOS |
| `delai_moyen_acquittement_s` | numeric(12,1) | Délai moyen d'acquittement des SOS du jour (NULL si aucun) |
| `total_metier` | bigint | Messages de catégorie `METIER` |
| `total_format` | bigint | Messages de catégorie `FORMAT` |

## 5. Hypothèses

1. **Message supprimé** : `message.is_deleted = true` (soft delete). 83 lignes anciennes ont `is_deleted = NULL`. Elles comptent comme **non supprimées** (`COALESCE(is_deleted, false)`).
2. **Catégorie** : `TEXT`, `IMAGE`, `FILE` = `FORMAT`. Tous les autres types = `METIER` (`INCIDENT`, `URGENT`, `ALERTE`, `MAINTENANCE`, `SECURITE`, `DEMANDE_DE_SUPPORT`). Un nouveau type ajouté plus tard à l'enum sera classé `METIER` par défaut.
3. **`categorie` dans `v_kpi_jour`** : une colonne `categorie` aurait créé deux lignes par jour. Les SOS et les utilisateurs actifs auraient alors été comptés deux fois. Cette vue a donc à la place deux compteurs, `total_metier` et `total_format`.
4. **Ordre des colonnes** : les nouvelles colonnes sont **en fin de vue**. PostgreSQL refuse `CREATE OR REPLACE VIEW` si on insère une colonne au milieu.
5. **Jour** : `date_trunc('day')` dans le fuseau de la session PostgreSQL (serveur : `Africa/Casablanca`).
6. **Utilisateurs** : la table utilisée est `app_user`, la seule référencée par les clés étrangères. La table `app_users` (1 ligne) n'est pas mappée par Hibernate, c'est probablement un reste d'une ancienne version. Elle est ignorée.
7. **Acquittements** : tous comptent, y compris un éventuel acquittement par l'émetteur lui-même.

## 6. Utilisateur Grafana

- `grafana_reader`, créé seulement s'il n'existe pas (bloc `DO`).
- Mot de passe **non versionné** : variable `GRAFANA_DB_PASSWORD` dans `.env` (voir `.env.example`), passée à psql avec `-v grafana_password=…`. Il ne sert qu'à la création du rôle, donc le changer après coup demande `ALTER USER grafana_reader WITH PASSWORD '…';`.
- Droits : `CONNECT` sur `minecom`, `USAGE` sur `public`, `SELECT` sur toutes les tables et vues.
- `ALTER DEFAULT PRIVILEGES` : il pourra aussi lire les futures tables et vues créées par le propriétaire (`minecom_user`, le compte utilisé par Hibernate).
- `REVOKE SELECT ON app_user, app_users` : pas d'accès direct aux hachages de mots de passe. Les vues (qui affichent `username`) restent lisibles, car elles s'exécutent avec les droits de leur propriétaire.
- Source de données Grafana : hôte `host.docker.internal:5433`, base `minecom`, utilisateur `grafana_reader`, SSL désactivé.

## 7. Exécution et tests (2026-10-03)

```bash
set -a; . ./.env; set +a
PGPASSWORD="$DB_PASSWORD" psql -h localhost -p 5433 -U "$DB_USERNAME" -d minecom -v ON_ERROR_STOP=1 \
  -v grafana_password="$GRAFANA_DB_PASSWORD" \
  -f minecom-server/src/main/resources/db/analytics/analytics_views.sql \
  -f minecom-server/src/main/resources/db/analytics/grafana_reader.sql
```

Résultat : `CREATE VIEW` ×4, `DO`, `GRANT` ×3, `ALTER DEFAULT PRIVILEGES`, sans erreur.

Lecture avec `grafana_reader` (`SELECT * FROM <vue> LIMIT 5`) : **les 4 vues renvoient 5 lignes chacune.** Extraits :

```
v_messages_par_type_heure
 2026-07-01 16:00:00+01 | TEXT   | 1 | FORMAT
 2026-01-23 11:00:00+01 | ALERTE | 1 | METIER
 2026-10-02 17:00:00+01 | URGENT | 4 | METIER

v_temps_reponse_sos
 1 | 2026-06-03 01:54 | Innocent | Zone A      |                  |       | 0 | EN ATTENTE
 3 | 2026-06-05 23:32 | Ouattara | Zone D      | 2026-06-05 23:35 | 186.9 | 2 | ACQUITTÉ
 5 | 2026-06-06 13:34 | Innocent | Galerie Sud | 2026-06-06 13:36 | 141.6 | 8 | ACQUITTÉ

v_kpi_jour
 2026-01-07 | total 23 | incident 1 | urgent 1 | alerte 1 | sos 0 | actifs 2 | metier 6 | format 17
```

Contrôles de cohérence :

| Contrôle | Résultat |
|---|---|
| Total des messages dans les vues | 108 = 110 en base − 2 supprimés ✅ |
| `METIER` + `FORMAT` = total | 39 + 69 = 108 ✅ |
| SOS non acquittés conservés | SOS 1 et 2 → `EN ATTENTE` ✅ |
| `grafana_reader` : `CREATE TABLE` | refusé (`droit refusé pour le schéma public`) ✅ |
| `grafana_reader` : `DELETE FROM message` | refusé (`droit refusé pour la table message`) ✅ |
