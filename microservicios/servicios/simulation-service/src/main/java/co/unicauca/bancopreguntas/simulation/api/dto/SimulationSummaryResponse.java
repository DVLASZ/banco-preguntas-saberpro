package co.unicauca.bancopreguntas.simulation.api.dto;

import co.unicauca.bancopreguntas.simulation.domain.Simulacro;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;

/** Una fila de la lista de simulacros: sin las preguntas. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record SimulationSummaryResponse(String id, String estado, Instant iniciadoEn, Instant finalizadoEn,
                                        int totalPreguntas, long respondidas, int duracionMinutos, Double puntaje) {

    public static SimulationSummaryResponse de(Simulacro s) {
        return new SimulationSummaryResponse(s.getId(), s.getEstado().name(), s.getIniciadoEn(), s.getFinalizadoEn(),
                s.getItems().size(), s.respondidas(), s.getCriterios().duracionMinutos(),
                s.getResultado() == null ? null : s.getResultado().puntaje());
    }
}
