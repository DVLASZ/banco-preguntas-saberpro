package co.unicauca.bancopreguntas.question.domain.validation;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Utilidades de texto que comparten las reglas de validación. */
class TextoTest {

    @Test
    void vacioReconoceNuloEnBlancoYTexto() {
        assertTrue(Texto.vacio(null));
        assertTrue(Texto.vacio(""));
        assertTrue(Texto.vacio("   \t"));
        assertFalse(Texto.vacio("algo"));
    }

    @Test
    void normalizarQuitaTildesMayusculasYEspaciosSobrantes() {
        assertEquals("todas las anteriores", Texto.normalizar("  TODAS   las  Anteriores "));
        assertEquals("opcion", Texto.normalizar("Opción"));
        assertEquals("", Texto.normalizar(null));
    }

    @Test
    void letrasYCamposDeLasOpciones() {
        assertEquals('A', Texto.letra(0));
        assertEquals('D', Texto.letra(3));
        assertEquals("opcionA", Texto.campoOpcion(0));
        assertEquals("opcionC", Texto.campoOpcion(2));
    }

    @Test
    void opcionesDevuelveLasCuatroEnOrdenYTrataElNuloComoVacio() {
        List<String> opciones = Texto.opciones(ContenidoDePrueba.valido().opcionA(null).opcionB("b").opcionC("c").opcionD("d").build());

        assertEquals(List.of("", "b", "c", "d"), opciones);
    }
}
