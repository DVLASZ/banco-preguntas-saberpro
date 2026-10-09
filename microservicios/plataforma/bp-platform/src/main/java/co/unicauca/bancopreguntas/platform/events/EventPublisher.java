package co.unicauca.bancopreguntas.platform.events;

/**
 * Publica un evento para los demás servicios. Hay dos implementaciones: el {@link OutboxEventPublisher}, que
 * guarda el evento en la misma transacción que el cambio de negocio y no se pierde aunque RabbitMQ esté caído, y el
 * {@link AmqpEventPublisher}, que lo envía directamente después del commit.
 */
public interface EventPublisher {

    /**
     * @param tipo  tipo del evento; es también la routing key (por ejemplo {@code question.submitted})
     * @param datos cuerpo del evento, serializable a JSON
     * @return el identificador único asignado al evento
     */
    String publicar(String tipo, Object datos);
}
