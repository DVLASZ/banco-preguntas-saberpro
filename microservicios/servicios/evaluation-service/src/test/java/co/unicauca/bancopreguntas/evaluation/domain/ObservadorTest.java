package co.unicauca.bancopreguntas.evaluation.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Observador (objeto de valor)")
class ObservadorTest {

    @Test
    @DisplayName("sabe si es administrador")
    void guardaSusDatos() {
        assertThat(new Observador("u-1", true).administrador()).isTrue();
        assertThat(new Observador("u-1", false).id()).isEqualTo("u-1");
    }

    @Test
    @DisplayName("exige un identificador")
    void exigeIdentificador() {
        assertThatThrownBy(() -> new Observador(null, false)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Observador("  ", true)).isInstanceOf(IllegalArgumentException.class);
    }
}
