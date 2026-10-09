package co.unicauca.saberpro.microkernel.plugins;

import co.unicauca.saberpro.microkernel.common.entities.QuestionRequest;
import co.unicauca.saberpro.preguntas.domain.Competencia;
import co.unicauca.saberpro.preguntas.domain.Dificultad;
import co.unicauca.saberpro.preguntas.domain.EstadoPregunta;
import co.unicauca.saberpro.preguntas.domain.Question;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * El esqueleto común de los plugins (Template Method): valida con el pipeline y
 * construye la pregunta; cada plugin solo aporta cómo se arma el enunciado.
 */
class BaseQuestionPluginTest {

    private final BaseQuestionPlugin plugin = new BaseQuestionPlugin() {
        @Override
        protected String construirEnunciado(QuestionRequest request) {
            return "Enunciado de prueba: " + request.getContent();
        }

        @Override
        public String getName() {
            return "de-prueba";
        }

        @Override
        public boolean supports(String type) {
            return "PRUEBA".equals(type);
        }
    };

    private static QuestionRequest solicitud(List<String> opciones, String correcta) {
        return new QuestionRequest("Título", "¿Qué es un plugin?", "PRUEBA", "Lectura crítica",
                "Arquitectura", Dificultad.INTERMEDIO, opciones, correcta, null);
    }

    @Test
    void generate_conSolicitudValida_armaLaPreguntaConElEnunciadoDelPlugin() {
        QuestionRequest solicitud = solicitud(List.of("Una extensión", "Un error", "Una tabla", "Un color"), "Un error");

        Question pregunta = plugin.generate(solicitud);

        assertNotNull(pregunta);
        assertEquals("Enunciado de prueba: ¿Qué es un plugin?", pregunta.getEnunciado());
        assertEquals('B', pregunta.getRespuestaCorrecta());
        assertEquals(EstadoPregunta.PENDIENTE_REVISION, pregunta.getEstado());
        assertEquals(Competencia.LECTURA_CRITICA, pregunta.getCompetencia());
        assertEquals(Dificultad.INTERMEDIO, pregunta.getDificultad());
        assertNull(plugin.getUltimoFiltroFallido());
    }

    @Test
    void generate_cadaLlamadaCreaUnaPreguntaConIdDistinto() {
        QuestionRequest solicitud = solicitud(List.of("a1", "b2", "c3", "d4"), "a1");

        assertNotEquals(plugin.generate(solicitud).getId(), plugin.generate(solicitud).getId());
    }

    @Test
    void generate_siLaSolicitudNoPasaLosFiltros_devuelveNuloYDiceCualFalló() {
        QuestionRequest sinRespuestaEntreLasOpciones =
                solicitud(List.of("Una extensión", "Un error", "Una tabla", "Un color"), "Otra cosa");

        assertNull(plugin.generate(sinRespuestaEntreLasOpciones));
        assertNotNull(plugin.getUltimoFiltroFallido());
    }
}
