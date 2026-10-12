package co.unicauca.bancopreguntas.review.domain;

import java.time.Instant;

/**
 * La evaluación de un revisor. Es definitiva: una vez enviada no se modifica.
 *
 * @param observacion obligatoria al rechazar, opcional al aprobar (puede ser nula)
 * @param tardia      si llegó después de que la ronda ya se había rechazado; no cambia el resultado,
 *                    pero el autor la lee junto con las demás
 */
public record Evaluacion(String revisorId, Decision decision, String observacion, Instant enviadaEn, boolean tardia) {

    public Evaluacion {
        if (revisorId == null || revisorId.isBlank()) {
            throw new IllegalArgumentException("El revisor es obligatorio");
        }
        if (decision == null) {
            throw new IllegalArgumentException("La decisión es obligatoria");
        }
        if (enviadaEn == null) {
            throw new IllegalArgumentException("La fecha de la evaluación es obligatoria");
        }
    }
}
