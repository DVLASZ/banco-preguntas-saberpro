package co.unicauca.saberpro.preguntas.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class QuestionTest {

    private final QuestionDistractors opciones = new QuestionDistractors("a", "b", "c", "d");

    private Question preguntaValida(char respuesta, EstadoPregunta estado) {
        return new Question("P-001", "n", "e", opciones, respuesta, estado,
                Competencia.LECTURA_CRITICA, "tema", Dificultad.BASICO);
    }

    @Test
    void constructor_normalizaLaRespuestaCorrectaAMayuscula() {
        Question pregunta = preguntaValida('b', EstadoPregunta.BORRADOR);

        assertEquals('B', pregunta.getRespuestaCorrecta());
    }

    @Test
    void constructor_rechazaRespuestaCorrectaInvalida() {
        assertThrows(IllegalArgumentException.class,
                () -> preguntaValida('Z', EstadoPregunta.BORRADOR));
    }

    @Test
    void constructor_rechazaIdVacio() {
        assertThrows(IllegalArgumentException.class,
                () -> new Question(" ", "n", "e", opciones, 'A', EstadoPregunta.BORRADOR,
                        Competencia.LECTURA_CRITICA, "tema", Dificultad.BASICO));
    }

    @Test
    void constructor_rechazaCompetenciaNula() {
        assertThrows(IllegalArgumentException.class,
                () -> new Question("P-001", "n", "e", opciones, 'A', EstadoPregunta.BORRADOR,
                        null, "tema", Dificultad.BASICO));
    }

    @Test
    void constructor_rechazaTemaVacio() {
        assertThrows(IllegalArgumentException.class,
                () -> new Question("P-001", "n", "e", opciones, 'A', EstadoPregunta.BORRADOR,
                        Competencia.LECTURA_CRITICA, " ", Dificultad.BASICO));
    }

    @Test
    void constructor_rechazaDificultadNula() {
        assertThrows(IllegalArgumentException.class,
                () -> new Question("P-001", "n", "e", opciones, 'A', EstadoPregunta.BORRADOR,
                        Competencia.LECTURA_CRITICA, "tema", null));
    }

    @Test
    void constructor_guardaCompetenciaTemaYDificultad() {
        Question pregunta = preguntaValida('A', EstadoPregunta.BORRADOR);

        assertEquals(Competencia.LECTURA_CRITICA, pregunta.getCompetencia());
        assertEquals("tema", pregunta.getTema());
        assertEquals(Dificultad.BASICO, pregunta.getDificultad());
    }

    @Test
    void setEstado_cambiaElEstadoActual() {
        Question pregunta = preguntaValida('A', EstadoPregunta.BORRADOR);

        pregunta.setEstado(EstadoPregunta.PUBLICADA);

        assertEquals(EstadoPregunta.PUBLICADA, pregunta.getEstado());
    }

    @Test
    void setEstado_rechazaEstadoNulo() {
        Question pregunta = preguntaValida('A', EstadoPregunta.BORRADOR);

        assertThrows(IllegalArgumentException.class, () -> pregunta.setEstado(null));
    }
}
