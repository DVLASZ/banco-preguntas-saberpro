package co.unicauca.saberpro.preguntas.domain;

/**
 * Competencias evaluadas por las Pruebas Saber Pro (HU-03: toda pregunta
 * se clasifica por competencia, tema y nivel de dificultad para poder
 * filtrarla después — HU-05 — y usarla en la generación de simulacros —
 * HU-12).
 */
public enum Competencia {
    LECTURA_CRITICA("Lectura crítica"),
    RAZONAMIENTO_CUANTITATIVO("Razonamiento cuantitativo"),
    COMPETENCIAS_CIUDADANAS("Competencias ciudadanas"),
    COMUNICACION_ESCRITA("Comunicación escrita"),
    INGLES("Inglés");

    private final String etiqueta;

    Competencia(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    @Override
    public String toString() {
        return etiqueta;
    }
}
