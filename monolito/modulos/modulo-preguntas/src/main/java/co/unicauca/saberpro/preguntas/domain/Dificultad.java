package co.unicauca.saberpro.preguntas.domain;

/** Nivel de dificultad de una pregunta (HU-03), usado luego para filtrarla (HU-05, HU-12). */
public enum Dificultad {
    BASICO("Básico"),
    INTERMEDIO("Intermedio"),
    AVANZADO("Avanzado");

    private final String etiqueta;

    Dificultad(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    @Override
    public String toString() {
        return etiqueta;
    }
}
