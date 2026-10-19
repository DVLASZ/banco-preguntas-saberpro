package co.unicauca.bancopreguntas.history.domain;

/**
 * Qué se registró en el historial. {@link #visibleParaElAutor()} marca lo que el autor puede ver de sus
 * preguntas: el rastro de su pregunta y los resultados de las rondas, pero no las evaluaciones individuales
 * mientras la ronda está en curso (esas llegan a él ya resumidas en {@link #REVISION_COMPLETADA}).
 */
public enum TipoEntrada {
    PREGUNTA_ENVIADA(true),
    PREGUNTA_REABIERTA(true),
    PREGUNTA_PUBLICADA(true),
    CAMBIO_DE_ESTADO(true),
    REVISORES_ASIGNADOS(false),
    EVALUACION_ENVIADA(false),
    EVALUACION_TARDIA(true),
    REVISION_COMPLETADA(true),
    INICIO_DE_SESION(false),
    INICIO_DE_SESION_FALLIDO(false);

    private final boolean visibleParaElAutor;

    TipoEntrada(boolean visibleParaElAutor) {
        this.visibleParaElAutor = visibleParaElAutor;
    }

    public boolean visibleParaElAutor() {
        return visibleParaElAutor;
    }

    /** Si la entrada es de la seguridad del sistema y no de una pregunta (solo la ve el administrador). */
    public boolean esDeSeguridad() {
        return this == INICIO_DE_SESION || this == INICIO_DE_SESION_FALLIDO;
    }
}
