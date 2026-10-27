package co.unicauca.bancopreguntas.assignment.domain;

import java.time.Instant;

/**
 * Una pregunta de la bandeja de un revisor (HU-05).
 *
 * @param miDecision su decisión, o {@code null} si todavía no evalúa
 */
public record AsignacionDeRevisor(String preguntaId, String titulo, String competencia, String autorId, int ronda,
                                  Instant asignadaEn, EstadoRonda estadoRonda, Decision miDecision) {

    public boolean pendiente() {
        return miDecision == null && (estadoRonda == EstadoRonda.EN_CURSO || estadoRonda == EstadoRonda.RECHAZADA);
    }
}
