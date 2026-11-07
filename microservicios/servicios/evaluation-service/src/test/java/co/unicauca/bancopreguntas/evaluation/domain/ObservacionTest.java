package co.unicauca.bancopreguntas.evaluation.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Observacion (objeto de valor)")
class ObservacionTest {

    @Test
    @DisplayName("guarda el texto sin espacios sobrantes")
    void recortaElTexto() {
        assertThat(new Observacion("  Falta un distractor  ").texto()).isEqualTo("Falta un distractor");
    }

    @Test
    @DisplayName("dos observaciones con el mismo texto son iguales")
    void seCompararPorSuTexto() {
        assertThat(new Observacion("Buena pregunta")).isEqualTo(new Observacion(" Buena pregunta "));
    }

    @Test
    @DisplayName("no puede estar vacía ni en blanco")
    void noPuedeEstarVacia() {
        assertThatThrownBy(() -> new Observacion(null)).isInstanceOf(RevisionInvalidaException.class);
        assertThatThrownBy(() -> new Observacion("")).isInstanceOf(RevisionInvalidaException.class);
        assertThatThrownBy(() -> new Observacion("   ")).isInstanceOf(RevisionInvalidaException.class);
    }

    @Test
    @DisplayName("acepta hasta el máximo y rechaza una más")
    void tieneUnMaximo() {
        assertThat(new Observacion("x".repeat(Observacion.MAXIMO)).texto()).hasSize(Observacion.MAXIMO);
        assertThatThrownBy(() -> new Observacion("x".repeat(Observacion.MAXIMO + 1)))
                .isInstanceOf(RevisionInvalidaException.class)
                .hasMessageContaining(String.valueOf(Observacion.MAXIMO));
    }

    @Test
    @DisplayName("de(texto): un texto vacío significa que no hay observación")
    void sinTextoNoHayObservacion() {
        assertThat(Observacion.de(null)).isEmpty();
        assertThat(Observacion.de("  ")).isEmpty();
        assertThat(Observacion.de(" Revisar la clave ")).contains(new Observacion("Revisar la clave"));
    }
}
