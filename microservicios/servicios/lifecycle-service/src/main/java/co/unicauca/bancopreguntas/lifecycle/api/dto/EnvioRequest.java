package co.unicauca.bancopreguntas.lifecycle.api.dto;

import jakarta.validation.constraints.NotBlank;

/** Lo que el evento question.submitted necesita saber de la pregunta que se envía a revisión. */
public record EnvioRequest(
        @NotBlank(message = "El título es obligatorio") String titulo,
        @NotBlank(message = "La competencia es obligatoria") String competencia) {
}
