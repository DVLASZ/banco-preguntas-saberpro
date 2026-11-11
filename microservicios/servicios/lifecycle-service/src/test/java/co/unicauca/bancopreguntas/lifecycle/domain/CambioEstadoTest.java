package co.unicauca.bancopreguntas.lifecycle.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("CambioEstado (entrada del historial)")
class CambioEstadoTest {

    private static final Instant AHORA = Instant.parse("2026-11-07T10:00:00Z");

    @Test
    @DisplayName("guarda de dónde viene, a dónde va, quién lo hizo y cuándo")
    void guardaSusDatos() {
        CambioEstado c = new CambioEstado(EstadoPregunta.BORRADOR, EstadoPregunta.PENDIENTE_REVISION, "autor-1", AHORA);

        assertThat(c.desde()).isEqualTo(EstadoPregunta.BORRADOR);
        assertThat(c.hacia()).isEqualTo(EstadoPregunta.PENDIENTE_REVISION);
        assertThat(c.cambiadoPor()).isEqualTo("autor-1");
        assertThat(c.fecha()).isEqualTo(AHORA);
    }

    @Test
    @DisplayName("la creación no tiene estado anterior, y un evento no tiene responsable")
    void camposOpcionales() {
        CambioEstado c = new CambioEstado(null, EstadoPregunta.BORRADOR, null, AHORA);

        assertThat(c.desde()).isNull();
        assertThat(c.cambiadoPor()).isNull();
    }

    @Test
    @DisplayName("exige el estado de destino y la fecha")
    void exigeDestinoYFecha() {
        assertThatThrownBy(() -> new CambioEstado(null, null, "autor-1", AHORA)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new CambioEstado(null, EstadoPregunta.BORRADOR, "autor-1", null))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
