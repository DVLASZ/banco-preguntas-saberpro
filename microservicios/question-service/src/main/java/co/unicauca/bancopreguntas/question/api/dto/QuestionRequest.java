package co.unicauca.bancopreguntas.question.api.dto;

import co.unicauca.bancopreguntas.question.domain.Competencia;
import co.unicauca.bancopreguntas.question.domain.ContenidoPregunta;
import co.unicauca.bancopreguntas.question.domain.Dificultad;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Cuerpo JSON de POST y PUT. El id, el autor y el estado los decide el servidor. Además de estas
 * comprobaciones de formato, el dominio aplica la validación estructural de la pregunta (HU-01).
 */
public record QuestionRequest(
        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 200, message = "El nombre no puede superar 200 caracteres")
        String nombre,
        @NotBlank(message = "El contexto es obligatorio")
        @Size(max = 4000, message = "El contexto no puede superar 4000 caracteres")
        String contexto,
        @NotBlank(message = "La pregunta directa (enunciado) es obligatoria")
        @Size(max = 2000, message = "El enunciado no puede superar 2000 caracteres")
        String enunciado,
        @NotBlank(message = "La opción A es obligatoria")
        @Size(max = 500, message = "La opción A no puede superar 500 caracteres")
        String opcionA,
        @NotBlank(message = "La opción B es obligatoria")
        @Size(max = 500, message = "La opción B no puede superar 500 caracteres")
        String opcionB,
        @NotBlank(message = "La opción C es obligatoria")
        @Size(max = 500, message = "La opción C no puede superar 500 caracteres")
        String opcionC,
        @NotBlank(message = "La opción D es obligatoria")
        @Size(max = 500, message = "La opción D no puede superar 500 caracteres")
        String opcionD,
        @NotBlank(message = "La respuesta correcta es obligatoria")
        @Pattern(regexp = "(?i)[A-D]", message = "La respuesta correcta debe ser A, B, C o D")
        String respuestaCorrecta,
        @NotBlank(message = "La justificación es obligatoria")
        @Size(max = 4000, message = "La justificación no puede superar 4000 caracteres")
        String justificacion,
        @NotBlank(message = "La bibliografía es obligatoria")
        @Size(max = 1000, message = "La bibliografía no puede superar 1000 caracteres")
        String bibliografia,
        @NotNull(message = "La competencia es obligatoria")
        Competencia competencia,
        @NotBlank(message = "El tema es obligatorio")
        @Size(max = 200, message = "El tema no puede superar 200 caracteres")
        String tema,
        @NotBlank(message = "El subtema es obligatorio")
        @Size(max = 200, message = "El subtema no puede superar 200 caracteres")
        String subtema,
        @NotNull(message = "La dificultad es obligatoria")
        Dificultad dificultad) {

    public ContenidoPregunta aContenido() {
        return new ContenidoPregunta(nombre, contexto, enunciado, opcionA, opcionB, opcionC, opcionD,
                respuestaCorrecta, justificacion, bibliografia, competencia, tema, subtema, dificultad);
    }
}
