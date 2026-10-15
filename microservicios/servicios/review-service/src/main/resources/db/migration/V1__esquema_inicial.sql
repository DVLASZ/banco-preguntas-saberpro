-- review_db: casos de revisión, rondas, asignaciones, evaluaciones y eventos procesados.
-- Las asignaciones y las evaluaciones solo se agregan, nunca se modifican ni se borran: son el historial
-- de la revisión (quién y cuándo), y una evaluación enviada es definitiva.

CREATE TABLE review_case (
    id           VARCHAR(36)  PRIMARY KEY,
    question_id  VARCHAR(36)  NOT NULL UNIQUE,
    author_id    VARCHAR(100) NOT NULL,
    title        VARCHAR(200) NOT NULL,
    competency   VARCHAR(40)  NOT NULL,
    created_at   TIMESTAMPTZ  NOT NULL,
    updated_at   TIMESTAMPTZ  NOT NULL
);

CREATE TABLE review_round (
    id                VARCHAR(36)  PRIMARY KEY,
    case_id           VARCHAR(36)  NOT NULL REFERENCES review_case (id),
    number            INTEGER      NOT NULL CHECK (number >= 1),
    question_version  INTEGER      NOT NULL CHECK (question_version >= 1),
    state             VARCHAR(30)  NOT NULL,
    started_at        TIMESTAMPTZ  NOT NULL,
    decided_at        TIMESTAMPTZ,
    closed_at         TIMESTAMPTZ,
    CONSTRAINT uq_round_number UNIQUE (case_id, number),
    CONSTRAINT uq_round_version UNIQUE (case_id, question_version)
);

CREATE INDEX idx_review_round_state ON review_round (state, started_at);

CREATE TABLE round_assignment (
    id           BIGSERIAL    PRIMARY KEY,
    round_id     VARCHAR(36)  NOT NULL REFERENCES review_round (id),
    reviewer_id  VARCHAR(100) NOT NULL,
    assigned_by  VARCHAR(100) NOT NULL,
    assigned_at  TIMESTAMPTZ  NOT NULL,
    automatic    BOOLEAN      NOT NULL,
    CONSTRAINT uq_assignment UNIQUE (round_id, reviewer_id)
);

CREATE INDEX idx_assignment_reviewer ON round_assignment (reviewer_id, assigned_at DESC);

CREATE TABLE evaluation (
    id            BIGSERIAL    PRIMARY KEY,
    round_id      VARCHAR(36)  NOT NULL REFERENCES review_round (id),
    reviewer_id   VARCHAR(100) NOT NULL,
    decision      VARCHAR(10)  NOT NULL CHECK (decision IN ('APROBAR', 'RECHAZAR')),
    observation   TEXT,
    submitted_at  TIMESTAMPTZ  NOT NULL,
    late          BOOLEAN      NOT NULL,
    CONSTRAINT uq_evaluation UNIQUE (round_id, reviewer_id)
);

-- Idempotencia de los consumidores de eventos.
CREATE TABLE processed_event (
    event_id      VARCHAR(64)  PRIMARY KEY,
    processed_at  TIMESTAMPTZ  NOT NULL
);
