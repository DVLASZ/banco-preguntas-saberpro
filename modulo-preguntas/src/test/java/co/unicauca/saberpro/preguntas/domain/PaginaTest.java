package co.unicauca.saberpro.preguntas.domain;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Una página de resultados: cuántas páginas hay y si se puede avanzar o retroceder. */
class PaginaTest {

    @Test
    void totalPaginas_redondeaHaciaArriba() {
        assertEquals(3, new Pagina<>(List.of("a"), 1, 5, 11).totalPaginas());
        assertEquals(2, new Pagina<>(List.of("a"), 1, 5, 10).totalPaginas());
        assertEquals(1, new Pagina<>(List.of("a"), 1, 5, 1).totalPaginas());
    }

    @Test
    void sinResultados_sigueHabiendoUnaPagina() {
        Pagina<String> vacia = new Pagina<>(List.of(), 1, 10, 0);

        assertEquals(1, vacia.totalPaginas());
        assertFalse(vacia.haySiguiente());
        assertFalse(vacia.hayAnterior());
    }

    @Test
    void haySiguienteYHayAnterior_segunLaPaginaEnQueEstoy() {
        assertTrue(new Pagina<>(List.of("a"), 1, 5, 12).haySiguiente());
        assertFalse(new Pagina<>(List.of("a"), 1, 5, 12).hayAnterior());
        assertTrue(new Pagina<>(List.of("a"), 2, 5, 12).haySiguiente());
        assertTrue(new Pagina<>(List.of("a"), 2, 5, 12).hayAnterior());
        assertFalse(new Pagina<>(List.of("a"), 3, 5, 12).haySiguiente());
        assertTrue(new Pagina<>(List.of("a"), 3, 5, 12).hayAnterior());
    }

    @Test
    void losElementos_sonUnaCopiaQueNoSePuedeModificar() {
        List<String> original = new ArrayList<>(List.of("a", "b"));
        Pagina<String> pagina = new Pagina<>(original, 1, 5, 2);

        original.add("c");

        assertEquals(List.of("a", "b"), pagina.elementos());
        assertThrows(UnsupportedOperationException.class, () -> pagina.elementos().add("z"));
    }
}
