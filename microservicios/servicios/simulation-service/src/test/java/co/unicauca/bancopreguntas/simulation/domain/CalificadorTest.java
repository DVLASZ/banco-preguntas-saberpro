package co.unicauca.bancopreguntas.simulation.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Calificador")
class CalificadorTest {

    private static ItemDeSimulacro item(int orden, String competencia, char correcta, Character respuesta) {
        return new ItemDeSimulacro(orden, Preguntas.pregunta("q-" + orden, competencia, "BASICO", "Tema", correcta), respuesta);
    }

    @Test
    @DisplayName("cuenta aciertos, errores y preguntas sin responder")
    void conteos() {
        ResultadoDeSimulacro r = Calificador.calificar(List.of(
                item(1, "LECTURA_CRITICA", 'A', 'A'),
                item(2, "LECTURA_CRITICA", 'B', 'C'),
                item(3, "INGLES", 'D', null),
                item(4, "INGLES", 'B', 'B')));

        assertThat(r.total()).isEqualTo(4);
        assertThat(r.correctas()).isEqualTo(2);
        assertThat(r.incorrectas()).isEqualTo(1);
        assertThat(r.sinResponder()).isEqualTo(1);
        assertThat(r.puntaje()).isEqualTo(50.0);
    }

    @Test
    @DisplayName("el puntaje es el porcentaje de aciertos sobre todas las preguntas, con dos decimales")
    void puntaje() {
        ResultadoDeSimulacro r = Calificador.calificar(List.of(
                item(1, "INGLES", 'A', 'A'), item(2, "INGLES", 'A', 'B'), item(3, "INGLES", 'A', 'B')));

        assertThat(r.puntaje()).isEqualTo(33.33);
    }

    @Test
    @DisplayName("agrupa por competencia en orden alfabético")
    void porCompetencia() {
        ResultadoDeSimulacro r = Calificador.calificar(List.of(
                item(1, "LECTURA_CRITICA", 'A', 'A'),
                item(2, "INGLES", 'A', 'B'),
                item(3, "LECTURA_CRITICA", 'A', 'B'),
                item(4, "INGLES", 'A', 'A')));

        assertThat(r.porCompetencia()).containsExactly(
                new ResultadoPorCompetencia("INGLES", 1, 2), new ResultadoPorCompetencia("LECTURA_CRITICA", 1, 2));
    }

    @Test
    @DisplayName("todo correcto da 100 y nada contestado da 0")
    void extremos() {
        assertThat(Calificador.calificar(List.of(item(1, "INGLES", 'C', 'C'))).puntaje()).isEqualTo(100.0);
        assertThat(Calificador.calificar(List.of(item(1, "INGLES", 'C', null))).puntaje()).isZero();
    }

    @Test
    @DisplayName("sin preguntas el puntaje es 0")
    void vacio() {
        assertThat(Calificador.calificar(List.of()).puntaje()).isZero();
    }

    @Test
    @DisplayName("la letra de la respuesta no distingue mayúsculas")
    void mayusculas() {
        ItemDeSimulacro i = new ItemDeSimulacro(1, Preguntas.pregunta("q-1", "INGLES", "BASICO", "t", 'B'), null).respondidoCon('b');

        assertThat(i.acertada()).isTrue();
        assertThat(i.respondida()).isTrue();
    }
}
