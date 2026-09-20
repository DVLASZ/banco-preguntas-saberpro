package co.unicauca.saberpro.preguntas.presentation;

import co.unicauca.saberpro.preguntas.access.QuestionImplRepository;
import co.unicauca.saberpro.preguntas.domain.Competencia;
import co.unicauca.saberpro.preguntas.domain.EstadoPregunta;
import co.unicauca.saberpro.preguntas.domain.Pagina;
import co.unicauca.saberpro.preguntas.domain.Question;
import co.unicauca.saberpro.preguntas.domain.QuestionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** El controlador del listado, probado con una vista falsa y el banco de ejemplo (autor1 tiene 6 preguntas). */
class MisPreguntasControllerTest {

    private final List<Pagina<Question>> mostradas = new ArrayList<>();
    private MisPreguntasController controlador;

    @BeforeEach
    void setUp() {
        QuestionService servicio = new QuestionService(new QuestionImplRepository());
        controlador = new MisPreguntasController(servicio, "autor1", mostradas::add);
        controlador.iniciar();
    }

    private Pagina<Question> ultima() {
        return mostradas.get(mostradas.size() - 1);
    }

    @Test
    void iniciar_muestraLaPrimeraPaginaSinFiltros() {
        assertEquals(1, mostradas.size());
        assertEquals(1, ultima().numero());
        assertEquals(MisPreguntasController.TAMANO_INICIAL, ultima().elementos().size());
        assertEquals(6, ultima().totalElementos());
    }

    @Test
    void siguienteYAnterior_recorrenLasPaginas() {
        controlador.paginaSiguiente();
        assertEquals(2, ultima().numero());
        assertEquals(1, ultima().elementos().size());

        controlador.paginaAnterior();
        assertEquals(1, ultima().numero());
    }

    @Test
    void siguienteEnLaUltimaPaginaYAnteriorEnLaPrimera_noHacenNada() {
        controlador.paginaAnterior();
        assertEquals(1, mostradas.size());

        controlador.paginaSiguiente();
        int vistas = mostradas.size();
        controlador.paginaSiguiente();
        assertEquals(vistas, mostradas.size());
        assertEquals(2, ultima().numero());
    }

    @Test
    void filtrar_vuelveALaPrimeraPaginaConLosResultadosDelFiltro() {
        controlador.paginaSiguiente();

        controlador.filtrar(EstadoPregunta.BORRADOR, null, "");

        assertEquals(1, ultima().numero());
        assertEquals(2, ultima().totalElementos());
        assertTrue(ultima().elementos().stream().allMatch(p -> p.getEstado() == EstadoPregunta.BORRADOR));
    }

    @Test
    void filtrarPorCompetenciaYTexto() {
        controlador.filtrar(null, Competencia.LECTURA_CRITICA, "solid");

        assertEquals(List.of("P-006"), ultima().elementos().stream().map(Question::getId).toList());
    }

    @Test
    void limpiarFiltros_devuelveTodasLasPreguntas() {
        controlador.filtrar(EstadoPregunta.BORRADOR, null, null);

        controlador.limpiarFiltros();

        assertEquals(6, ultima().totalElementos());
        assertEquals(EstadoPregunta.BORRADOR, ultima().elementos().get(0).getEstado());
    }

    @Test
    void cambiarTamano_recalculaLasPaginas() {
        controlador.cambiarTamanoDePagina(2);

        assertEquals(1, ultima().numero());
        assertEquals(2, ultima().elementos().size());
        assertEquals(3, ultima().totalPaginas());
    }

    @Test
    void refrescar_conservaLaPaginaYElFiltro() {
        controlador.filtrar(null, null, "patron");
        controlador.refrescar();

        assertEquals(2, ultima().totalElementos());
        assertEquals("patron", controlador.filtroActual().texto());
    }
}
