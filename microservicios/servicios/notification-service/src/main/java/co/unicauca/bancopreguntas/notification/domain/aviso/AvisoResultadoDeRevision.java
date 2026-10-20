package co.unicauca.bancopreguntas.notification.domain.aviso;

import co.unicauca.bancopreguntas.notification.domain.Mensaje;
import co.unicauca.bancopreguntas.notification.domain.TipoNotificacion;

/** Avisa al administrador y a los revisores cómo terminó la revisión de una pregunta. */
public final class AvisoResultadoDeRevision extends CreadorDeAviso {

    @Override
    public TipoNotificacion tipo() {
        return TipoNotificacion.RESULTADO_DE_REVISION;
    }

    @Override
    protected Mensaje crearMensaje(ContextoDeAviso c) {
        String pregunta = c.nombreDeLaPregunta();
        StringBuilder cuerpo = new StringBuilder(c.aprobada()
                ? "La revisión de " + pregunta + " terminó: los revisores la aprobaron (ronda " + c.ronda()
                        + "). Queda lista para que el administrador la publique."
                : "La revisión de " + pregunta + " terminó: fue rechazada (ronda " + c.ronda()
                        + "). El autor puede corregirla y enviarla de nuevo.");
        agregarObservaciones(cuerpo, c.observaciones());
        return new Mensaje((c.aprobada() ? "Revisión terminada, aprobada: " : "Revisión terminada, rechazada: ") + pregunta,
                cuerpo.toString());
    }
}
