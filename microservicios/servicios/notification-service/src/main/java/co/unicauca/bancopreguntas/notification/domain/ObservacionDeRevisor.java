package co.unicauca.bancopreguntas.notification.domain;

/** La evaluación de un revisor incluida en el resultado de una ronda. {@code texto} puede ser nulo. */
public record ObservacionDeRevisor(String revisorId, String decision, String texto) {
}
