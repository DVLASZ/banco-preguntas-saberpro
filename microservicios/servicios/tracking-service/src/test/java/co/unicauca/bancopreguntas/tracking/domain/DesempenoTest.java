package co.unicauca.bancopreguntas.tracking.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static co.unicauca.bancopreguntas.tracking.domain.Resultados.comp;
import static co.unicauca.bancopreguntas.tracking.domain.Resultados.resultado;
import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Desempeno")
class DesempenoTest {

    @Test
    @DisplayName("suma los aciertos de cada competencia entre todos los resultados, en orden alfabético")
    void porCompetencia() {
        List<ResultadoRegistrado> resultados = List.of(
                resultado("s1", "e1", 1, 6, 10, comp("RAZONAMIENTO", 2, 5), comp("LECTURA", 4, 5)),
                resultado("s2", "e1", 2, 8, 10, comp("RAZONAMIENTO", 4, 5), comp("LECTURA", 4, 5)));

        List<DesempenoPorCompetencia> r = Desempeno.porCompetencia(resultados);

        assertThat(r).extracting(DesempenoPorCompetencia::competencia).containsExactly("LECTURA", "RAZONAMIENTO");
        assertThat(r.get(0).porcentaje()).isEqualTo(80.0);
        assertThat(r.get(1).porcentaje()).isEqualTo(60.0);
    }

    @Test
    @DisplayName("sin resultados no hay competencias")
    void sinResultados() {
        assertThat(Desempeno.porCompetencia(List.of())).isEmpty();
    }

    @Test
    @DisplayName("el promedio de los puntajes se redondea a dos decimales")
    void promedio() {
        List<ResultadoRegistrado> resultados = List.of(
                resultado("s1", "e1", 1, 1, 3), resultado("s2", "e1", 2, 2, 3), resultado("s3", "e1", 3, 3, 3));

        assertThat(Desempeno.promedio(resultados)).isEqualTo(66.67);
    }

    @Test
    @DisplayName("sin resultados el promedio es cero")
    void promedioVacio() {
        assertThat(Desempeno.promedio(List.of())).isZero();
    }

    @Test
    @DisplayName("ordena los resultados por fecha de finalización, del más viejo al más nuevo")
    void porFecha() {
        ResultadoRegistrado tarde = resultado("tarde", "e1", 5, 5, 10);
        ResultadoRegistrado temprano = resultado("temprano", "e1", 1, 5, 10);

        assertThat(Desempeno.porFecha(List.of(tarde, temprano))).containsExactly(temprano, tarde);
    }

    @Test
    @DisplayName("redondea a dos decimales")
    void redondear() {
        assertThat(Desempeno.redondear(3.14159)).isEqualTo(3.14);
        assertThat(Desempeno.redondear(7.0)).isEqualTo(7.0);
    }
}
