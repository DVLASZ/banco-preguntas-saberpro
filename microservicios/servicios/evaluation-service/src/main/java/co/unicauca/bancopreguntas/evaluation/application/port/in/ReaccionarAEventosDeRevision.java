package co.unicauca.bancopreguntas.evaluation.application.port.in;

import java.time.Instant;
import java.util.List;

/**
 * Puerto de entrada: lo que este servicio hace cuando otro servicio avisa algo sobre la revisión. Los adaptadores de
 * mensajería lo llaman; todas las operaciones son idempotentes.
 */
public interface ReaccionarAEventosDeRevision {

    /** {@code question.submitted}: el autor envió una versión de la pregunta; se abre su ronda. */
    void alEnviarseUnaPregunta(String eventId, String preguntaId, String autorId, String titulo,
                               String competencia, int version);

    /** {@code review.reviewers-assigned}: se asignaron revisores a una ronda. */
    void alAsignarseRevisores(String eventId, String preguntaId, String autorId, String titulo, String competencia,
                              int version, int ronda, List<String> revisores, String asignadoPor,
                              boolean automatica, Instant ocurridoEn);

    /** {@code question.reopened}: el autor reabrió la pregunta rechazada; la ronda abierta a observaciones se cierra. */
    void alReabrirseUnaPregunta(String eventId, String preguntaId);
}
