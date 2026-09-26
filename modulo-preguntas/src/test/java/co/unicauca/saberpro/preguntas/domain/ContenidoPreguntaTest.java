package co.unicauca.saberpro.preguntas.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ContenidoPreguntaTest {

    @Test
    void de_extraeElContenidoEditableDeUnaPregunta() {
        Question pregunta = Question.builder().id("P-001").nombre("Nombre").contexto("Contexto")
                .enunciado("¿Pregunta?").opciones(new QuestionDistractors("Aa1", "Bb2", "Cc3", "Dd4"))
                .respuestaCorrecta('c').justificacion("Justificación").bibliografia("Libro")
                .estado(EstadoPregunta.BORRADOR).competencia(Competencia.INGLES).tema("Tema")
                .subtema("Subtema").dificultad(Dificultad.AVANZADO).autor("autor1").build();

        ContenidoPregunta contenido = ContenidoPregunta.de(pregunta);

        assertEquals("Nombre", contenido.nombre());
        assertEquals("Contexto", contenido.contexto());
        assertEquals("¿Pregunta?", contenido.enunciado());
        assertEquals("Aa1", contenido.opcionA());
        assertEquals("Dd4", contenido.opcionD());
        assertEquals("C", contenido.respuestaCorrecta());
        assertEquals("Justificación", contenido.justificacion());
        assertEquals("Libro", contenido.bibliografia());
        assertEquals(Competencia.INGLES, contenido.competencia());
        assertEquals("Tema", contenido.tema());
        assertEquals("Subtema", contenido.subtema());
        assertEquals(Dificultad.AVANZADO, contenido.dificultad());
    }
}
