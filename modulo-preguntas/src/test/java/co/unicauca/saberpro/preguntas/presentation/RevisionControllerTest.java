package co.unicauca.saberpro.preguntas.presentation;

import co.unicauca.saberpro.preguntas.access.QuestionImplRepository;
import co.unicauca.saberpro.preguntas.domain.EstadoPregunta;
import co.unicauca.saberpro.preguntas.domain.Question;
import co.unicauca.saberpro.preguntas.domain.QuestionService;
import co.unicauca.saberpro.preguntas.domain.TodasLasPreguntasEnRevision;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** El controlador del Revisor, con una vista falsa y el banco de ejemplo (P-003 pendiente, P-004 en revisión). */
class RevisionControllerTest {

    /** Vista sin ventana que recuerda lo que el controlador le pidió. */
    private static final class VistaFalsa implements RevisionVista {
        List<Question> porRevisar = List.of();
        Question mostrada;
        boolean puedeDecidir;
        final List<String> decisiones = new ArrayList<>();
        final List<String> errores = new ArrayList<>();

        @Override
        public void mostrarPreguntasPorRevisar(List<Question> preguntas) {
            porRevisar = preguntas;
        }

        @Override
        public void mostrarPregunta(Question pregunta, boolean puedeDecidir) {
            mostrada = pregunta;
            this.puedeDecidir = puedeDecidir;
        }

        @Override
        public void mostrarDecision(String idPregunta, EstadoPregunta decision) {
            decisiones.add(idPregunta + " -> " + decision);
        }

        @Override
        public void mostrarError(String titulo, String mensaje) {
            errores.add(titulo + ": " + mensaje);
        }
    }

    private QuestionService modelo;
    private VistaFalsa vista;
    private RevisionController controlador;

    @BeforeEach
    void setUp() {
        modelo = new QuestionService(new QuestionImplRepository());
        vista = new VistaFalsa();
        controlador = new RevisionController(modelo, new TodasLasPreguntasEnRevision(modelo), "revisor1", vista);
        controlador.iniciar();
    }

    @Test
    void iniciar_muestraLasPreguntasQueLeCorrespondenAlRevisor() {
        assertEquals(List.of("P-003", "P-004"), vista.porRevisar.stream().map(Question::getId).toList());
    }

    @Test
    void cargarUnaPreguntaEnRevision_dejaDecidir() {
        controlador.cargarPregunta("P-004");

        assertEquals("P-004", vista.mostrada.getId());
        assertTrue(vista.puedeDecidir);
        assertEquals(EstadoPregunta.EN_REVISION, modelo.obtenerPregunta("P-004").getEstado());
    }

    @Test
    void cargarUnaPreguntaPendiente_laPasaAEnRevisionYDejaDecidir() {
        controlador.cargarPregunta("P-003");

        assertEquals(EstadoPregunta.EN_REVISION, modelo.obtenerPregunta("P-003").getEstado());
        assertEquals(EstadoPregunta.EN_REVISION, vista.mostrada.getEstado());
        assertTrue(vista.puedeDecidir);
    }

    @Test
    void cargarUnaPreguntaYaDecidida_noDejaDecidir() {
        controlador.cargarPregunta("P-005");

        assertEquals(EstadoPregunta.APROBADA, vista.mostrada.getEstado());
        assertFalse(vista.puedeDecidir);
    }

    @Test
    void aprobar_registraLaDecisionEnElModelo() {
        controlador.cargarPregunta("P-004");

        controlador.aprobar();

        assertEquals(EstadoPregunta.APROBADA, modelo.obtenerPregunta("P-004").getEstado());
        assertEquals(List.of("P-004 -> " + EstadoPregunta.APROBADA), vista.decisiones);
    }

    @Test
    void rechazar_registraLaDecisionEnElModelo() {
        controlador.cargarPregunta("P-004");

        controlador.rechazar();

        assertEquals(EstadoPregunta.RECHAZADA, modelo.obtenerPregunta("P-004").getEstado());
        assertEquals(1, vista.decisiones.size());
    }

    @Test
    void decidirSinHaberCargadoNada_noHaceNada() {
        controlador.aprobar();

        assertTrue(vista.decisiones.isEmpty());
        assertTrue(vista.errores.isEmpty());
    }

    @Test
    void decidirDosVeces_laSegundaMuestraElError() {
        controlador.cargarPregunta("P-004");
        controlador.aprobar();

        controlador.rechazar();

        assertEquals(1, vista.decisiones.size());
        assertEquals(1, vista.errores.size());
        assertTrue(vista.errores.get(0).startsWith("No se pudo registrar la revisión"));
        assertEquals(EstadoPregunta.APROBADA, modelo.obtenerPregunta("P-004").getEstado());
    }
}
