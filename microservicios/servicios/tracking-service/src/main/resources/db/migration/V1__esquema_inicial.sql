-- tracking_db: historial de simulacros terminados de cada estudiante.

-- Una fila por simulacro terminado, aprendida de simulation.completed (RF-25). Las estadísticas, las fortalezas y los
-- reportes se calculan sobre estas filas.
CREATE TABLE result_entry (
    simulation_id     VARCHAR(36)   PRIMARY KEY,
    student_id        VARCHAR(100)  NOT NULL,
    started_at        TIMESTAMPTZ,
    finished_at       TIMESTAMPTZ   NOT NULL,
    duration_seconds  BIGINT        NOT NULL,
    expired           BOOLEAN       NOT NULL,
    total             INTEGER       NOT NULL CHECK (total >= 1),
    correct           INTEGER       NOT NULL CHECK (correct >= 0),
    incorrect         INTEGER       NOT NULL,
    unanswered        INTEGER       NOT NULL,
    score             NUMERIC(5, 2) NOT NULL,
    by_competency     TEXT          NOT NULL
);

CREATE INDEX idx_result_student ON result_entry (student_id, finished_at DESC);

-- Idempotencia de los consumidores de eventos.
CREATE TABLE processed_event (
    event_id      VARCHAR(64)  PRIMARY KEY,
    processed_at  TIMESTAMPTZ  NOT NULL
);
