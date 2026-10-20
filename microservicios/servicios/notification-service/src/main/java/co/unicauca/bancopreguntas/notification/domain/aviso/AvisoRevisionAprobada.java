package co.unicauca.bancopreguntas.notification.domain.aviso;

import co.unicauca.bancopreguntas.notification.domain.Mensaje;
import co.unicauca.bancopreguntas.notification.domain.TipoNotificacion;

/** Avisa al autor que los revisores aprobaron su pregunta. */
public final class AvisoRevisionAprobada extends CreadorDeAviso {

    @Override
    public TipoNotificacion tipo() {
        return TipoNotificacion.REVISION_APROBADA;
    }

    @Override
    protected Mensaje crearMensaje(ContextoDeAviso c) {
        String pregunta = c.nombreDeLaPregunta();
        StringBuilder cuerpo = new StringBuilder("Los revisores aprobaron " + pregunta + " (ronda " + c.ronda()
                + "). Queda lista para que el administrador la publique.");
        agregarObservaciones(cuerpo, c.observaciones());
        return new Mensaje("Pregunta aprobada: " + pregunta, cuerpo.toString());
    }
}
