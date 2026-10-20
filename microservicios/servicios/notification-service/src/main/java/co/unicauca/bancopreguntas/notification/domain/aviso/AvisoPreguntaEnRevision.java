package co.unicauca.bancopreguntas.notification.domain.aviso;

import co.unicauca.bancopreguntas.notification.domain.Mensaje;
import co.unicauca.bancopreguntas.notification.domain.TipoNotificacion;

/** Avisa al autor que su pregunta entró en revisión porque ya tiene revisores asignados. */
public final class AvisoPreguntaEnRevision extends CreadorDeAviso {

    @Override
    public TipoNotificacion tipo() {
        return TipoNotificacion.PREGUNTA_EN_REVISION;
    }

    @Override
    protected Mensaje crearMensaje(ContextoDeAviso c) {
        String pregunta = c.nombreDeLaPregunta();
        return new Mensaje("Tu pregunta está en revisión: " + pregunta,
                "Se asignaron revisores a " + pregunta + " (ronda " + c.ronda() + ") y su estado ahora es «En revisión». "
                        + "Te avisaremos cuando termine la revisión.");
    }
}
