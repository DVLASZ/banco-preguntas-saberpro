package co.unicauca.bancopreguntas.validation.domain;

/**
 * Datos de una pregunta tal como los diligencia el Autor (o llegan por la
 * API): todo son textos crudos, sin validar. Este servicio no guarda preguntas; solo
 * revisa un contenido y reporta <b>todos</b> los campos inválidos de una vez.
 */
public record ContenidoPregunta(
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
}
