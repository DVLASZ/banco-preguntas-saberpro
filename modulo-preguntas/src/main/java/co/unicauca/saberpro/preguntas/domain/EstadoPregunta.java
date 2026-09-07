package co.unicauca.saberpro.preguntas.domain;

/**
 * Estados por los que puede pasar una pregunta del banco de preguntas.
 * Corresponde exactamente al ciclo de vida definido en RF-14 del proyecto
 * de curso (ver Taller 3 — modelo C4, Nivel 4: componente de Gestión de
 * Preguntas), para que este taller sea congruente con el resto del
 * proyecto en vez de usar una simplificación propia.
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

    @Override
    public String toString() {
        return etiqueta;
    }
}
