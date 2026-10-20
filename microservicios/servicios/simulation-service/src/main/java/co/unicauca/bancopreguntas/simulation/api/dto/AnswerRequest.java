package co.unicauca.bancopreguntas.simulation.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/** La respuesta que da el estudiante a una pregunta del simulacro. */
public record AnswerRequest(
        @NotBlank(message = "preguntaId es obligatorio") String preguntaId,
        @NotBlank(message = "opcion es obligatoria")
        @Pattern(regexp = "[A-Da-d]", message = "opcion debe ser A, B, C o D") String opcion) {
}
