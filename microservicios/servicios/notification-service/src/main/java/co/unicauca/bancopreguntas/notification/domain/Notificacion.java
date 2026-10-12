package co.unicauca.bancopreguntas.notification.domain;

import java.time.Instant;

/**
 * Un aviso para un usuario. Además de quedar en su bandeja, se envía por correo (en este proyecto, simulado):
 * {@code correoEnviadoEn} es nulo mientras el envío no se haya hecho.
 */
public record Notificacion(String id, String destinatarioId, String destinatarioCorreo, TipoNotificacion tipo,
                           String preguntaId, String asunto, String mensaje, Instant creadaEn, Instant leidaEn,
                           Instant correoEnviadoEn) {

    public Notificacion {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("El identificador de la notificación es obligatorio");
        }
        if (destinatarioId == null || destinatarioId.isBlank()) {
            throw new IllegalArgumentException("El destinatario es obligatorio");
        }
        if (tipo == null) {
            throw new IllegalArgumentException("El tipo de notificación es obligatorio");
        }
        if (asunto == null || asunto.isBlank() || mensaje == null || mensaje.isBlank()) {
            throw new IllegalArgumentException("El asunto y el mensaje son obligatorios");
        }
        if (creadaEn == null) {
            throw new IllegalArgumentException("La fecha de creación es obligatoria");
        }
    }

    /** Una notificación recién creada: sin leer y sin correo enviado. */
    public static Notificacion nueva(String id, String destinatarioId, String destinatarioCorreo, TipoNotificacion tipo,
                                     String preguntaId, Mensaje mensaje, Instant ahora) {
        return new Notificacion(id, destinatarioId, destinatarioCorreo, tipo, preguntaId, mensaje.asunto(),
                mensaje.cuerpo(), ahora, null, null);
    }

    public boolean leida() {
        return leidaEn != null;
    }

    /** La misma notificación marcada como leída; si ya lo estaba, no cambia la fecha original. */
    public Notificacion marcadaComoLeida(Instant ahora) {
        return leida() ? this : new Notificacion(id, destinatarioId, destinatarioCorreo, tipo, preguntaId, asunto,
                mensaje, creadaEn, ahora, correoEnviadoEn);
    }

    /** La misma notificación con el correo registrado como enviado. */
    public Notificacion conCorreoEnviado(Instant ahora) {
        return new Notificacion(id, destinatarioId, destinatarioCorreo, tipo, preguntaId, asunto, mensaje, creadaEn,
                leidaEn, ahora);
    }

    /** El correo que corresponde a esta notificación. */
    public Correo comoCorreo() {
        return new Correo(destinatarioCorreo, asunto, mensaje);
    }
}
