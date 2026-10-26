-- audit_db: auditoría del sistema (hechos de las preguntas, de su revisión y de la seguridad).
-- Las entradas solo se agregan: nunca se modifican ni se borran.

CREATE TABLE audit_entry (
    id           VARCHAR(36)  PRIMARY KEY,
    event_id     VARCHAR(64)  NOT NULL UNIQUE,
    question_id  VARCHAR(36),
    actor_id     VARCHAR(100),
    type         VARCHAR(40)  NOT NULL,
    summary      VARCHAR(1000) NOT NULL,
    details      TEXT,
    occurred_at  TIMESTAMPTZ  NOT NULL,
    recorded_at  TIMESTAMPTZ  NOT NULL
);

CREATE INDEX idx_audit_type ON audit_entry (type, occurred_at DESC);
CREATE INDEX idx_audit_actor ON audit_entry (actor_id, occurred_at DESC);
CREATE INDEX idx_audit_question ON audit_entry (question_id, occurred_at DESC);
CREATE INDEX idx_audit_occurred ON audit_entry (occurred_at DESC);

-- Idempotencia de los consumidores de eventos.
CREATE TABLE processed_event (
    event_id      VARCHAR(64)  PRIMARY KEY,
    processed_at  TIMESTAMPTZ  NOT NULL
);
