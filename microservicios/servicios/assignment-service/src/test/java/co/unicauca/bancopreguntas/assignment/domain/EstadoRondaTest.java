package co.unicauca.bancopreguntas.assignment.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("EstadoRonda")
class EstadoRondaTest {

    @Test
    @DisplayName("solo acepta revisores mientras espera la asignación o está en curso")
    void aceptaAsignaciones() {
        assertThat(EstadoRonda.PENDIENTE_ASIGNACION.aceptaAsignaciones()).isTrue();
        assertThat(EstadoRonda.EN_CURSO.aceptaAsignaciones()).isTrue();
        assertThat(EstadoRonda.RECHAZADA.aceptaAsignaciones()).isFalse();
        assertThat(EstadoRonda.APROBADA.aceptaAsignaciones()).isFalse();
        assertThat(EstadoRonda.CERRADA.aceptaAsignaciones()).isFalse();
    }

    @Test
    @DisplayName("está decidida cuando se aprobó, se rechazó o se cerró")
    void decidida() {
        assertThat(EstadoRonda.PENDIENTE_ASIGNACION.decidida()).isFalse();
        assertThat(EstadoRonda.EN_CURSO.decidida()).isFalse();
        assertThat(EstadoRonda.RECHAZADA.decidida()).isTrue();
        assertThat(EstadoRonda.APROBADA.decidida()).isTrue();
        assertThat(EstadoRonda.CERRADA.decidida()).isTrue();
    }
}
