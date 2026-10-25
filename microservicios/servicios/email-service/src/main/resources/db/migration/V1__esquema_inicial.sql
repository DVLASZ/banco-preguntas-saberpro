-- email_db: registro de los correos simulados enviados a partir de las notificaciones.

CREATE TABLE sent_email (
    id              VARCHAR(36)  PRIMARY KEY,
    notification_id VARCHAR(36),
    recipient_id    VARCHAR(100) NOT NULL,
    to_address      VARCHAR(200) NOT NULL,
    subject         VARCHAR(300) NOT NULL,
    body            TEXT         NOT NULL,
    status          VARCHAR(10)  NOT NULL,
    error           VARCHAR(500),
    attempted_at    TIMESTAMPTZ  NOT NULL
);

CREATE INDEX idx_sent_email_attempted ON sent_email (attempted_at DESC);
CREATE INDEX idx_sent_email_status ON sent_email (status, attempted_at DESC);

-- Idempotencia de los consumidores de eventos.
CREATE TABLE processed_event (
    event_id      VARCHAR(64)  PRIMARY KEY,
    processed_at  TIMESTAMPTZ  NOT NULL
);
