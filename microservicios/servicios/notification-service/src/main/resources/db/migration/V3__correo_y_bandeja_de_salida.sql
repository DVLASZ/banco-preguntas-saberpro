-- El correo pasó al email-service: la notificación ya no guarda la dirección ni la marca de envío.
DROP INDEX IF EXISTS idx_notification_email;
ALTER TABLE notification DROP COLUMN recipient_email;
ALTER TABLE notification DROP COLUMN email_sent_at;

-- Patrón Outbox: el evento notification.created se guarda aquí en la misma transacción que la notificación y un
-- relevo lo publica en el broker; así no se pierde si RabbitMQ está caído.
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
