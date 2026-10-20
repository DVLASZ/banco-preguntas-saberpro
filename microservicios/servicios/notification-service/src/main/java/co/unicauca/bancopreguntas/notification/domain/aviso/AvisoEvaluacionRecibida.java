package co.unicauca.bancopreguntas.notification.domain.aviso;

import co.unicauca.bancopreguntas.notification.domain.Mensaje;
import co.unicauca.bancopreguntas.notification.domain.TipoNotificacion;

/** Avisa al administrador que un revisor envió su evaluación. */
public final class AvisoEvaluacionRecibida extends CreadorDeAviso {

    @Override
    public TipoNotificacion tipo() {
        return TipoNotificacion.EVALUACION_RECIBIDA;
    }

    @Override
    protected Mensaje crearMensaje(ContextoDeAviso c) {
        String pregunta = c.nombreDeLaPregunta();
        StringBuilder cuerpo = new StringBuilder("El revisor " + c.revisorId() + " evaluó " + pregunta + " en la ronda "
                + c.ronda() + ": " + c.decision() + ".");
        if (c.observacion() != null && !c.observacion().isBlank()) {
            cuerpo.append("\nObservación: ").append(c.observacion());
        }
        return new Mensaje("Evaluación recibida: " + pregunta, cuerpo.toString());
    }
}
