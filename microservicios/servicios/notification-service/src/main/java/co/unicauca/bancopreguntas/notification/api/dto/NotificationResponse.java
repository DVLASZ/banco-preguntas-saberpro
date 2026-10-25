package co.unicauca.bancopreguntas.notification.api.dto;

import co.unicauca.bancopreguntas.notification.domain.Notificacion;
import co.unicauca.bancopreguntas.notification.domain.TipoNotificacion;

import java.time.Instant;

/** Una notificación de la bandeja. */
public record NotificationResponse(String id, TipoNotificacion tipo, String preguntaId, String asunto, String mensaje,
                                   Instant creadaEn, boolean leida, Instant leidaEn) {

    public static NotificationResponse de(Notificacion n) {
        return new NotificationResponse(n.id(), n.tipo(), n.preguntaId(), n.asunto(), n.mensaje(), n.creadaEn(),
                n.leida(), n.leidaEn());
    }
}
