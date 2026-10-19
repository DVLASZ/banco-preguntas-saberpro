package co.unicauca.bancopreguntas.tracking.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("ResultadoRegistrado, ResultadoPorCompetencia, Umbrales y Pagina")
class ResultadoRegistradoTest {

    private static ResultadoRegistrado con(String id, String estudiante, Instant fin, int total, int correctas) {
        return new ResultadoRegistrado(id, estudiante, null, fin, 60, false, total, correctas, 0, 0, 50, null);
    }

    @Test
    @DisplayName("exige simulacro, estudiante y fecha de fin")
    void obligatorios() {
        Instant fin = Resultados.T0;

        assertThatThrownBy(() -> con(" ", "e", fin, 10, 5)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> con("s", null, fin, 10, 5)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> con("s", "e", null, 10, 5)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("los aciertos deben estar entre 0 y el total, y debe haber al menos una pregunta")
    void aciertos() {
        assertThatThrownBy(() -> con("s", "e", Resultados.T0, 0, 0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> con("s", "e", Resultados.T0, 5, 6)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> con("s", "e", Resultados.T0, 5, -1)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("sin detalle por competencia queda una lista vacía e inmutable")
    void listaInmutable() {
        ResultadoRegistrado r = con("s", "e", Resultados.T0, 5, 5);

        assertThat(r.porCompetencia()).isEmpty();
        assertThatThrownBy(() -> r.porCompetencia().add(Resultados.comp("INGLES", 1, 1))).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    @DisplayName("el resultado por competencia valida sus datos")
    void porCompetencia() {
        assertThatThrownBy(() -> new ResultadoPorCompetencia(" ", 1, 2)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ResultadoPorCompetencia("INGLES", 3, 2)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ResultadoPorCompetencia("INGLES", -1, 2)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("los umbrales deben ser coherentes")
    void umbrales() {
        assertThat(Umbrales.POR_DEFECTO.fortaleza()).isEqualTo(70);
        assertThatThrownBy(() -> new Umbrales(40, 50, 5)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Umbrales(101, 50, 5)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Umbrales(70, -1, 5)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Umbrales(70, 50, 0)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("el desempeño calcula el porcentaje con dos decimales y tolera cero preguntas")
    void desempeno() {
        assertThat(DesempenoPorCompetencia.de("INGLES", 1, 3).porcentaje()).isEqualTo(33.33);
        assertThat(DesempenoPorCompetencia.de("INGLES", 0, 0).porcentaje()).isZero();
    }

    @Test
    @DisplayName("la página nunca tiene menos de una página y no se modifica desde fuera")
    void pagina() {
        Pagina<String> vacia = new Pagina<>(List.of(), 1, 10, 0);
        Pagina<String> trece = new Pagina<>(List.of("a"), 2, 10, 13);

        assertThat(vacia.totalPaginas()).isEqualTo(1);
        assertThat(trece.totalPaginas()).isEqualTo(2);
        assertThatThrownBy(() -> trece.elementos().add("b")).isInstanceOf(UnsupportedOperationException.class);
    }
}
