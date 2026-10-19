package co.unicauca.bancopreguntas.tracking.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static co.unicauca.bancopreguntas.tracking.domain.Resultados.comp;
import static co.unicauca.bancopreguntas.tracking.domain.Resultados.resultado;
import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("AnalisisDeCompetencias (RF-27)")
class AnalisisDeCompetenciasTest {

    private static final Umbrales UMBRALES = new Umbrales(70, 50, 5);

    @Test
    @DisplayName("separa fortalezas, debilidades, intermedias y las que tienen pocas preguntas")
    void clasifica() {
        List<ResultadoRegistrado> resultados = List.of(resultado("s-1", "e", 1, 14, 28,
                comp("LECTURA_CRITICA", 9, 10),        // 90 %: fortaleza
                comp("RAZONAMIENTO_CUANTITATIVO", 2, 8), // 25 %: debilidad
                comp("COMPETENCIAS_CIUDADANAS", 6, 10),  // 60 %: intermedia
                comp("INGLES", 1, 2)));                  // pocas preguntas

        AnalisisDeCompetencias a = AnalisisDeCompetencias.de("e", resultados, UMBRALES);

        assertThat(a.fortalezas()).extracting(DesempenoPorCompetencia::competencia).containsExactly("LECTURA_CRITICA");
        assertThat(a.debilidades()).extracting(DesempenoPorCompetencia::competencia).containsExactly("RAZONAMIENTO_CUANTITATIVO");
        assertThat(a.intermedias()).extracting(DesempenoPorCompetencia::competencia).containsExactly("COMPETENCIAS_CIUDADANAS");
        assertThat(a.sinDatosSuficientes()).extracting(DesempenoPorCompetencia::competencia).containsExactly("INGLES");
    }

    @Test
    @DisplayName("el límite de fortaleza cuenta como fortaleza y el de debilidad todavía no es debilidad")
    void limites() {
        List<ResultadoRegistrado> resultados = List.of(resultado("s-1", "e", 1, 12, 20,
                comp("LECTURA_CRITICA", 7, 10), comp("INGLES", 5, 10)));

        AnalisisDeCompetencias a = AnalisisDeCompetencias.de("e", resultados, UMBRALES);

        assertThat(a.fortalezas()).extracting(DesempenoPorCompetencia::competencia).containsExactly("LECTURA_CRITICA");
        assertThat(a.debilidades()).isEmpty();
        assertThat(a.intermedias()).extracting(DesempenoPorCompetencia::competencia).containsExactly("INGLES");
    }

    @Test
    @DisplayName("ordena las fortalezas de la mejor a la peor y las debilidades de la peor a la mejor")
    void orden() {
        List<ResultadoRegistrado> resultados = List.of(resultado("s-1", "e", 1, 10, 40,
                comp("A", 8, 10), comp("B", 10, 10), comp("C", 3, 10), comp("D", 1, 10)));

        AnalisisDeCompetencias a = AnalisisDeCompetencias.de("e", resultados, UMBRALES);

        assertThat(a.fortalezas()).extracting(DesempenoPorCompetencia::competencia).containsExactly("B", "A");
        assertThat(a.debilidades()).extracting(DesempenoPorCompetencia::competencia).containsExactly("D", "C");
    }

    @Test
    @DisplayName("acumula varios simulacros antes de clasificar")
    void acumula() {
        List<ResultadoRegistrado> resultados = List.of(
                resultado("s-1", "e", 1, 1, 3, comp("INGLES", 1, 3)),
                resultado("s-2", "e", 2, 3, 3, comp("INGLES", 3, 3)));

        AnalisisDeCompetencias a = AnalisisDeCompetencias.de("e", resultados, UMBRALES);

        assertThat(a.intermedias()).singleElement().satisfies(d -> {
            assertThat(d.total()).isEqualTo(6);
            assertThat(d.porcentaje()).isEqualTo(66.67);
        });
        assertThat(a.fortalezas()).isEmpty();
    }

    @Test
    @DisplayName("sin resultados no hay nada que clasificar")
    void vacio() {
        AnalisisDeCompetencias a = AnalisisDeCompetencias.de("e", List.of(), UMBRALES);

        assertThat(a.fortalezas()).isEmpty();
        assertThat(a.debilidades()).isEmpty();
        assertThat(a.intermedias()).isEmpty();
        assertThat(a.sinDatosSuficientes()).isEmpty();
    }
}
