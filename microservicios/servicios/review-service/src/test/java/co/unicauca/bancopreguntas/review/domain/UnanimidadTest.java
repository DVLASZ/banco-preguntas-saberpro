package co.unicauca.bancopreguntas.review.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Política de decisión: unanimidad")
class UnanimidadTest {

    private static final Instant T = Instant.parse("2026-10-08T10:00:00Z");
    private final PoliticaDeDecision politica = new Unanimidad();

    private static Evaluacion eval(String revisor, Decision decision) {
        return new Evaluacion(revisor, decision, decision == Decision.RECHAZAR ? "obs" : null, T, false);
    }

    @Test
    @DisplayName("sin evaluaciones sigue pendiente")
    void sinEvaluaciones() {
        assertThat(politica.decidir(List.of("r1", "r2"), List.of())).isEqualTo(ResultadoDeDecision.PENDIENTE);
    }

    @Test
    @DisplayName("faltan evaluaciones y todas las recibidas aprueban: pendiente")
    void faltanAprobaciones() {
        assertThat(politica.decidir(List.of("r1", "r2", "r3"), List.of(eval("r1", Decision.APROBAR))))
                .isEqualTo(ResultadoDeDecision.PENDIENTE);
    }

    @Test
    @DisplayName("todos aprueban: aprobada")
    void todosAprueban() {
        assertThat(politica.decidir(List.of("r1", "r2"),
                List.of(eval("r1", Decision.APROBAR), eval("r2", Decision.APROBAR))))
                .isEqualTo(ResultadoDeDecision.APROBADA);
    }

    @Test
    @DisplayName("un solo rechazo basta, aunque falten los demás")
    void unRechazo() {
        assertThat(politica.decidir(List.of("r1", "r2", "r3"), List.of(eval("r2", Decision.RECHAZAR))))
                .isEqualTo(ResultadoDeDecision.RECHAZADA);
    }

    @Test
    @DisplayName("un rechazo entre aprobaciones: rechazada")
    void rechazoEntreAprobaciones() {
        assertThat(politica.decidir(List.of("r1", "r2"),
                List.of(eval("r1", Decision.APROBAR), eval("r2", Decision.RECHAZAR))))
                .isEqualTo(ResultadoDeDecision.RECHAZADA);
    }

    @Test
    @DisplayName("con un único revisor decide su evaluación")
    void unRevisor() {
        assertThat(politica.decidir(List.of("r1"), List.of(eval("r1", Decision.APROBAR))))
                .isEqualTo(ResultadoDeDecision.APROBADA);
        assertThat(politica.decidir(List.of("r1"), List.of(eval("r1", Decision.RECHAZAR))))
                .isEqualTo(ResultadoDeDecision.RECHAZADA);
    }
}
