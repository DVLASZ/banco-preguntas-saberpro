package co.unicauca.saberpro.microkernel.core;

import co.unicauca.saberpro.microkernel.common.entities.QuestionRequest;
import co.unicauca.saberpro.preguntas.domain.Dificultad;
import co.unicauca.saberpro.preguntas.domain.Question;
import co.unicauca.saberpro.preguntas.domain.QuestionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Prueba el núcleo con los plugins reales, cargados por Reflexión desde el
 * {@code plugins.properties} de {@code src/main/resources} (Maven lo pone
 * también en el classpath de pruebas) — así se verifica el registro y
 * carga dinámica de verdad, no un doble de prueba.
 */
@ExtendWith(MockitoExtension.class)
class QuestionMicrokernelTest {

    @Mock
    private QuestionService questionService;

    private QuestionMicrokernel microkernel;

    @BeforeEach
    void setUp() {
        when(questionService.listarPreguntas()).thenReturn(List.of());
        microkernel = new QuestionMicrokernel(questionService);
    }

    private QuestionRequest solicitudValida() {
        return new QuestionRequest("Pregunta SOLID", "¿Qué representa la S en SOLID?", "MULTIPLE_CHOICE",
                "Lectura crítica", "Principios SOLID", Dificultad.BASICO,
                List.of("Single Responsibility", "Open Closed", "Liskov", "Interface Segregation"),
                "Single Responsibility", null);
    }

    @Test
    void constructor_cargaPorReflexionLosTresPluginsRegistrados() {
        List<String> nombres = microkernel.listarPlugins();

        assertEquals(3, nombres.size());
        assertTrue(nombres.contains("multiple-choice"));
        assertTrue(nombres.contains("case-based"));
        assertTrue(nombres.contains("multimedia"));
    }

    @Test
    void executePlugin_generaLaPreguntaYLaRegistraEnElQuestionService() {
        Question generada = microkernel.executePlugin("MULTIPLE_CHOICE", solicitudValida());

        assertNotNull(generada);
        verify(questionService).registrarPreguntaGenerada(generada);
        assertSame(generada, microkernel.getQuestions().get(generada.getId()));
    }

    @Test
    void executePlugin_retornaNuloSiLaSolicitudNoPasaElPipeline() {
        QuestionRequest sinOpciones = new QuestionRequest("t", "c", "MULTIPLE_CHOICE",
                "Lectura crítica", "tema", Dificultad.BASICO, List.of("a", "b"), "a", null);

        Question resultado = microkernel.executePlugin("MULTIPLE_CHOICE", sinOpciones);

        assertNull(resultado);
        verify(questionService, never()).registrarPreguntaGenerada(any());
    }

    @Test
    void executePlugin_lanzaExcepcionSiNingunPluginSoportaElTipo() {
        assertThrows(IllegalArgumentException.class,
                () -> microkernel.executePlugin("TIPO_INEXISTENTE", solicitudValida()));
    }

    @Test
    void constructor_inicializaElMapaConLasPreguntasYaExistentes() {
        Question existente = new MultipleChoiceQuestionPluginTestHelper().generar();
        when(questionService.listarPreguntas()).thenReturn(List.of(existente));

        QuestionMicrokernel microkernelConDatos = new QuestionMicrokernel(questionService);

        assertEquals(1, microkernelConDatos.getQuestions().size());
        assertSame(existente, microkernelConDatos.getQuestions().get(existente.getId()));
    }

    /** Pequeño ayudante para generar una Question real de prueba sin repetir el armado en cada test. */
    private static class MultipleChoiceQuestionPluginTestHelper {
        Question generar() {
            co.unicauca.saberpro.microkernel.plugins.MultipleChoiceQuestionPlugin plugin =
                    new co.unicauca.saberpro.microkernel.plugins.MultipleChoiceQuestionPlugin();
            return plugin.generate(new QuestionRequest("t", "c", "MULTIPLE_CHOICE", "Lectura crítica", "tema",
                    Dificultad.BASICO, List.of("a", "b", "c", "d"), "a", null));
        }
    }
}
