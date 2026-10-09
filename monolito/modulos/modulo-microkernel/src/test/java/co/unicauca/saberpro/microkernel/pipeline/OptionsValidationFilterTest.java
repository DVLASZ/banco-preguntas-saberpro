package co.unicauca.saberpro.microkernel.pipeline;

import co.unicauca.saberpro.microkernel.common.entities.QuestionRequest;
import co.unicauca.saberpro.microkernel.pipeline.filters.OptionsValidationFilter;
import co.unicauca.saberpro.preguntas.domain.Dificultad;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class OptionsValidationFilterTest {

    private final OptionsValidationFilter filter = new OptionsValidationFilter();

    private QuestionRequest solicitudConOpciones(List<String> opciones) {
        return new QuestionRequest("t", "c", "MULTIPLE_CHOICE", "Lectura crítica", "tema",
                Dificultad.BASICO, opciones, "a", null);
    }

    @Test
    void process_aceptaExactamenteCuatroOpcionesCompletas() {
        assertTrue(filter.process(solicitudConOpciones(List.of("a", "b", "c", "d"))));
    }

    @Test
    void process_rechazaMenosDeCuatroOpciones() {
        assertFalse(filter.process(solicitudConOpciones(List.of("a", "b", "c"))));
    }

    @Test
    void process_rechazaMasDeCuatroOpciones() {
        assertFalse(filter.process(solicitudConOpciones(List.of("a", "b", "c", "d", "e"))));
    }

    @Test
    void process_rechazaUnaOpcionVacia() {
        assertFalse(filter.process(solicitudConOpciones(List.of("a", "", "c", "d"))));
    }

    @Test
    void process_rechazaListaNula() {
        assertFalse(filter.process(solicitudConOpciones(null)));
    }
}
