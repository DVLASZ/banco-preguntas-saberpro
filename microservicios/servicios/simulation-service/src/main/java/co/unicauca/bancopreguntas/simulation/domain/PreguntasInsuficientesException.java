package co.unicauca.bancopreguntas.simulation.domain;

/** El catálogo no tiene suficientes preguntas publicadas que cumplan los criterios. */
public class PreguntasInsuficientesException extends RuntimeException {

    private final int disponibles;
    private final int solicitadas;

    public PreguntasInsuficientesException(int disponibles, int solicitadas) {
        super("Solo hay " + disponibles + " pregunta(s) publicada(s) que cumplen los criterios y se pidieron "
                + solicitadas + ". Pide menos preguntas o amplía los criterios.");
        this.disponibles = disponibles;
        this.solicitadas = solicitadas;
    }

    public int disponibles() {
        return disponibles;
    }

    public int solicitadas() {
        return solicitadas;
    }
}
