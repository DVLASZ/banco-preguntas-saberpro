package co.unicauca.bancopreguntas.lifecycle.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Registro de una pregunta recién creada. El autor es quien hace la petición. */
public record RegistroRequest(
        @NotBlank(message = "El id de la pregunta es obligatorio")
        @Size(max = 36, message = "El id de la pregunta no puede superar 36 caracteres")
        String preguntaId) {
}
