package co.unicauca.bancopreguntas.notification.domain.event;

import java.time.Instant;

/**
 * Se creó una notificación en la bandeja de un usuario. Lo consume el {@code email-service}, que envía el correo
 * correspondiente.
 *
 * @param notificationId identificador de la notificación
 * @param recipientId    el usuario al que se avisa
 * @param kind           el tipo de aviso (por ejemplo {@code REVISORES_ASIGNADOS})
 * @param questionId     la pregunta a la que se refiere, si la hay
 * @param subject        asunto del aviso
 * @param message        cuerpo del aviso
 */
public record NotificationCreated(String notificationId, String recipientId, String kind, String questionId,
                                  String subject, String message, Instant createdAt) implements DomainEvent {

    public static final String TYPE = "notification.created";

    @Override
    public String type() {
        return TYPE;
    }
}
