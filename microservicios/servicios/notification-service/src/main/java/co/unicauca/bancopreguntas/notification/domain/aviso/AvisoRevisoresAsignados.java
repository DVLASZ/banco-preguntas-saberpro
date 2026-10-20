package co.unicauca.bancopreguntas.notification.domain.aviso;

import co.unicauca.bancopreguntas.notification.domain.Mensaje;
import co.unicauca.bancopreguntas.notification.domain.TipoNotificacion;

/** Avisa al revisor que tiene una pregunta por evaluar. */
public final class AvisoRevisoresAsignados extends CreadorDeAviso {

    @Override
    public TipoNotificacion tipo() {
        return TipoNotificacion.REVISORES_ASIGNADOS;
    }

    @Override
    protected Mensaje crearMensaje(ContextoDeAviso c) {
        String pregunta = c.nombreDeLaPregunta();
        String motivo = c.automatica()
                ? "El autor corrigió la pregunta y la envió de nuevo; como ya la habías revisado, quedas asignado otra vez "
                        + "(ronda " + c.ronda() + ")."
                : "El administrador te asignó como revisor (ronda " + c.ronda() + ").";
        return new Mensaje("Pregunta por revisar: " + pregunta,
                "Tienes pendiente evaluar " + pregunta + ". " + motivo
                        + " Entra a tu bandeja de revisión para aprobarla o rechazarla.");
    }
}
