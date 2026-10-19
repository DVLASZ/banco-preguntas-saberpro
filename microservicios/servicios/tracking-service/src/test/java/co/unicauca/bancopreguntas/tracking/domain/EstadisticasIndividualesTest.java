package co.unicauca.bancopreguntas.tracking.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static co.unicauca.bancopreguntas.tracking.domain.Resultados.comp;
import static co.unicauca.bancopreguntas.tracking.domain.Resultados.resultado;
import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("EstadisticasIndividuales (RF-26)")
class EstadisticasIndividualesTest {

    @Test
    @DisplayName("un estudiante sin simulacros tiene estadísticas en cero y sin tendencia")
    void sinSimulacros() {
        EstadisticasIndividuales e = EstadisticasIndividuales.de("est-1", List.of());

        assertThat(e.simulacros()).isZero();
        assertThat(e.puntajePromedio()).isZero();
        assertThat(e.ultimoPuntaje()).isNull();
        assertThat(e.ultimoSimulacro()).isNull();
        assertThat(e.tendencia()).isEqualTo(Tendencia.SIN_DATOS);
        assertThat(e.porCompetencia()).isEmpty();
    }

    @Test
    @DisplayName("calcula promedio, mejor, peor y último sin importar el orden en que llegan")
    void resumen() {
        EstadisticasIndividuales e = EstadisticasIndividuales.de("est-1", List.of(
                resultado("s-2", "est-1", 2, 9, 10),
                resultado("s-1", "est-1", 1, 5, 10),
                resultado("s-3", "est-1", 3, 7, 10)));

        assertThat(e.simulacros()).isEqualTo(3);
        assertThat(e.puntajePromedio()).isEqualTo(70.0);
        assertThat(e.mejorPuntaje()).isEqualTo(90.0);
        assertThat(e.peorPuntaje()).isEqualTo(50.0);
        assertThat(e.ultimoPuntaje()).isEqualTo(70.0);
        assertThat(e.ultimoSimulacro()).isEqualTo(Resultados.T0.plusSeconds(3 * 86400L));
        assertThat(e.tiempoPromedioSegundos()).isEqualTo(600);
    }

    @Test
    @DisplayName("cuenta los simulacros que se cerraron por tiempo agotado")
    void expirados() {
        ResultadoRegistrado expirado = new ResultadoRegistrado("s-2", "est-1", null, Resultados.T0.plusSeconds(100), 1800, true,
                10, 2, 3, 5, 20.0, null);

        EstadisticasIndividuales e = EstadisticasIndividuales.de("est-1", List.of(resultado("s-1", "est-1", 0, 5, 10), expirado));

        assertThat(e.expirados()).isEqualTo(1);
        assertThat(e.tiempoPromedioSegundos()).isEqualTo(1200);
    }

    @Test
    @DisplayName("acumula los aciertos por competencia de todos los simulacros")
    void porCompetencia() {
        EstadisticasIndividuales e = EstadisticasIndividuales.de("est-1", List.of(
                resultado("s-1", "est-1", 1, 3, 6, comp("LECTURA_CRITICA", 2, 3), comp("INGLES", 1, 3)),
                resultado("s-2", "est-1", 2, 5, 6, comp("LECTURA_CRITICA", 3, 3), comp("INGLES", 2, 3))));

        assertThat(e.porCompetencia()).containsExactly(
                new DesempenoPorCompetencia("INGLES", 3, 6, 50.0),
                new DesempenoPorCompetencia("LECTURA_CRITICA", 5, 6, 83.33));
    }

    @Test
    @DisplayName("con un solo simulacro todavía no hay tendencia")
    void unSolo() {
        assertThat(EstadisticasIndividuales.de("e", List.of(resultado("s-1", "e", 1, 5, 10))).tendencia()).isEqualTo(Tendencia.SIN_DATOS);
    }

    @Test
    @DisplayName("mejora si los simulacros recientes puntúan 5 puntos o más sobre los anteriores")
    void mejorando() {
        List<ResultadoRegistrado> r = List.of(resultado("s-1", "e", 1, 4, 10), resultado("s-2", "e", 2, 5, 10), resultado("s-3", "e", 3, 7, 10));

        assertThat(EstadisticasIndividuales.de("e", r).tendencia()).isEqualTo(Tendencia.MEJORANDO);
    }

    @Test
    @DisplayName("baja si los simulacros recientes puntúan 5 puntos o más por debajo de los anteriores")
    void bajando() {
        List<ResultadoRegistrado> r = List.of(resultado("s-1", "e", 1, 9, 10), resultado("s-2", "e", 2, 8, 10), resultado("s-3", "e", 3, 5, 10));

        assertThat(EstadisticasIndividuales.de("e", r).tendencia()).isEqualTo(Tendencia.BAJANDO);
    }

    @Test
    @DisplayName("es estable si el cambio es menor de 5 puntos")
    void estable() {
        List<ResultadoRegistrado> r = List.of(resultado("s-1", "e", 1, 6, 10), resultado("s-2", "e", 2, 6, 10), resultado("s-3", "e", 3, 6, 10));

        assertThat(EstadisticasIndividuales.de("e", r).tendencia()).isEqualTo(Tendencia.ESTABLE);
    }

    @Test
    @DisplayName("solo se comparan los 3 simulacros más recientes con los 3 anteriores")
    void ventana() {
        // los dos primeros fueron muy malos, pero ya no cuentan: los 3 anteriores a los últimos 3 son iguales a ellos
        List<ResultadoRegistrado> r = List.of(resultado("s-1", "e", 1, 0, 10), resultado("s-2", "e", 2, 0, 10),
                resultado("s-3", "e", 3, 6, 10), resultado("s-4", "e", 4, 6, 10), resultado("s-5", "e", 5, 6, 10),
                resultado("s-6", "e", 6, 6, 10), resultado("s-7", "e", 7, 6, 10), resultado("s-8", "e", 8, 6, 10));

        assertThat(EstadisticasIndividuales.de("e", r).tendencia()).isEqualTo(Tendencia.ESTABLE);
    }

    @Test
    @DisplayName("la lista de competencias no se modifica desde fuera")
    void inmutable() {
        EstadisticasIndividuales e = EstadisticasIndividuales.de("e", List.of(resultado("s-1", "e", 1, 1, 2, comp("INGLES", 1, 2))));

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> e.porCompetencia().clear())
                .isInstanceOf(UnsupportedOperationException.class);
    }
}
