package co.unicauca.saberpro.microkernel.plugins;

import co.unicauca.saberpro.microkernel.common.entities.QuestionRequest;
import co.unicauca.saberpro.preguntas.domain.Dificultad;
import co.unicauca.saberpro.preguntas.domain.Question;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CaseQuestionPluginTest {

    private final CaseQuestionPlugin plugin = new CaseQuestionPlugin();

    @Test
    void supports_soloElTipoCaso() {
        assertTrue(plugin.supports("CASO"));
        assertFalse(plugin.supports("MULTIPLE_CHOICE"));
    }

    @Test
    void generate_anteponeCasoDeEstudioAlEnunciado() {
        QuestionRequest solicitud = new QuestionRequest("t", "Analice la siguiente arquitectura", "CASO",
                "Lectura crítica", "tema", Dificultad.INTERMEDIO,
                List.of("a", "b", "c", "d"), "a", null);

        Question pregunta = plugin.generate(solicitud);

        assertNotNull(pregunta);
        assertEquals("Caso de estudio: Analice la siguiente arquitectura", pregunta.getEnunciado());
    }
}
