package co.unicauca.saberpro.preguntas.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** El filtro de búsqueda de preguntas (RF-07): cada criterio por separado y combinados. */
class FiltroPreguntasTest {

    private static Question pregunta() {
        return Question.builder().id("P-1").nombre("Patrón Observer").contexto("Contexto de la pregunta")
                .enunciado("¿Qué notifica el sujeto a sus observadores?")
                .opciones(new QuestionDistractors("Un cambio", "Un error", "Una vista", "Un usuario"))
                .respuestaCorrecta('A').justificacion("Porque el sujeto avisa los cambios de estado")
                .bibliografia("Gamma et al. (1994)").estado(EstadoPregunta.BORRADOR)
                .competencia(Competencia.LECTURA_CRITICA).tema("Patrones de diseño").subtema("Comportamiento")
                .dificultad(Dificultad.BASICO).autor("autor1").build();
    }

    @Test
    void sinFiltros_aceptaCualquierPregunta() {
        assertTrue(FiltroPreguntas.sinFiltros().coincide(pregunta()));
    }

    @Test
    void porEstado() {
        assertTrue(new FiltroPreguntas(EstadoPregunta.BORRADOR, null, null).coincide(pregunta()));
        assertFalse(new FiltroPreguntas(EstadoPregunta.PUBLICADA, null, null).coincide(pregunta()));
    }

    @Test
    void porCompetencia() {
        assertTrue(new FiltroPreguntas(null, Competencia.LECTURA_CRITICA, null).coincide(pregunta()));
        assertFalse(new FiltroPreguntas(null, Competencia.INGLES, null).coincide(pregunta()));
    }

    @Test
    void porTexto_buscaEnNombreEnunciadoTemaYSubtema() {
        assertTrue(new FiltroPreguntas(null, null, "observer").coincide(pregunta()));
        assertTrue(new FiltroPreguntas(null, null, "observadores").coincide(pregunta()));
        assertTrue(new FiltroPreguntas(null, null, "patrones").coincide(pregunta()));
        assertTrue(new FiltroPreguntas(null, null, "comportamiento").coincide(pregunta()));
        assertFalse(new FiltroPreguntas(null, null, "recursion").coincide(pregunta()));
    }

    @Test
    void porTexto_ignoraTildesMayusculasYEspaciosDeLosExtremos() {
        assertTrue(new FiltroPreguntas(null, null, "  PATRON  ").coincide(pregunta()));
        assertTrue(new FiltroPreguntas(null, null, "Diseño").coincide(pregunta()));
        assertTrue(new FiltroPreguntas(null, null, "diseno").coincide(pregunta()));
    }

    @Test
    void textoNuloOEnBlanco_noFiltra() {
        assertTrue(new FiltroPreguntas(null, null, null).coincide(pregunta()));
        assertTrue(new FiltroPreguntas(null, null, "   ").coincide(pregunta()));
    }

    @Test
    void combinados_debenCumplirseTodos() {
        assertTrue(new FiltroPreguntas(EstadoPregunta.BORRADOR, Competencia.LECTURA_CRITICA, "observer")
                .coincide(pregunta()));
        assertFalse(new FiltroPreguntas(EstadoPregunta.BORRADOR, Competencia.LECTURA_CRITICA, "recursion")
                .coincide(pregunta()));
        assertFalse(new FiltroPreguntas(EstadoPregunta.APROBADA, Competencia.LECTURA_CRITICA, "observer")
                .coincide(pregunta()));
    }
}
