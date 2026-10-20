package co.unicauca.bancopreguntas.notification.domain.aviso;

import co.unicauca.bancopreguntas.notification.domain.Mensaje;
import co.unicauca.bancopreguntas.notification.domain.Notificacion;
import co.unicauca.bancopreguntas.notification.domain.ObservacionDeRevisor;
import co.unicauca.bancopreguntas.notification.domain.TipoNotificacion;

import java.time.Instant;
import java.util.List;

/**
 * Patrón Factory Method. Esta clase es el <em>creador</em>: define cómo se arma una {@link Notificacion}
 * ({@link #crear}) y deja a las subclases la decisión de qué {@link Mensaje} fabrican ({@link #crearMensaje},
 * el método fábrica). Cada tipo de aviso es un creador concreto; para agregar un tipo nuevo basta una subclase
 * y registrarla en {@link CreadoresDeAviso}, sin tocar el servicio de aplicación.
 */
public abstract class CreadorDeAviso {

    /** El tipo de notificación que este creador produce. */
    public abstract TipoNotificacion tipo();

    /** Método fábrica: redacta el asunto y el cuerpo propios de este tipo de aviso. */
    protected abstract Mensaje crearMensaje(ContextoDeAviso contexto);

    /** Crea la notificación completa, con el mensaje que fabrica la subclase. */
    public final Notificacion crear(String id, String destinatarioId, String destinatarioCorreo,
                                    ContextoDeAviso contexto, Instant ahora) {
        return Notificacion.nueva(id, destinatarioId, destinatarioCorreo, tipo(), contexto.preguntaId(),
                crearMensaje(contexto), ahora);
    }

    /** Agrega al cuerpo la lista de observaciones con texto; si no hay ninguna no agrega nada. */
    protected static void agregarObservaciones(StringBuilder cuerpo, List<ObservacionDeRevisor> observaciones) {
        List<ObservacionDeRevisor> conTexto = observaciones.stream()
                .filter(o -> o.texto() != null && !o.texto().isBlank()).toList();
        if (conTexto.isEmpty()) {
            return;
        }
        cuerpo.append("\nObservaciones:");
        conTexto.forEach(o -> cuerpo.append("\n- ").append(o.revisorId()).append(" (").append(o.decision())
                .append("): ").append(o.texto()));
    }
}
