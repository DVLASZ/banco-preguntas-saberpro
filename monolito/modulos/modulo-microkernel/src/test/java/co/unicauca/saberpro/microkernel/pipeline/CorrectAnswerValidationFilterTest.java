package co.unicauca.saberpro.microkernel.pipeline;

import co.unicauca.saberpro.microkernel.common.entities.QuestionRequest;
import co.unicauca.saberpro.microkernel.pipeline.filters.CorrectAnswerValidationFilter;
import co.unicauca.saberpro.preguntas.domain.Dificultad;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CorrectAnswerValidationFilterTest {

    private final CorrectAnswerValidationFilter filter = new CorrectAnswerValidationFilter();

    private QuestionRequest solicitud(String correcta) {
        return new QuestionRequest("t", "c", "MULTIPLE_CHOICE", "Lectura crítica", "tema",
                Dificultad.BASICO, List.of("a", "b", "c", "d"), correcta, null);
    }

    @Test
    void process_aceptaSiLaRespuestaEstaEntreLasOpciones() {
        assertTrue(filter.process(solicitud("b")));
    }

    @Test
    void process_rechazaSiLaRespuestaNoEstaEntreLasOpciones() {
        assertFalse(filter.process(solicitud("z")));
    }

    @Test
    void process_rechazaRespuestaVacia() {
        assertFalse(filter.process(solicitud(" ")));
    }
}
