package co.unicauca.saberpro.preguntas.domain.validation;

import co.unicauca.saberpro.preguntas.domain.ContenidoPregunta;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Una clase anidada por regla de la validación estructural (HU03). */
class ReglasDeValidacionTest {

    private static Set<String> campos(List<Violacion> violaciones) {
        return violaciones.stream().map(Violacion::campo).collect(Collectors.toSet());
    }

    @Nested
    class CamposObligatorios {
        private final CamposObligatoriosRule regla = new CamposObligatoriosRule();

        @Test
        void contenidoCompletoNoTieneViolaciones() {
            assertTrue(regla.validar(ContenidoDePrueba.valido().build()).isEmpty());
        }

        @Test
        void reportaCadaCampoVacioPorSuNombre() {
            ContenidoPregunta vacio = new ContenidoPregunta(" ", "x", null, "a", "b", "c", "d", "",
                    "", " ", null, "", null, null);

            assertEquals(Set.of("nombre", "enunciado", "respuestaCorrecta", "justificacion", "bibliografia",
                    "tema", "subtema", "competencia", "dificultad"), campos(regla.validar(vacio)));
        }
    }

    @Nested
    class ContextoObligatorio {
        private final ContextoObligatorioRule regla = new ContextoObligatorioRule();

        @Test
        void aceptaUnContexto() {
            assertTrue(regla.validar(ContenidoDePrueba.valido().build()).isEmpty());
        }

        @Test
        void rechazaContextoVacioONulo() {
            assertEquals(Set.of("contexto"), campos(regla.validar(ContenidoDePrueba.valido().contexto("  ").build())));
            assertEquals(Set.of("contexto"), campos(regla.validar(ContenidoDePrueba.valido().contexto(null).build())));
        }
    }

    @Nested
    class PreguntaDirectaUnica {
        private final PreguntaDirectaUnicaRule regla = new PreguntaDirectaUnicaRule();

        @Test
        void aceptaUnaSolaPregunta() {
            assertTrue(regla.validar(ContenidoDePrueba.valido().build()).isEmpty());
        }

        @Test
        void rechazaUnEnunciadoSinSignoDeInterrogacion() {
            List<Violacion> v = regla.validar(ContenidoDePrueba.valido().enunciado("Nombre el patrón de notificación").build());

            assertEquals(Set.of("enunciado"), campos(v));
        }

        @Test
        void rechazaMasDeUnaPregunta() {
            List<Violacion> v = regla.validar(ContenidoDePrueba.valido()
                    .enunciado("¿Qué es un Subject? ¿Y un Observer?").build());

            assertEquals(1, v.size());
            assertTrue(v.get(0).mensaje().contains("única pregunta directa"));
        }

        @Test
        void ignoraUnEnunciadoVacioPorqueDeEsoSeEncargaLaReglaDeCamposObligatorios() {
            assertTrue(regla.validar(ContenidoDePrueba.valido().enunciado("").build()).isEmpty());
        }
    }

    @Nested
    class CuatroOpciones {
        private final CuatroOpcionesRule regla = new CuatroOpcionesRule();

        @Test
        void aceptaCuatroOpcionesDistintas() {
            assertTrue(regla.validar(ContenidoDePrueba.valido().build()).isEmpty());
        }

        @Test
        void rechazaUnaOpcionVaciaONula() {
            assertEquals(Set.of("opcionC"), campos(regla.validar(ContenidoDePrueba.valido().opcionC(" ").build())));
            assertEquals(Set.of("opcionD"), campos(regla.validar(ContenidoDePrueba.valido().opcionD(null).build())));
        }

        @Test
        void rechazaUnaOpcionQueRepiteOtraIgnorandoMayusculasTildesYEspacios() {
            List<Violacion> v = regla.validar(ContenidoDePrueba.valido().opcionA("Diseño  de patrones")
                    .opcionD("diseno de patrones").build());

            assertEquals(Set.of("opcionD"), campos(v));
            assertTrue(v.get(0).mensaje().contains("repite el contenido de la opción A"));
        }
    }

    @Nested
    class RespuestaCorrectaUnica {
        private final RespuestaCorrectaUnicaRule regla = new RespuestaCorrectaUnicaRule();

        @Test
        void aceptaUnaLetraDeAaD() {
            assertTrue(regla.validar(ContenidoDePrueba.valido().respuesta("b").build()).isEmpty());
            assertTrue(regla.validar(ContenidoDePrueba.valido().respuesta(" D ").build()).isEmpty());
        }

        @Test
        void rechazaMasDeUnaLetraOUnaLetraFueraDelRango() {
            assertEquals(Set.of("respuestaCorrecta"), campos(regla.validar(ContenidoDePrueba.valido().respuesta("AB").build())));
            assertEquals(Set.of("respuestaCorrecta"), campos(regla.validar(ContenidoDePrueba.valido().respuesta("E").build())));
        }

        @Test
        void ignoraLaRespuestaVaciaPorqueDeEsoSeEncargaLaReglaDeCamposObligatorios() {
            assertTrue(regla.validar(ContenidoDePrueba.valido().respuesta("").build()).isEmpty());
        }
    }

    @Nested
    class ExpresionesProhibidas {
        private final ExpresionesProhibidasRule regla = new ExpresionesProhibidasRule();

        @Test
        void aceptaOpcionesNormales() {
            assertTrue(regla.validar(ContenidoDePrueba.valido().build()).isEmpty());
        }

        @Test
        void rechazaTodasLasAnterioresEnCualquierEscritura() {
            assertEquals(Set.of("opcionA"), campos(regla.validar(ContenidoDePrueba.valido().opcionA("Todas las anteriores").build())));
            assertEquals(Set.of("opcionB"), campos(regla.validar(ContenidoDePrueba.valido().opcionB("TODAS   LAS ANTERIORES son correctas").build())));
        }

        @Test
        void rechazaNingunaDeLasAnterioresIgnorandoTildes() {
            assertEquals(Set.of("opcionC"), campos(regla.validar(ContenidoDePrueba.valido().opcionC("Ninguna de las anteriores").build())));
            assertEquals(Set.of("opcionD"), campos(regla.validar(ContenidoDePrueba.valido().opcionD("Ningúna de las opciónes").build())));
        }

        @Test
        void rechazaTodasLasOpciones() {
            assertEquals(Set.of("opcionA"), campos(regla.validar(ContenidoDePrueba.valido().opcionA("Todas las opciones").build())));
        }

        @Test
        void unaPalabraSueltaComoTodasNoEsUnaExpresionProhibida() {
            assertTrue(regla.validar(ContenidoDePrueba.valido().opcionA("Todas las clases son objetos").build()).isEmpty());
        }
    }

    @Nested
    class LongitudYEstructura {
        private final LongitudYEstructuraOpcionesRule regla = new LongitudYEstructuraOpcionesRule();

        @Test
        void aceptaOpcionesBienFormadas() {
            assertTrue(regla.validar(ContenidoDePrueba.valido().opcionA("3 capas").build()).isEmpty());
        }

        @Test
        void rechazaOpcionesMuyCortas() {
            assertEquals(Set.of("opcionA"), campos(regla.validar(ContenidoDePrueba.valido().opcionA("Ab").build())));
        }

        @Test
        void rechazaOpcionesMuyLargas() {
            String larga = "A" + "b".repeat(LongitudYEstructuraOpcionesRule.MAXIMO);

            assertEquals(Set.of("opcionB"), campos(regla.validar(ContenidoDePrueba.valido().opcionB(larga).build())));
        }

        @Test
        void rechazaOpcionesQueNoComienzanConMayuscula() {
            assertEquals(Set.of("opcionC"), campos(regla.validar(ContenidoDePrueba.valido().opcionC("singleton").build())));
        }

        @Test
        void rechazaEspaciosRepetidos() {
            assertEquals(Set.of("opcionD"), campos(regla.validar(ContenidoDePrueba.valido().opcionD("Patrón  Adapter").build())));
        }

        @Test
        void ignoraLasOpcionesVaciasPorqueDeEsoSeEncargaOtraRegla() {
            assertTrue(regla.validar(ContenidoDePrueba.valido().opcionA("").opcionB(null).build()).isEmpty());
        }
    }
}
