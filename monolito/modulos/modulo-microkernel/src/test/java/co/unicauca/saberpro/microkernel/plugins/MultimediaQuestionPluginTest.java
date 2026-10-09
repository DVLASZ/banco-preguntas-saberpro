package co.unicauca.saberpro.microkernel.plugins;

import co.unicauca.saberpro.microkernel.common.entities.QuestionRequest;
import co.unicauca.saberpro.preguntas.domain.Dificultad;
import co.unicauca.saberpro.preguntas.domain.Question;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MultimediaQuestionPluginTest {

    private final MultimediaQuestionPlugin plugin = new MultimediaQuestionPlugin();

    @Test
    void supports_soloElTipoMultimedia() {
        assertTrue(plugin.supports("MULTIMEDIA"));
        assertFalse(plugin.supports("CASO"));
    }

    @Test
    void generate_incluyeElRecursoMultimediaEnElEnunciado() {
        QuestionRequest solicitud = new QuestionRequest("t", "Observe el siguiente diagrama", "MULTIMEDIA",
                "Lectura crítica", "tema", Dificultad.AVANZADO,
                List.of("a", "b", "c", "d"), "a", "https://ejemplo.com/diagrama.png");

        Question pregunta = plugin.generate(solicitud);

        assertNotNull(pregunta);
        assertTrue(pregunta.getEnunciado().contains("Observe el siguiente diagrama"));
        assertTrue(pregunta.getEnunciado().contains("https://ejemplo.com/diagrama.png"));
    }

    @Test
    void generate_usaTextoPorDefectoSiNoHayRecurso() {
        QuestionRequest solicitud = new QuestionRequest("t", "Observe el recurso", "MULTIMEDIA",
                "Lectura crítica", "tema", Dificultad.AVANZADO,
                List.of("a", "b", "c", "d"), "a", null);

        Question pregunta = plugin.generate(solicitud);

        assertNotNull(pregunta);
        assertTrue(pregunta.getEnunciado().contains("sin recurso adjunto"));
    }
}
