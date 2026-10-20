package co.unicauca.bancopreguntas.question.domain;

/**
 * Estados por los que puede pasar una pregunta del banco de preguntas.
 * Corresponde exactamente al ciclo de vida definido en RF-14 del proyecto
 * de curso. Es el identificador del estado (se guarda y viaja en los eventos); lo que se puede hacer en cada
 * uno, incluidas las transiciones válidas (RF-15), lo decide su {@link FaseDePregunta} (patrón State).
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
        return fase().puedePasarA(destino);
    }

    /** El comportamiento de este estado (patrón State): qué se puede hacer con una pregunta que está en él. */
    public FaseDePregunta fase() {
        return FaseDePregunta.de(this);
    }

    @Override
    public String toString() {
        return etiqueta;
    }
}
