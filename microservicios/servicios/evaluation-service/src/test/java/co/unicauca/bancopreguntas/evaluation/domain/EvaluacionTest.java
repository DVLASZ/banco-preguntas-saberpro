package co.unicauca.bancopreguntas.evaluation.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Evaluacion (objeto de valor)")
class EvaluacionTest {

    private static final Instant AHORA = Instant.parse("2026-11-07T10:00:00Z");

    @Test
    @DisplayName("guarda la decisión, la observación y si llegó tarde")
    void guardaSusDatos() {
        Evaluacion e = new Evaluacion("revisor-1", Decision.RECHAZAR, "La clave es discutible", AHORA, true);

        assertThat(e.revisorId()).isEqualTo("revisor-1");
        assertThat(e.decision()).isEqualTo(Decision.RECHAZAR);
        assertThat(e.observacion()).isEqualTo("La clave es discutible");
        assertThat(e.enviadaEn()).isEqualTo(AHORA);
        assertThat(e.tardia()).isTrue();
    }

    @Test
    @DisplayName("aprobar puede ir sin observación")
    void laObservacionEsOpcional() {
        assertThat(new Evaluacion("revisor-1", Decision.APROBAR, null, AHORA, false).observacion()).isNull();
    }

    @Test
    @DisplayName("exige revisor, decisión y fecha")
    void exigeSusDatos() {
        assertThatThrownBy(() -> new Evaluacion(null, Decision.APROBAR, null, AHORA, false))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Evaluacion(" ", Decision.APROBAR, null, AHORA, false))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Evaluacion("revisor-1", null, null, AHORA, false))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Evaluacion("revisor-1", Decision.APROBAR, null, null, false))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
