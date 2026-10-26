package co.unicauca.bancopreguntas.validation.api.dto;

import co.unicauca.bancopreguntas.validation.domain.Competencia;
import co.unicauca.bancopreguntas.validation.domain.ContenidoPregunta;
import co.unicauca.bancopreguntas.validation.domain.Dificultad;

/**
 * El contenido que se quiere revisar. Todos los campos son opcionales a propósito: los que falten o estén vacíos se
 * reportan como violaciones, junto con las demás, en una sola respuesta.
 */
public record ValidationRequest(
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
        Competencia competencia,
        String tema,
        String subtema,
        Dificultad dificultad) {

    public ContenidoPregunta aContenido() {
        return new ContenidoPregunta(nombre, contexto, enunciado, opcionA, opcionB, opcionC, opcionD,
                respuestaCorrecta, justificacion, bibliografia, competencia, tema, subtema, dificultad);
    }
}
