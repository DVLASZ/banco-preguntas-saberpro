package co.unicauca.bancopreguntas.notification.domain.aviso;

import co.unicauca.bancopreguntas.notification.domain.ObservacionDeRevisor;

import java.util.List;

/**
 * Los datos que un creador de aviso necesita para redactar su mensaje. Cada tipo de aviso usa solo una parte.
 */
public record ContextoDeAviso(String titulo, String preguntaId, int ronda, boolean automatica, boolean aprobada,
                              List<ObservacionDeRevisor> observaciones,
                              String revisorId, String decision, String observacion) {

    public ContextoDeAviso {
        observaciones = observaciones == null ? List.of() : List.copyOf(observaciones);
    }

    /** Contexto mínimo: la pregunta de la que trata el aviso. */
    public static ContextoDeAviso de(String titulo, String preguntaId) {
        return new ContextoDeAviso(titulo, preguntaId, 1, false, false, List.of(), null, null, null);
    }

    public ContextoDeAviso enRonda(int nuevaRonda) {
        return new ContextoDeAviso(titulo, preguntaId, nuevaRonda, automatica, aprobada, observaciones, revisorId, decision, observacion);
    }

    public ContextoDeAviso asignadaAutomaticamente(boolean esAutomatica) {
        return new ContextoDeAviso(titulo, preguntaId, ronda, esAutomatica, aprobada, observaciones, revisorId, decision, observacion);
    }

    public ContextoDeAviso conResultado(boolean fueAprobada) {
        return new ContextoDeAviso(titulo, preguntaId, ronda, automatica, fueAprobada, observaciones, revisorId, decision, observacion);
    }

    public ContextoDeAviso conObservaciones(List<ObservacionDeRevisor> nuevas) {
        return new ContextoDeAviso(titulo, preguntaId, ronda, automatica, aprobada, nuevas, revisorId, decision, observacion);
    }

    public ContextoDeAviso conEvaluacion(String revisor, String decisionTomada, String texto) {
        return new ContextoDeAviso(titulo, preguntaId, ronda, automatica, aprobada, observaciones, revisor, decisionTomada, texto);
    }

    /** Cómo se nombra la pregunta en el texto: por su título, o por su identificador si no se conoce. */
    public String nombreDeLaPregunta() {
        return titulo == null || titulo.isBlank() ? "la pregunta " + preguntaId : "«" + titulo + "»";
    }
}
