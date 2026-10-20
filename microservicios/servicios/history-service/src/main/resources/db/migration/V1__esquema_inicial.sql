-- history_db: historial de las preguntas y auditoría del sistema.
-- Las entradas solo se agregan: nunca se modifican ni se borran.

CREATE TABLE history_entry (
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

CREATE INDEX idx_history_question ON history_entry (question_id, occurred_at);
CREATE INDEX idx_history_type ON history_entry (type, occurred_at DESC);
CREATE INDEX idx_history_actor ON history_entry (actor_id, occurred_at DESC);
CREATE INDEX idx_history_occurred ON history_entry (occurred_at DESC);

-- Quién es el autor de cada pregunta, aprendido de question.submitted.
CREATE TABLE question_info (
    question_id  VARCHAR(36)  PRIMARY KEY,
    title        VARCHAR(200),
    author_id    VARCHAR(100) NOT NULL
);

CREATE INDEX idx_question_info_author ON question_info (author_id);

-- Quiénes han revisado cada pregunta, aprendido de review.reviewers-assigned.
CREATE TABLE question_reviewer (
    id           BIGSERIAL    PRIMARY KEY,
    question_id  VARCHAR(36)  NOT NULL,
    reviewer_id  VARCHAR(100) NOT NULL,
    CONSTRAINT uq_question_reviewer UNIQUE (question_id, reviewer_id)
);

CREATE INDEX idx_question_reviewer_user ON question_reviewer (reviewer_id);

-- Idempotencia de los consumidores de eventos.
CREATE TABLE processed_event (
    event_id      VARCHAR(64)  PRIMARY KEY,
    processed_at  TIMESTAMPTZ  NOT NULL
);
