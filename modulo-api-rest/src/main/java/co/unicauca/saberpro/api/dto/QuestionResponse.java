package co.unicauca.saberpro.api.dto;

import co.unicauca.saberpro.preguntas.domain.Competencia;
import co.unicauca.saberpro.preguntas.domain.Dificultad;
import co.unicauca.saberpro.preguntas.domain.EstadoPregunta;

/** Representación JSON de una pregunta. */
public record QuestionResponse(
        String id,
        String nombre,
        String contexto,
        String enunciado,
        String opcionA,
        String opcionB,
        String opcionC,
        String opcionD,
        String respuestaCorrecta,
        String justificacion,
        String bibliografia,
        EstadoPregunta estado,
        Competencia competencia,
        String tema,
        String subtema,
        Dificultad dificultad,
        String autor) {
}
