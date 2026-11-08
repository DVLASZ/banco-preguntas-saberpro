package co.unicauca.bancopreguntas.evaluation.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Asignacion (objeto de valor)")
class AsignacionTest {

    private static final Instant AHORA = Instant.parse("2026-11-07T10:00:00Z");

    @Test
    @DisplayName("guarda quién revisa, quién lo asignó y cuándo")
    void guardaSusDatos() {
        Asignacion a = new Asignacion("revisor-1", "admin-1", AHORA, false);

        assertThat(a.revisorId()).isEqualTo("revisor-1");
        assertThat(a.asignadoPor()).isEqualTo("admin-1");
        assertThat(a.asignadoEn()).isEqualTo(AHORA);
        assertThat(a.automatica()).isFalse();
    }

    @Test
    @DisplayName("una reasignación del sistema se marca con el asignador system")
    void asignacionAutomatica() {
        Asignacion a = new Asignacion("revisor-1", Asignacion.SISTEMA, AHORA, true);

        assertThat(a.asignadoPor()).isEqualTo("system");
        assertThat(a.automatica()).isTrue();
    }

    @Test
    @DisplayName("exige revisor, asignador y fecha")
    void exigeSusDatos() {
        assertThatThrownBy(() -> new Asignacion(null, "admin-1", AHORA, false)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Asignacion(" ", "admin-1", AHORA, false)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Asignacion("revisor-1", null, AHORA, false)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Asignacion("revisor-1", " ", AHORA, false)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Asignacion("revisor-1", "admin-1", null, false)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("dos asignaciones con los mismos datos son iguales")
    void seComparaPorSusDatos() {
        assertThat(new Asignacion("r", "a", AHORA, false)).isEqualTo(new Asignacion("r", "a", AHORA, false));
    }
}
