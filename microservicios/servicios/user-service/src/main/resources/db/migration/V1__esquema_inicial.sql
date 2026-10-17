-- user_db: copia del directorio de usuarios de Keycloak y cursor de los inicios de sesión ya publicados.

CREATE TABLE user_profile (
    username       VARCHAR(100) PRIMARY KEY,
    keycloak_id    VARCHAR(64),
    full_name      VARCHAR(200) NOT NULL,
    email          VARCHAR(200),
    -- Roles entre barras para poder buscar uno exacto: |ADMIN|REVIEWER|
    roles          VARCHAR(200) NOT NULL DEFAULT '|',
    active         BOOLEAN      NOT NULL,
    last_login_at  TIMESTAMPTZ,
    synced_at      TIMESTAMPTZ  NOT NULL
);

CREATE INDEX idx_user_profile_roles ON user_profile (roles);
CREATE INDEX idx_user_profile_active ON user_profile (active);

-- Una sola fila: hasta qué momento se publicaron los inicios de sesión de Keycloak. Sirve también de cerrojo
-- entre réplicas (SELECT ... FOR UPDATE SKIP LOCKED). Arranca en el momento de la instalación para no reenviar
-- eventos antiguos.
CREATE TABLE session_cursor (
    id             INTEGER      PRIMARY KEY CHECK (id = 1),
    last_event_at  TIMESTAMPTZ  NOT NULL
);

INSERT INTO session_cursor (id, last_event_at) VALUES (1, now());
