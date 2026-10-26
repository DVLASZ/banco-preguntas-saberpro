package co.unicauca.bancopreguntas.audit.domain;

/** Qué se registró en la auditoría: un hecho de una pregunta, de su revisión o de la seguridad del sistema. */
public enum TipoEntrada {
    PREGUNTA_ENVIADA,
    PREGUNTA_REABIERTA,
    PREGUNTA_PUBLICADA,
    CAMBIO_DE_ESTADO,
    REVISORES_ASIGNADOS,
    EVALUACION_ENVIADA,
    EVALUACION_TARDIA,
    REVISION_COMPLETADA,
    INICIO_DE_SESION,
    INICIO_DE_SESION_FALLIDO;

    /** Si la entrada es de la seguridad del sistema y no de una pregunta. */
    public boolean esDeSeguridad() {
        return this == INICIO_DE_SESION || this == INICIO_DE_SESION_FALLIDO;
    }
}
