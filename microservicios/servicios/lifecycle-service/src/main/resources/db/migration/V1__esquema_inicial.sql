-- lifecycle_db: estado de cada pregunta, historial de cambios, eventos procesados y bandeja de salida.
-- Sin borrado físico (RNF-16): ninguna operación del servicio elimina filas de estas tablas.

CREATE TABLE question_lifecycle (
    question_id       VARCHAR(36)  PRIMARY KEY,
    author_id         VARCHAR(100) NOT NULL,
    estado            VARCHAR(30)  NOT NULL,
    question_version  INTEGER      NOT NULL CHECK (question_version >= 1),
    sequence          INTEGER      NOT NULL CHECK (sequence >= 1),
    updated_at        TIMESTAMPTZ  NOT NULL
);

CREATE TABLE state_history (
    id            BIGSERIAL    PRIMARY KEY,
    question_id   VARCHAR(36)  NOT NULL REFERENCES question_lifecycle (question_id),
    desde         VARCHAR(30),
    hacia         VARCHAR(30)  NOT NULL,
    cambiado_por  VARCHAR(100),
    fecha         TIMESTAMPTZ  NOT NULL
);

CREATE INDEX idx_state_history_question ON state_history (question_id, fecha, id);

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
