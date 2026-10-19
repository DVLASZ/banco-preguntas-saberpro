package co.unicauca.bancopreguntas.simulation.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Simulacro")
class SimulacroTest {

    private static final Instant T0 = Instant.parse("2026-10-16T10:00:00Z");

    private static Simulacro simulacro() {
        return Simulacro.iniciar("s-1", "estudiante1", Preguntas.criterios(3, 30), Preguntas.lectura(3), T0);
    }

    @Test
    @DisplayName("empieza en curso, con las preguntas numeradas y el tiempo corriendo")
    void inicio() {
        Simulacro s = simulacro();

        assertThat(s.getEstado()).isEqualTo(EstadoSimulacro.EN_CURSO);
        assertThat(s.getItems()).extracting(ItemDeSimulacro::orden).containsExactly(1, 2, 3);
        assertThat(s.getVenceEn()).isEqualTo(T0.plus(Duration.ofMinutes(30)));
        assertThat(s.segundosRestantes(T0.plusSeconds(60))).isEqualTo(1740);
        assertThat(s.respondidas()).isZero();
        assertThat(s.revelaRespuestas()).isFalse();
        assertThat(s.getResultado()).isNull();
    }

    @Test
    @DisplayName("se puede contestar y cambiar la respuesta; cuenta la última")
    void responder() {
        Simulacro s = simulacro();

        s.responder("q-1", 'a', T0.plusSeconds(10));
        s.responder("q-1", 'B', T0.plusSeconds(20));

        assertThat(s.getItems().get(0).respuesta()).isEqualTo('B');
        assertThat(s.respondidas()).isEqualTo(1);
    }

    @Test
    @DisplayName("rechaza una opción inválida y una pregunta que no es del simulacro")
    void respuestasInvalidas() {
        Simulacro s = simulacro();

        assertThatThrownBy(() -> s.responder("q-1", 'E', T0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> s.responder("q-99", 'A', T0)).isInstanceOf(OperacionNoPermitidaException.class);
    }

    @Test
    @DisplayName("el instante exacto del vencimiento todavía cuenta; un segundo después ya no")
    void limiteDelTiempo() {
        Simulacro s = simulacro();

        s.responder("q-1", 'B', s.getVenceEn());
        assertThatThrownBy(() -> s.responder("q-2", 'B', s.getVenceEn().plusSeconds(1))).isInstanceOf(TiempoAgotadoException.class);
        assertThat(s.vencido(s.getVenceEn())).isFalse();
        assertThat(s.vencido(s.getVenceEn().plusSeconds(1))).isTrue();
    }

    @Test
    @DisplayName("al finalizar a tiempo queda FINALIZADO, calificado y con las respuestas reveladas")
    void finalizarATiempo() {
        Simulacro s = simulacro();
        s.responder("q-1", 'B', T0.plusSeconds(10));
        s.responder("q-2", 'A', T0.plusSeconds(20));

        ResultadoDeSimulacro r = s.finalizar(T0.plusSeconds(600));

        assertThat(s.getEstado()).isEqualTo(EstadoSimulacro.FINALIZADO);
        assertThat(s.getFinalizadoEn()).isEqualTo(T0.plusSeconds(600));
        assertThat(r.correctas()).isEqualTo(1);
        assertThat(r.incorrectas()).isEqualTo(1);
        assertThat(r.sinResponder()).isEqualTo(1);
        assertThat(s.getResultado()).isSameAs(r);
        assertThat(s.revelaRespuestas()).isTrue();
        assertThat(s.segundosRestantes(T0.plusSeconds(601))).isZero();
        assertThat(s.segundosUsados(T0.plusSeconds(9999))).isEqualTo(600);
    }

    @Test
    @DisplayName("si el tiempo ya se agotó queda EXPIRADO y se califica con lo contestado antes")
    void expirado() {
        Simulacro s = simulacro();
        s.responder("q-1", 'B', T0.plusSeconds(10));

        ResultadoDeSimulacro r = s.finalizar(T0.plus(Duration.ofHours(3)));

        assertThat(s.getEstado()).isEqualTo(EstadoSimulacro.EXPIRADO);
        assertThat(s.getFinalizadoEn()).isEqualTo(s.getVenceEn());
        assertThat(r.correctas()).isEqualTo(1);
        assertThat(s.segundosUsados(T0.plus(Duration.ofHours(3)))).isEqualTo(1800);
    }

    @Test
    @DisplayName("un simulacro terminado no se contesta ni se finaliza otra vez")
    void terminado() {
        Simulacro s = simulacro();
        s.finalizar(T0.plusSeconds(5));

        assertThatThrownBy(() -> s.responder("q-1", 'A', T0.plusSeconds(6))).isInstanceOf(OperacionNoPermitidaException.class);
        assertThatThrownBy(() -> s.finalizar(T0.plusSeconds(7))).isInstanceOf(OperacionNoPermitidaException.class);
    }

    @Test
    @DisplayName("mientras está en curso los segundos usados llegan hasta ahora, sin pasar del vencimiento")
    void segundosUsados() {
        Simulacro s = simulacro();

        assertThat(s.segundosUsados(T0.plusSeconds(90))).isEqualTo(90);
        assertThat(s.segundosUsados(T0.plus(Duration.ofHours(2)))).isEqualTo(1800);
    }

    @Test
    @DisplayName("necesita identificador, estudiante y al menos una pregunta")
    void invariantes() {
        assertThatThrownBy(() -> Simulacro.iniciar(" ", "e", Preguntas.criterios(1, 5), Preguntas.lectura(1), T0))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Simulacro.iniciar("s", null, Preguntas.criterios(1, 5), Preguntas.lectura(1), T0))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Simulacro.iniciar("s", "e", Preguntas.criterios(1, 5), java.util.List.of(), T0))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("la lista de preguntas que entrega no se puede modificar desde fuera")
    void inmutable() {
        Simulacro s = simulacro();

        assertThatThrownBy(() -> s.getItems().clear()).isInstanceOf(UnsupportedOperationException.class);
    }
}
