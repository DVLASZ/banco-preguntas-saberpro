package co.unicauca.bancopreguntas.email.api.dto;

import co.unicauca.bancopreguntas.email.domain.CorreoEnviado;
import co.unicauca.bancopreguntas.email.domain.EstadoCorreo;

import java.time.Instant;

/** Un correo registrado, tal como lo ve el administrador. */
public record EmailResponse(String notificacionId, String para, String destinatarioId, String asunto, String cuerpo,
                            EstadoCorreo estado, String error, Instant enviadoEn) {

    public static EmailResponse de(CorreoEnviado c) {
        return new EmailResponse(c.notificacionId(), c.para(), c.destinatarioId(), c.asunto(), c.cuerpo(), c.estado(),
                c.error(), c.intentadoEn());
    }
}
