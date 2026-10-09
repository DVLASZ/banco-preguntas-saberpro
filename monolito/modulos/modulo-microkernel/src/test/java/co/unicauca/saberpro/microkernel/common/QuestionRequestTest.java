package co.unicauca.saberpro.microkernel.common;

import co.unicauca.saberpro.microkernel.common.entities.QuestionRequest;
import co.unicauca.saberpro.preguntas.domain.Dificultad;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** La solicitud que se le entrega a un plugin para generar una pregunta. */
class QuestionRequestTest {

    @Test
    void guardaTodosSusDatos() {
        List<String> opciones = List.of("a", "b", "c", "d");

        QuestionRequest solicitud = new QuestionRequest("Título", "Contenido", "MULTIPLE_CHOICE",
                "Inglés", "Gramática", Dificultad.AVANZADO, opciones, "c", "imagen.png");

        assertEquals("Título", solicitud.getTitle());
        assertEquals("Contenido", solicitud.getContent());
        assertEquals("MULTIPLE_CHOICE", solicitud.getType());
        assertEquals("Inglés", solicitud.getClassification());
        assertEquals("Gramática", solicitud.getTema());
        assertEquals(Dificultad.AVANZADO, solicitud.getDificultad());
        assertEquals(opciones, solicitud.getOptions());
        assertEquals("c", solicitud.getCorrectAnswer());
        assertEquals("imagen.png", solicitud.getRecursoMultimedia());
    }

    @Test
    void elRecursoMultimedia_esOpcional() {
        QuestionRequest solicitud = new QuestionRequest("T", "C", "CASO", "Inglés", "Tema",
                Dificultad.BASICO, List.of("a", "b", "c", "d"), "a", null);

        assertNull(solicitud.getRecursoMultimedia());
    }
}
