package co.unicauca.bancopreguntas.assignment.infrastructure.persistence;

import java.time.Instant;

/** Fila de la bandeja de un revisor (resultado de una consulta JPQL). */
public record InboxRow(String questionId, String title, String competency, String authorId, int round,
                       Instant assignedAt, String roundState, String decision) {
}
