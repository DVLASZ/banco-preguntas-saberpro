package co.unicauca.bancopreguntas.tracking.domain;

/** Cómo viene evolucionando el puntaje de un estudiante. */
public enum Tendencia {
    /** Todavía no hay suficientes simulacros para opinar. */
    SIN_DATOS,
    /** Los simulacros recientes puntúan claramente más que los anteriores. */
    MEJORANDO,
    /** Sin cambios importantes. */
    ESTABLE,
    /** Los simulacros recientes puntúan claramente menos que los anteriores. */
    BAJANDO
}
