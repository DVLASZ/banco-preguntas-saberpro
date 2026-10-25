package co.unicauca.bancopreguntas.notification.domain;

import java.time.Instant;

/**
 * Un aviso para un usuario, que queda en su bandeja. El correo que lo acompaña lo envía el {@code email-service}, que
 * se entera por el evento {@code notification.created}.
 */
public record Notificacion(String id, String destinatarioId, TipoNotificacion tipo, String preguntaId, String asunto,
                           String mensaje, Instant creadaEn, Instant leidaEn) {

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

    /** Una notificación recién creada y sin leer. */
    public static Notificacion nueva(String id, String destinatarioId, TipoNotificacion tipo, String preguntaId,
                                     Mensaje mensaje, Instant ahora) {
        return new Notificacion(id, destinatarioId, tipo, preguntaId, mensaje.asunto(), mensaje.cuerpo(), ahora, null);
    }

    public boolean leida() {
        return leidaEn != null;
    }

    /** La misma notificación marcada como leída; si ya lo estaba, no cambia la fecha original. */
    public Notificacion marcadaComoLeida(Instant ahora) {
        return leida() ? this : new Notificacion(id, destinatarioId, tipo, preguntaId, asunto, mensaje, creadaEn, ahora);
    }
}
