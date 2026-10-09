package co.unicauca.saberpro.microkernel.pipeline;

import co.unicauca.saberpro.microkernel.common.entities.QuestionRequest;
import co.unicauca.saberpro.microkernel.pipeline.filters.ContentValidationFilter;
import co.unicauca.saberpro.preguntas.domain.Dificultad;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ContentValidationFilterTest {

    private final ContentValidationFilter filter = new ContentValidationFilter();

    private QuestionRequest solicitud(String titulo, String contenido) {
        return new QuestionRequest(titulo, contenido, "MULTIPLE_CHOICE", "Lectura crítica", "tema",
                Dificultad.BASICO, List.of("a", "b", "c", "d"), "a", null);
    }

    @Test
    void process_aceptaTituloYContenidoValidos() {
        assertTrue(filter.process(solicitud("Título", "Contenido")));
    }

    @Test
    void process_rechazaTituloVacio() {
        assertFalse(filter.process(solicitud("  ", "Contenido")));
    }

    @Test
    void process_rechazaContenidoNulo() {
        assertFalse(filter.process(solicitud("Título", null)));
    }
}
