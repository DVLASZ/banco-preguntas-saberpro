package co.unicauca.saberpro.microkernel.plugins;

import co.unicauca.saberpro.microkernel.common.entities.QuestionRequest;
import co.unicauca.saberpro.preguntas.domain.Competencia;
import co.unicauca.saberpro.preguntas.domain.Dificultad;
import co.unicauca.saberpro.preguntas.domain.EstadoPregunta;
import co.unicauca.saberpro.preguntas.domain.Question;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MultipleChoiceQuestionPluginTest {

    private final MultipleChoiceQuestionPlugin plugin = new MultipleChoiceQuestionPlugin();

    private QuestionRequest solicitudValida() {
        return new QuestionRequest("Pregunta SOLID", "¿Qué representa la S en SOLID?", "MULTIPLE_CHOICE",
                "Lectura crítica", "Principios SOLID", Dificultad.BASICO,
                List.of("Single Responsibility", "Open Closed", "Liskov", "Interface Segregation"),
                "Single Responsibility", null);
    }

    @Test
    void getName_esMultipleChoice() {
        assertEquals("multiple-choice", plugin.getName());
    }

    @Test
    void supports_soloElTipoMultipleChoice() {
        assertTrue(plugin.supports("MULTIPLE_CHOICE"));
        assertTrue(plugin.supports("multiple_choice"));
        assertFalse(plugin.supports("CASO"));
    }

    @Test
    void generate_conSolicitudValida_construyeLaPreguntaRealDelBanco() {
        Question pregunta = plugin.generate(solicitudValida());

        assertNotNull(pregunta);
        assertEquals("Pregunta SOLID", pregunta.getNombre());
        assertEquals("¿Qué representa la S en SOLID?", pregunta.getEnunciado());
        assertEquals('A', pregunta.getRespuestaCorrecta());
        assertEquals(EstadoPregunta.PENDIENTE_REVISION, pregunta.getEstado());
        assertEquals(Competencia.LECTURA_CRITICA, pregunta.getCompetencia());
        assertEquals("Principios SOLID", pregunta.getTema());
        assertEquals(Dificultad.BASICO, pregunta.getDificultad());
    }

    @Test
    void generate_retornaNuloSiNoPasaElPipeline() {
        QuestionRequest sinTitulo = new QuestionRequest(" ", "contenido", "MULTIPLE_CHOICE",
                "Lectura crítica", "tema", Dificultad.BASICO,
                List.of("a", "b", "c", "d"), "a", null);

        assertNull(plugin.generate(sinTitulo));
        assertEquals("ContentValidationFilter", plugin.getUltimoFiltroFallido());
    }

    @Test
    void generate_calculaLaLetraSegunLaPosicionDeLaRespuestaCorrecta() {
        QuestionRequest solicitud = new QuestionRequest("t", "c", "MULTIPLE_CHOICE", "Lectura crítica", "tema",
                Dificultad.BASICO, List.of("a", "b", "c", "d"), "c", null);

        Question pregunta = plugin.generate(solicitud);

        assertNotNull(pregunta);
        assertEquals('C', pregunta.getRespuestaCorrecta());
    }
}
