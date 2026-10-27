-- El estado, el historial y los eventos de las preguntas pasaron a lifecycle-service: aquí solo queda una copia del
-- estado. La secuencia es el número del último cambio de estado aplicado (avisos repetidos o atrasados se ignoran).
ALTER TABLE question ADD COLUMN estado_secuencia INTEGER NOT NULL DEFAULT 0 CHECK (estado_secuencia >= 0);

DROP TABLE question_state_history;
DROP TABLE processed_event;
DROP TABLE outbox_event;
