-- question_db: preguntas, historial de estados y eventos procesados.
-- Sin borrado físico (RNF-16): ninguna operación del servicio elimina filas de estas tablas.

-- Permite buscar sin distinguir tildes (HU-03, RF-07).
CREATE EXTENSION IF NOT EXISTS unaccent;

CREATE TABLE question (
    id                  VARCHAR(36)   PRIMARY KEY,
    author_id           VARCHAR(100)  NOT NULL,
    nombre              VARCHAR(200)  NOT NULL,
    contexto            TEXT          NOT NULL,
    enunciado           TEXT          NOT NULL,
    opcion_a            VARCHAR(500)  NOT NULL,
    opcion_b            VARCHAR(500)  NOT NULL,
    opcion_c            VARCHAR(500)  NOT NULL,
    opcion_d            VARCHAR(500)  NOT NULL,
    respuesta_correcta  VARCHAR(1)    NOT NULL CHECK (respuesta_correcta IN ('A', 'B', 'C', 'D')),
    justificacion       TEXT          NOT NULL,
    bibliografia        VARCHAR(1000) NOT NULL,
    competencia         VARCHAR(40)   NOT NULL,
    tema                VARCHAR(200)  NOT NULL,
    subtema             VARCHAR(200)  NOT NULL,
    dificultad          VARCHAR(20)   NOT NULL,
    estado              VARCHAR(30)   NOT NULL,
    creada_en           TIMESTAMPTZ   NOT NULL,
    actualizada_en      TIMESTAMPTZ   NOT NULL
);

CREATE INDEX idx_question_author_estado ON question (author_id, estado);
CREATE INDEX idx_question_estado ON question (estado);

CREATE TABLE question_state_history (
    id            BIGSERIAL    PRIMARY KEY,
    question_id   VARCHAR(36)  NOT NULL REFERENCES question (id),
    desde         VARCHAR(30),
    hacia         VARCHAR(30)  NOT NULL,
    cambiado_por  VARCHAR(100),
    fecha         TIMESTAMPTZ  NOT NULL
);

CREATE INDEX idx_state_history_question ON question_state_history (question_id, fecha, id);

-- Idempotencia de los consumidores de eventos.
CREATE TABLE processed_event (
    event_id      VARCHAR(64)  PRIMARY KEY,
    processed_at  TIMESTAMPTZ  NOT NULL
);
