package co.unicauca.saberpro.microkernel.pipeline;

import co.unicauca.saberpro.microkernel.common.entities.QuestionRequest;
import co.unicauca.saberpro.microkernel.pipeline.filters.ClassificationFilter;
import co.unicauca.saberpro.preguntas.domain.Dificultad;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ClassificationFilterTest {

    private final ClassificationFilter filter = new ClassificationFilter();

    private QuestionRequest solicitudConClasificacion(String clasificacion) {
        return new QuestionRequest("t", "c", "MULTIPLE_CHOICE", clasificacion, "tema",
                Dificultad.BASICO, List.of("a", "b", "c", "d"), "a", null);
    }

    @Test
    void process_aceptaUnaCompetenciaRealDelBanco() {
        assertTrue(filter.process(solicitudConClasificacion("Lectura crítica")));
    }

    @Test
    void process_esInsensibleAMayusculasYEspacios() {
        assertTrue(filter.process(solicitudConClasificacion("  lectura CRÍTICA  ")));
    }

    @Test
    void process_rechazaUnaClasificacionInventada() {
        assertFalse(filter.process(solicitudConClasificacion("Matemáticas avanzadas")));
    }

    @Test
    void process_rechazaClasificacionVacia() {
        assertFalse(filter.process(solicitudConClasificacion(" ")));
    }
}
