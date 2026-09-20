package co.unicauca.saberpro.preguntas.domain;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

import static co.unicauca.saberpro.preguntas.domain.EstadoPregunta.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EstadoPreguntaTest {

    /** Transiciones válidas de RF-15 (sin contar archivar, que es válido desde cualquier estado). */
    private static final Map<EstadoPregunta, Set<EstadoPregunta>> VALIDAS = new EnumMap<>(EstadoPregunta.class);

    static {
        VALIDAS.put(BORRADOR, EnumSet.of(PENDIENTE_REVISION));
        VALIDAS.put(PENDIENTE_REVISION, EnumSet.of(EN_REVISION));
        VALIDAS.put(EN_REVISION, EnumSet.of(APROBADA, RECHAZADA));
        VALIDAS.put(APROBADA, EnumSet.of(PUBLICADA));
        VALIDAS.put(RECHAZADA, EnumSet.of(BORRADOR));
        VALIDAS.put(PUBLICADA, EnumSet.noneOf(EstadoPregunta.class));
        VALIDAS.put(ARCHIVADA, EnumSet.noneOf(EstadoPregunta.class));
    }

    @Test
    void soloSePermitenLasTransicionesDelCicloDeVida() {
        for (EstadoPregunta origen : EstadoPregunta.values()) {
            for (EstadoPregunta destino : EstadoPregunta.values()) {
                boolean esperado = destino == ARCHIVADA || VALIDAS.get(origen).contains(destino);
                assertEquals(esperado, origen.puedePasarA(destino), origen + " -> " + destino);
            }
        }
    }

    @ParameterizedTest
    @EnumSource(EstadoPregunta.class)
    void cualquierPreguntaPuedeArchivarseNuncaEliminarse(EstadoPregunta origen) {
        assertTrue(origen.puedePasarA(ARCHIVADA));
    }

    @Test
    void unaPreguntaArchivadaOPublicadaNoVuelveAAtras() {
        assertFalse(ARCHIVADA.puedePasarA(BORRADOR));
        assertFalse(PUBLICADA.puedePasarA(EN_REVISION));
    }

    @Test
    void unBorradorNoSaltaDirectoAPublicada() {
        assertFalse(BORRADOR.puedePasarA(PUBLICADA));
        assertFalse(BORRADOR.puedePasarA(APROBADA));
    }

    @Test
    void unDestinoNuloNuncaEsValido() {
        assertFalse(BORRADOR.puedePasarA(null));
    }

    @Test
    void laEtiquetaEsElNombreLegibleDelEstado() {
        assertEquals("Pendiente de revisión", PENDIENTE_REVISION.toString());
    }
}
