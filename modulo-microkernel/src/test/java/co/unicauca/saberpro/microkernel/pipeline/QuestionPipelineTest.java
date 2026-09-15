package co.unicauca.saberpro.microkernel.pipeline;

import co.unicauca.saberpro.microkernel.common.entities.QuestionRequest;
import co.unicauca.saberpro.microkernel.pipeline.base.QuestionFilter;
import co.unicauca.saberpro.microkernel.pipeline.base.QuestionPipeline;
import co.unicauca.saberpro.microkernel.pipeline.filters.ContentValidationFilter;
import co.unicauca.saberpro.preguntas.domain.Dificultad;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class QuestionPipelineTest {

    private QuestionRequest solicitudValida() {
        return new QuestionRequest("t", "c", "MULTIPLE_CHOICE", "Lectura crítica", "tema",
                Dificultad.BASICO, List.of("a", "b", "c", "d"), "a", null);
    }

    @Test
    void testContentValidationFilterInvalido() {
        ContentValidationFilter filter = new ContentValidationFilter();
        QuestionRequest requestInvalido = new QuestionRequest("", "", "MULTIPLE_CHOICE", "Arquitectura", "tema",
                Dificultad.BASICO, null, "", null);
        assertFalse(filter.process(requestInvalido));
    }

    @Test
    void execute_retornaTrueSiTodosLosFiltrosPasan() {
        QuestionPipeline pipeline = new QuestionPipeline();
        pipeline.addFilter(request -> true);
        pipeline.addFilter(request -> true);

        assertTrue(pipeline.execute(solicitudValida()));
        assertNull(pipeline.getUltimoFiltroFallido());
    }

    @Test
    void execute_seDetieneEnElPrimerFiltroQueFalla() {
        QuestionFilter siempreFalla = request -> false;
        QuestionFilter noDeberiaEjecutarse = request -> {
            throw new AssertionError("No debería llegar a este filtro");
        };

        QuestionPipeline pipeline = new QuestionPipeline();
        pipeline.addFilter(siempreFalla);
        pipeline.addFilter(noDeberiaEjecutarse);

        assertFalse(pipeline.execute(solicitudValida()));
    }

    @Test
    void execute_recuerdaElNombreDelFiltroQueFallo() {
        QuestionPipeline pipeline = new QuestionPipeline();
        pipeline.addFilter(new ContentValidationFilter());

        QuestionRequest sinTitulo = new QuestionRequest(" ", "c", "MULTIPLE_CHOICE", "Lectura crítica", "tema",
                Dificultad.BASICO, List.of("a", "b", "c", "d"), "a", null);
        pipeline.execute(sinTitulo);

        assertEquals("ContentValidationFilter", pipeline.getUltimoFiltroFallido());
    }
}
