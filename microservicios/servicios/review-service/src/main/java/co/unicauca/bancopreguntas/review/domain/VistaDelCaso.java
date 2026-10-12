package co.unicauca.bancopreguntas.review.domain;

import java.time.Instant;
import java.util.List;

/**
 * Una revisión tal como la puede ver un {@link Observador}: el administrador y los revisores ven todo; el
 * autor ve el detalle de las rondas ya decididas y solo los totales de la que sigue en curso.
 */
public record VistaDelCaso(String preguntaId, String autorId, String titulo, String competencia,
                           List<VistaDeRonda> rondas) {

    /**
     * @param detalleVisible si el observador puede ver los revisores y sus evaluaciones; si es {@code false},
     *                       {@code revisores} viene vacío y solo valen los totales
     */
    public record VistaDeRonda(int numero, int versionDePregunta, EstadoRonda estado, Instant iniciadaEn,
                               Instant decididaEn, Instant cerradaEn, int totalDeRevisores, int totalDeEvaluadas,
                               boolean detalleVisible, List<VistaDeRevisor> revisores) {
    }

    /** Un revisor de la ronda con su evaluación, que es nula mientras no haya evaluado. */
    public record VistaDeRevisor(String revisorId, String asignadoPor, Instant asignadoEn, boolean automatica,
                                 Evaluacion evaluacion) {
    }
}
