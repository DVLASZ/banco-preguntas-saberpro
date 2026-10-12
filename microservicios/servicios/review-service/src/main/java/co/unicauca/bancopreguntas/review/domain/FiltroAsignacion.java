package co.unicauca.bancopreguntas.review.domain;

/** Qué asignaciones de la bandeja de un revisor se piden. */
public enum FiltroAsignacion {
    /** Las que todavía puede evaluar (ronda en curso, o rechazada y abierta a observaciones). */
    PENDIENTES,
    /** Las que ya evaluó. */
    EVALUADAS,
    TODAS
}
