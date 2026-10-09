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
