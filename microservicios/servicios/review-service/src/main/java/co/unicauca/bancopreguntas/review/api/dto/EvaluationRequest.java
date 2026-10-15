package co.unicauca.bancopreguntas.review.api.dto;

import co.unicauca.bancopreguntas.review.domain.Decision;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Cuerpo de la evaluación de un revisor. La observación es obligatoria al rechazar. */
public record EvaluationRequest(
        @NotNull(message = "La decisión es obligatoria (APROBAR o RECHAZAR)") Decision decision,
        @Size(max = 4000, message = "La observación no puede superar 4000 caracteres") String observacion) {
}
