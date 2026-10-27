package co.unicauca.bancopreguntas.lifecycle.api.dto;

import co.unicauca.bancopreguntas.lifecycle.domain.ContenidoPublicado;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/** El contenido de la pregunta que se publica, para el evento question.published. */
public record PublicacionRequest(
        @NotBlank(message = "El título es obligatorio") String titulo,
        @NotBlank(message = "La competencia es obligatoria") String competencia,
        @NotBlank(message = "El tema es obligatorio") String tema,
        String subtema,
        @NotBlank(message = "La dificultad es obligatoria") String dificultad,
        String contexto,
        @NotBlank(message = "El enunciado es obligatorio") String enunciado,
        @NotBlank(message = "La opción A es obligatoria") String opcionA,
        @NotBlank(message = "La opción B es obligatoria") String opcionB,
        @NotBlank(message = "La opción C es obligatoria") String opcionC,
        @NotBlank(message = "La opción D es obligatoria") String opcionD,
        @NotBlank(message = "La respuesta correcta es obligatoria")
        @Pattern(regexp = "[A-D]", message = "La respuesta correcta debe ser A, B, C o D")
        String respuestaCorrecta) {

    public ContenidoPublicado aContenido() {
        return new ContenidoPublicado(titulo, competencia, tema, subtema == null ? "" : subtema, dificultad,
                contexto == null ? "" : contexto, enunciado, opcionA, opcionB, opcionC, opcionD, respuestaCorrecta);
    }
}
