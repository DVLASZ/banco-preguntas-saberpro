package co.unicauca.bancopreguntas.simulation.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("CriteriosDeSimulacro")
class CriteriosDeSimulacroTest {

    @Test
    @DisplayName("sin filtros acepta cualquier pregunta")
    void sinFiltros() {
        assertThat(Preguntas.criterios(5, 30).acepta(Preguntas.pregunta("q-1", "INGLES", "AVANZADO", "Verbos", 'A'))).isTrue();
    }

    @Test
    @DisplayName("filtra por competencia, dificultad y tema sin importar mayúsculas ni tildes")
    void filtros() {
        CriteriosDeSimulacro c = new CriteriosDeSimulacro(Set.of("lectura_critica"), Set.of("IDEA PRINCIPAL"),
                Set.of("basico"), 5, 30);

        assertThat(c.acepta(Preguntas.pregunta("q-1", "LECTURA_CRITICA", "BASICO", "Idea principal", 'A'))).isTrue();
        assertThat(c.acepta(Preguntas.pregunta("q-2", "INGLES", "BASICO", "Idea principal", 'A'))).isFalse();
        assertThat(c.acepta(Preguntas.pregunta("q-3", "LECTURA_CRITICA", "AVANZADO", "Idea principal", 'A'))).isFalse();
        assertThat(c.acepta(Preguntas.pregunta("q-4", "LECTURA_CRITICA", "BASICO", "Inferencias", 'A'))).isFalse();
    }

    @Test
    @DisplayName("el tema también puede coincidir con el subtema")
    void temaPorSubtema() {
        CriteriosDeSimulacro c = new CriteriosDeSimulacro(null, Set.of("subtema q-1"), null, 1, 10);

        assertThat(c.acepta(Preguntas.pregunta("q-1"))).isTrue();
        assertThat(c.acepta(Preguntas.pregunta("q-2"))).isFalse();
    }

    @Test
    @DisplayName("el tema se compara sin tildes")
    void temaSinTildes() {
        CriteriosDeSimulacro c = new CriteriosDeSimulacro(null, Set.of("Inferéncias"), null, 1, 10);

        assertThat(c.acepta(Preguntas.pregunta("q-1", "LECTURA_CRITICA", "BASICO", "inferencias", 'A'))).isTrue();
    }

    @Test
    @DisplayName("ignora los valores vacíos de los conjuntos")
    void ignoraVacios() {
        CriteriosDeSimulacro c = new CriteriosDeSimulacro(Set.of(" ", "ingles"), null, Set.of(""), 1, 10);

        assertThat(c.competencias()).containsExactly("INGLES");
        assertThat(c.dificultades()).isEmpty();
        assertThat(c.temas()).isEmpty();
    }

    @Test
    @DisplayName("la cantidad debe ser al menos 1")
    void cantidad() {
        assertThatThrownBy(() -> Preguntas.criterios(0, 30)).isInstanceOf(CriteriosInvalidosException.class);
    }

    @Test
    @DisplayName("la duración debe estar entre 1 y 240 minutos")
    void duracion() {
        assertThatThrownBy(() -> Preguntas.criterios(5, 0)).isInstanceOf(CriteriosInvalidosException.class);
        assertThatThrownBy(() -> Preguntas.criterios(5, 241)).isInstanceOf(CriteriosInvalidosException.class);
        assertThat(Preguntas.criterios(5, 240).duracionMinutos()).isEqualTo(240);
    }
}
