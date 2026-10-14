package co.unicauca.bancopreguntas.notification.api.dto;

import co.unicauca.bancopreguntas.notification.domain.Notificacion;

import java.time.Instant;

/** Un correo simulado ya enviado, tal como lo ve el administrador. */
public record EmailResponse(String notificacionId, String para, String destinatarioId, String asunto, String cuerpo,
                            Instant enviadoEn) {

    public static EmailResponse de(Notificacion n) {
        return new EmailResponse(n.id(), n.destinatarioCorreo(), n.destinatarioId(), n.asunto(), n.mensaje(),
                n.correoEnviadoEn());
    }
}
