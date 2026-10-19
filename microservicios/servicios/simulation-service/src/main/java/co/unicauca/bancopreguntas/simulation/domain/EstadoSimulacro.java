package co.unicauca.bancopreguntas.simulation.domain;

/** Estado de un simulacro. */
public enum EstadoSimulacro {
    /** El estudiante lo está presentando y todavía tiene tiempo. */
    EN_CURSO,
    /** El estudiante lo finalizó dentro del tiempo. */
    FINALIZADO,
    /** Se agotó el tiempo: se calificó automáticamente con lo que alcanzó a contestar. */
    EXPIRADO
}
