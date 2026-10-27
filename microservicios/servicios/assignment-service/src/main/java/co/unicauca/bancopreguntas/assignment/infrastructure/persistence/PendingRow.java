package co.unicauca.bancopreguntas.assignment.infrastructure.persistence;

import java.time.Instant;

/** Fila de la lista de preguntas por asignar (resultado de una consulta JPQL). */
public record PendingRow(String questionId, String title, String competency, String authorId, int round,
                         Instant startedAt) {
}
