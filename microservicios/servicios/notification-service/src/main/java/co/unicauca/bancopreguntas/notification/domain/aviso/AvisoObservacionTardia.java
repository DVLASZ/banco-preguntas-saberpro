package co.unicauca.bancopreguntas.notification.domain.aviso;

import co.unicauca.bancopreguntas.notification.domain.Mensaje;
import co.unicauca.bancopreguntas.notification.domain.TipoNotificacion;

/** Avisa al autor de una evaluación que llegó cuando la ronda ya había sido rechazada. */
public final class AvisoObservacionTardia extends CreadorDeAviso {

    @Override
    public TipoNotificacion tipo() {
        return TipoNotificacion.OBSERVACION_TARDIA;
    }

    @Override
    protected Mensaje crearMensaje(ContextoDeAviso c) {
        String pregunta = c.nombreDeLaPregunta();
        StringBuilder cuerpo = new StringBuilder("El revisor " + c.revisorId() + " dejó su evaluación (" + c.decision()
                + ") sobre " + pregunta + " en la ronda " + c.ronda() + ", que ya había sido rechazada.");
        if (c.observacion() != null && !c.observacion().isBlank()) {
            cuerpo.append("\nObservación: ").append(c.observacion());
        }
        return new Mensaje("Nueva observación sobre " + pregunta, cuerpo.toString());
    }
}
