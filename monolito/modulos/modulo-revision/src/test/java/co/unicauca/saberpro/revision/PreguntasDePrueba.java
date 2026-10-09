package co.unicauca.saberpro.revision;

import co.unicauca.saberpro.preguntas.domain.Competencia;
import co.unicauca.saberpro.preguntas.domain.Dificultad;
import co.unicauca.saberpro.preguntas.domain.EstadoPregunta;
import co.unicauca.saberpro.preguntas.domain.Question;
import co.unicauca.saberpro.preguntas.domain.QuestionDistractors;

/** Preguntas listas para usar en las pruebas de este módulo. */
public final class PreguntasDePrueba {

    private PreguntasDePrueba() {
    }

    /** Una pregunta completa con el id, el estado y el autor dados. */
    public static Question pregunta(String id, EstadoPregunta estado, String autor) {
        return Question.builder()
                .id(id)
                .nombre("Pregunta " + id)
                .contexto("Un equipo de desarrollo debe elegir cómo organizar su código.")
                .enunciado("¿Cuál es la mejor forma de organizarlo?")
                .opciones(new QuestionDistractors("Por capas", "Todo en una clase", "Sin pruebas", "Sin diseño"))
                .respuestaCorrecta('A')
                .justificacion("Separar por capas reduce el acoplamiento entre las partes del sistema.")
                .bibliografia("Bass, L. (2012). Software Architecture in Practice. Addison-Wesley.")
                .estado(estado)
                .competencia(Competencia.LECTURA_CRITICA)
                .tema("Arquitectura")
                .subtema("Arquitectura en capas")
                .dificultad(Dificultad.BASICO)
                .autor(autor)
                .build();
    }
}
