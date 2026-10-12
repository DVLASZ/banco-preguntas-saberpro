package co.unicauca.bancopreguntas.notification.domain;

/** Qué ocurrió para que se notifique al usuario. */
public enum TipoNotificacion {
    /** Se asignó (o reasignó) al revisor una pregunta para evaluar. */
    REVISORES_ASIGNADOS,
    /** La revisión de la pregunta del autor terminó aprobada. */
    REVISION_APROBADA,
    /** La revisión de la pregunta del autor terminó rechazada. */
    REVISION_RECHAZADA,
    /** Un revisor dejó su observación después de que la ronda ya se había rechazado. */
    OBSERVACION_TARDIA,
    /** El administrador publicó la pregunta del autor. */
    PREGUNTA_PUBLICADA
}
