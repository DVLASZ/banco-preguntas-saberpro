package co.unicauca.bancopreguntas.simulation.api.dto;

import co.unicauca.bancopreguntas.simulation.application.VistaDeSimulacro;
import co.unicauca.bancopreguntas.simulation.domain.Simulacro;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.List;

/** Un simulacro completo, con sus preguntas. {@code resultado} aparece cuando ya fue calificado. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record SimulationResponse(String id, String estudianteId, String estado, Instant iniciadoEn, Instant venceEn,
                                 Instant finalizadoEn, long segundosRestantes, int duracionMinutos, int totalPreguntas,
                                 long respondidas, List<SimulationItemResponse> preguntas, ResultResponse resultado) {

    public static SimulationResponse de(VistaDeSimulacro vista) {
        Simulacro s = vista.simulacro();
        boolean revelar = s.revelaRespuestas();
        return new SimulationResponse(s.getId(), s.getEstudianteId(), s.getEstado().name(), s.getIniciadoEn(),
                s.getVenceEn(), s.getFinalizadoEn(), vista.segundosRestantes(), s.getCriterios().duracionMinutos(),
                s.getItems().size(), s.respondidas(),
                s.getItems().stream().map(i -> SimulationItemResponse.de(i, revelar)).toList(),
                s.getResultado() == null ? null : ResultResponse.de(s.getResultado()));
    }
}
