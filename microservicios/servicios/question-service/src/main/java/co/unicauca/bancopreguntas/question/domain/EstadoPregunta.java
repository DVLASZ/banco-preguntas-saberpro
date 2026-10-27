package co.unicauca.bancopreguntas.question.domain;

/**
 * Estados por los que puede pasar una pregunta del banco de preguntas. Corresponde al ciclo de vida definido en
 * RF-14 del proyecto de curso. Este servicio solo guarda una copia del estado (para listar, filtrar y saber si la
 * pregunta todavía se puede editar); quien decide las transiciones válidas (RF-15) es lifecycle-service.
 */
public enum EstadoPregunta {
    BORRADOR("Borrador"),
    PENDIENTE_REVISION("Pendiente de revisión"),
    EN_REVISION("En revisión"),
    APROBADA("Aprobada"),
    RECHAZADA("Rechazada"),
    PUBLICADA("Publicada"),
    ARCHIVADA("Archivada");

    private final String etiqueta;

    EstadoPregunta(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    /** Solo en Borrador el autor puede modificar el contenido (RF-06). */
    public boolean editable() {
        return this == BORRADOR;
    }

    @Override
    public String toString() {
        return etiqueta;
    }
}
