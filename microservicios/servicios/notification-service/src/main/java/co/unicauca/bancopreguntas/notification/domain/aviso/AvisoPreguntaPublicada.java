package co.unicauca.bancopreguntas.notification.domain.aviso;

import co.unicauca.bancopreguntas.notification.domain.Mensaje;
import co.unicauca.bancopreguntas.notification.domain.TipoNotificacion;

/** Avisa al autor que el administrador publicó su pregunta. */
public final class AvisoPreguntaPublicada extends CreadorDeAviso {

    @Override
    public TipoNotificacion tipo() {
        return TipoNotificacion.PREGUNTA_PUBLICADA;
    }

    @Override
    protected Mensaje crearMensaje(ContextoDeAviso c) {
        String pregunta = c.nombreDeLaPregunta();
        return new Mensaje("Pregunta publicada: " + pregunta,
                "El administrador publicó " + pregunta + ". Ya hace parte del banco de preguntas.");
    }
}
