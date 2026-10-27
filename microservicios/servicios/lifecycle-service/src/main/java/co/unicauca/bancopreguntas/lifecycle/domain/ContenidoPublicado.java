package co.unicauca.bancopreguntas.lifecycle.domain;

/**
 * El contenido de la pregunta que se publica, tal como lo envía el servicio de preguntas. Este servicio no lo guarda:
 * solo lo pasa al evento {@code question.published}.
 */
public record ContenidoPublicado(String titulo, String competencia, String tema, String subtema, String dificultad,
                                 String contexto, String enunciado, String opcionA, String opcionB, String opcionC,
                                 String opcionD, String respuestaCorrecta) {
}
