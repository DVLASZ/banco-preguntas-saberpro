package co.unicauca.bancopreguntas.tracking.api.dto;

import co.unicauca.bancopreguntas.tracking.domain.EstadisticasIndividuales;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.List;

/** Estadísticas individuales de un estudiante (RF-26). */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record StatsResponse(String estudianteId, int simulacros, int expirados, double puntajePromedio,
                            double mejorPuntaje, double peorPuntaje, Double ultimoPuntaje, Instant ultimoSimulacro,
                            long tiempoPromedioSegundos, String tendencia, List<CompetencyResponse> porCompetencia) {

    public static StatsResponse de(EstadisticasIndividuales e) {
        return new StatsResponse(e.estudianteId(), e.simulacros(), e.expirados(), e.puntajePromedio(), e.mejorPuntaje(),
                e.peorPuntaje(), e.ultimoPuntaje(), e.ultimoSimulacro(), e.tiempoPromedioSegundos(), e.tendencia().name(),
                e.porCompetencia().stream().map(CompetencyResponse::de).toList());
    }
}
