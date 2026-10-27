-- assignment_db: casos de revisión, rondas, asignaciones de revisores, eventos procesados y bandeja de salida.
-- Las asignaciones solo se agregan, nunca se modifican ni se borran (salvo la decisión del revisor, que se anota cuando
-- evaluation-service informa su evaluación): son el historial de quién revisó y cuándo.

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
    decision     VARCHAR(10)  CHECK (decision IN ('APROBAR', 'RECHAZAR')),
    CONSTRAINT uq_assignment UNIQUE (round_id, reviewer_id)
);

CREATE INDEX idx_assignment_reviewer ON round_assignment (reviewer_id, assigned_at DESC);

-- Idempotencia de los consumidores de eventos.
CREATE TABLE processed_event (
    event_id      VARCHAR(64)  PRIMARY KEY,
    processed_at  TIMESTAMPTZ  NOT NULL
);

-- Patrón Outbox: los eventos se guardan aquí en la misma transacción que el cambio de negocio y un relevo los
-- publica en el broker; así no se pierden si RabbitMQ está caído.
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
