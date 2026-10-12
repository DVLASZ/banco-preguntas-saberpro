package co.unicauca.bancopreguntas.review.domain;

/**
 * Estado de una ronda de revisión.
 *
 * <pre>
 * PENDIENTE_ASIGNACION --asigna el administrador--> EN_CURSO --unanimidad--> APROBADA
 *                                                      |
 *                                          primer rechazo
 *                                                      v
 *                                                  RECHAZADA --el autor reabre la pregunta--> CERRADA
 * </pre>
 *
 * Una ronda {@code RECHAZADA} ya devolvió la pregunta al autor, pero sigue abierta a las observaciones
 * de los revisores que faltaban, hasta que el autor la reabre.
 */
public enum EstadoRonda {
    PENDIENTE_ASIGNACION,
    EN_CURSO,
    RECHAZADA,
    APROBADA,
    CERRADA;

    /** Si todavía se pueden asignar revisores a la ronda. */
    public boolean aceptaAsignaciones() {
        return this == PENDIENTE_ASIGNACION || this == EN_CURSO;
    }

    /** Si la ronda ya tiene una decisión (aprobada o rechazada), aunque siga abierta a observaciones. */
    public boolean decidida() {
        return this == RECHAZADA || this == APROBADA || this == CERRADA;
    }
}
