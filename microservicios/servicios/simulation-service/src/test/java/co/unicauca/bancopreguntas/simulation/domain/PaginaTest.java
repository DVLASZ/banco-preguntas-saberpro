package co.unicauca.bancopreguntas.simulation.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Pagina y resumen del catálogo")
class PaginaTest {

    @Test
    @DisplayName("nunca tiene menos de una página y no se modifica desde fuera")
    void pagina() {
        Pagina<String> vacia = new Pagina<>(List.of(), 1, 10, 0);
        Pagina<String> trece = new Pagina<>(List.of("a"), 2, 10, 13);

        assertThat(vacia.totalPaginas()).isEqualTo(1);
        assertThat(trece.totalPaginas()).isEqualTo(2);
        assertThatThrownBy(() -> trece.elementos().add("b")).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    @DisplayName("el resumen del catálogo y el resultado por competencia son inmutables")
    void inmutables() {
        ResumenDelCatalogo resumen = new ResumenDelCatalogo(2, java.util.Map.of("INGLES", 2), java.util.Map.of("BASICO", 2));

        assertThat(resumen.total()).isEqualTo(2);
        assertThatThrownBy(() -> resumen.porCompetencia().put("X", 1)).isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> new ResultadoDeSimulacro(1, 1, 0, 0, 100, List.of()).porCompetencia().add(null))
                .isInstanceOf(UnsupportedOperationException.class);
    }
}
