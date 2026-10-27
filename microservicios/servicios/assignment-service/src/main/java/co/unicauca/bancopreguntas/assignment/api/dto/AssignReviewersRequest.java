package co.unicauca.bancopreguntas.assignment.api.dto;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

/** Cuerpo de la asignación de revisores a la ronda vigente de una pregunta. */
public record AssignReviewersRequest(@NotEmpty(message = "Debe indicarse al menos un revisor") List<String> reviewerIds) {
}
