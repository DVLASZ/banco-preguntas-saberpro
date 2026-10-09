package co.unicauca.saberpro.preguntas.domain;

/**
 * Estados por los que puede pasar una pregunta del banco de preguntas.
 * Corresponde exactamente al ciclo de vida definido en RF-14 del proyecto
 * de curso (ver Taller 3 — modelo C4, Nivel 4: componente de Gestión de
 * Preguntas), y {@link #puedePasarA(EstadoPregunta)} concentra las
 * transiciones válidas entre ellos (RF-15).
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

    /**
     * Indica si una pregunta en este estado puede pasar al estado destino
     * (RF-15). Cualquier pregunta puede archivarse, porque RNF-16 prohíbe
     * eliminarlas físicamente.
     */
    public boolean puedePasarA(EstadoPregunta destino) {
        if (destino == null) {
            return false;
        }
        if (destino == ARCHIVADA) {
            return true;
        }
        return switch (this) {
            case BORRADOR -> destino == PENDIENTE_REVISION;
            case PENDIENTE_REVISION -> destino == EN_REVISION;
            case EN_REVISION -> destino == APROBADA || destino == RECHAZADA;
            case APROBADA -> destino == PUBLICADA;
            case RECHAZADA -> destino == BORRADOR;
            case PUBLICADA, ARCHIVADA -> false;
        };
    }

    @Override
    public String toString() {
        return etiqueta;
    }
}
