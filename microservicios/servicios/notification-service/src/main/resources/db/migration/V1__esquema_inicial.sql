-- notification_db: bandeja de notificaciones, correos simulados y datos mínimos de las preguntas.

CREATE TABLE notification (
    id                VARCHAR(36)  PRIMARY KEY,
    recipient_id      VARCHAR(100) NOT NULL,
    recipient_email   VARCHAR(200),
    type              VARCHAR(40)  NOT NULL,
    question_id       VARCHAR(36),
    subject           VARCHAR(300) NOT NULL,
    message           TEXT         NOT NULL,
    created_at        TIMESTAMPTZ  NOT NULL,
    read_at           TIMESTAMPTZ,
    email_sent_at     TIMESTAMPTZ
);

CREATE INDEX idx_notification_recipient ON notification (recipient_id, created_at DESC);
CREATE INDEX idx_notification_unread ON notification (recipient_id) WHERE read_at IS NULL;
CREATE INDEX idx_notification_email ON notification (email_sent_at DESC) WHERE email_sent_at IS NOT NULL;

-- Lo que este servicio sabe de cada pregunta, aprendido de question.submitted.
CREATE TABLE question_info (
    question_id  VARCHAR(36)  PRIMARY KEY,
    title        VARCHAR(200),
    author_id    VARCHAR(100) NOT NULL
);

-- Idempotencia de los consumidores de eventos.
CREATE TABLE processed_event (
    event_id      VARCHAR(64)  PRIMARY KEY,
    processed_at  TIMESTAMPTZ  NOT NULL
);
