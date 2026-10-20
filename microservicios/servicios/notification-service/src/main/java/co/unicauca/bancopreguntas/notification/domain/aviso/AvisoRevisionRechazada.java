package co.unicauca.bancopreguntas.notification.domain.aviso;

import co.unicauca.bancopreguntas.notification.domain.Mensaje;
import co.unicauca.bancopreguntas.notification.domain.TipoNotificacion;

/** Avisa al autor que un revisor rechazó su pregunta, con las observaciones recibidas. */
public final class AvisoRevisionRechazada extends CreadorDeAviso {

    @Override
    public TipoNotificacion tipo() {
        return TipoNotificacion.REVISION_RECHAZADA;
    }

    @Override
    protected Mensaje crearMensaje(ContextoDeAviso c) {
        String pregunta = c.nombreDeLaPregunta();
        StringBuilder cuerpo = new StringBuilder("Un revisor rechazó " + pregunta + " (ronda " + c.ronda()
                + "). Puedes reabrirla, corregirla y enviarla de nuevo; los demás revisores aún pueden dejar sus observaciones.");
        agregarObservaciones(cuerpo, c.observaciones());
        return new Mensaje("Pregunta rechazada: " + pregunta, cuerpo.toString());
    }
}
