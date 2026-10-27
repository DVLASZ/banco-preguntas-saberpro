package co.unicauca.bancopreguntas.evaluation.domain;

/** Lo que una {@link PoliticaDeDecision} concluye a partir de las evaluaciones recibidas hasta el momento. */
public enum ResultadoDeDecision {
    /** Todavía no se puede decidir. */
    PENDIENTE,
    APROBADA,
    RECHAZADA
}
