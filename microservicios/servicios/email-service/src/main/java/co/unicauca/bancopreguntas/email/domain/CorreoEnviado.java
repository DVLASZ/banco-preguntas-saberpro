package co.unicauca.bancopreguntas.email.domain;

import java.time.Instant;

/**
 * El registro de un correo que se intentó enviar a partir de una notificación: a quién, qué decía y cómo terminó.
 *
 * @param notificacionId la notificación que lo originó
 * @param error          el motivo si el envío falló; {@code null} si salió bien
 * @param intentadoEn    cuándo se hizo el envío
 */
public record CorreoEnviado(String id, String notificacionId, String destinatarioId, String para, String asunto,
                            String cuerpo, EstadoCorreo estado, String error, Instant intentadoEn) {

    public CorreoEnviado {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("El identificador del correo es obligatorio");
        }
        if (para == null || para.isBlank()) {
            throw new IllegalArgumentException("El destinatario del correo es obligatorio");
        }
        if (estado == null || intentadoEn == null) {
            throw new IllegalArgumentException("El estado y la fecha del envío son obligatorios");
        }
        if (estado == EstadoCorreo.FALLIDO && (error == null || error.isBlank())) {
            throw new IllegalArgumentException("Un envío fallido debe decir por qué falló");
        }
    }

    /** El correo que salió bien. */
    public static CorreoEnviado enviado(String id, String notificacionId, String destinatarioId, Correo correo, Instant ahora) {
        return new CorreoEnviado(id, notificacionId, destinatarioId, correo.para(), correo.asunto(), correo.cuerpo(),
                EstadoCorreo.ENVIADO, null, ahora);
    }

    /** El correo que no se pudo enviar, con el motivo. */
    public static CorreoEnviado fallido(String id, String notificacionId, String destinatarioId, Correo correo, String error,
                                        Instant ahora) {
        return new CorreoEnviado(id, notificacionId, destinatarioId, correo.para(), correo.asunto(), correo.cuerpo(),
                EstadoCorreo.FALLIDO, error == null || error.isBlank() ? "Error desconocido" : error, ahora);
    }
}
