-- simulation_db: catálogo de preguntas publicadas, simulacros y su calificación.

-- Copia de las preguntas publicadas, aprendida de question.published. Incluye la respuesta correcta, por eso
-- este servicio nunca la devuelve mientras el simulacro está en curso.
CREATE TABLE catalog_question (
    question_id       VARCHAR(36)  PRIMARY KEY,
    question_version  INTEGER      NOT NULL CHECK (question_version >= 1),
    title             VARCHAR(200),
    competency        VARCHAR(40)  NOT NULL,
    topic             VARCHAR(200),
    subtopic          VARCHAR(200),
    difficulty        VARCHAR(20)  NOT NULL,
    context           TEXT,
    statement         TEXT         NOT NULL,
    option_a          VARCHAR(500) NOT NULL,
    option_b          VARCHAR(500) NOT NULL,
    option_c          VARCHAR(500) NOT NULL,
    option_d          VARCHAR(500) NOT NULL,
    correct_answer    VARCHAR(1)   NOT NULL CHECK (correct_answer IN ('A', 'B', 'C', 'D')),
    published_at      TIMESTAMPTZ,
    available         BOOLEAN      NOT NULL
);

CREATE INDEX idx_catalog_available ON catalog_question (available);

CREATE TABLE simulation (
    id                  VARCHAR(36)  PRIMARY KEY,
    student_id          VARCHAR(100) NOT NULL,
    status              VARCHAR(20)  NOT NULL,
    started_at          TIMESTAMPTZ  NOT NULL,
    due_at              TIMESTAMPTZ  NOT NULL,
    finished_at         TIMESTAMPTZ,
    duration_minutes    INTEGER      NOT NULL,
    requested_count     INTEGER      NOT NULL,
    competencies        VARCHAR(500) NOT NULL DEFAULT '|',
    topics              VARCHAR(1000) NOT NULL DEFAULT '|',
    difficulties        VARCHAR(200) NOT NULL DEFAULT '|',
    total               INTEGER,
    correct             INTEGER,
    incorrect           INTEGER,
    unanswered          INTEGER,
    score               NUMERIC(5, 2),
    by_competency       TEXT
);

CREATE INDEX idx_simulation_student ON simulation (student_id, started_at DESC);
CREATE INDEX idx_simulation_due ON simulation (due_at) WHERE status = 'EN_CURSO';

-- Las preguntas del simulacro, tal como se le mostraron al estudiante (copia, no referencia al catálogo).
CREATE TABLE simulation_item (
    id                BIGSERIAL    PRIMARY KEY,
    simulation_id     VARCHAR(36)  NOT NULL REFERENCES simulation (id),
    position          INTEGER      NOT NULL,
    question_id       VARCHAR(36)  NOT NULL,
    question_version  INTEGER      NOT NULL,
    title             VARCHAR(200),
    competency        VARCHAR(40)  NOT NULL,
    topic             VARCHAR(200),
    subtopic          VARCHAR(200),
    difficulty        VARCHAR(20)  NOT NULL,
    context           TEXT,
    statement         TEXT         NOT NULL,
    option_a          VARCHAR(500) NOT NULL,
    option_b          VARCHAR(500) NOT NULL,
    option_c          VARCHAR(500) NOT NULL,
    option_d          VARCHAR(500) NOT NULL,
    correct_answer    VARCHAR(1)   NOT NULL,
    answer            VARCHAR(1),
    CONSTRAINT uq_item_position UNIQUE (simulation_id, position),
    CONSTRAINT uq_item_question UNIQUE (simulation_id, question_id)
);

-- Idempotencia de los consumidores de eventos.
CREATE TABLE processed_event (
    event_id      VARCHAR(64)  PRIMARY KEY,
    processed_at  TIMESTAMPTZ  NOT NULL
);

-- Bandeja de salida (patrón Outbox): simulation.completed se guarda con la calificación y un relevo lo publica.
CREATE TABLE outbox_event (
    seq         BIGSERIAL    PRIMARY KEY,
    event_id    VARCHAR(36)  NOT NULL UNIQUE,
    routing_key VARCHAR(100) NOT NULL,
    payload     TEXT         NOT NULL,
    created_at  TIMESTAMPTZ  NOT NULL,
    sent_at     TIMESTAMPTZ
);

CREATE INDEX idx_outbox_pendientes ON outbox_event (seq) WHERE sent_at IS NULL;
CREATE INDEX idx_outbox_enviados ON outbox_event (sent_at) WHERE sent_at IS NOT NULL;
