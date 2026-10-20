package co.unicauca.bancopreguntas.tracking.api.dto;

import co.unicauca.bancopreguntas.tracking.domain.ResultadoRegistrado;

import java.time.Instant;
import java.util.List;

/** Un simulacro terminado dentro del historial de un estudiante. */
public record HistoryEntryResponse(String simulacroId, Instant iniciadoEn, Instant finalizadoEn, long duracionSegundos,
                                   boolean expirado, int total, int correctas, int incorrectas, int sinResponder,
                                   double puntaje, List<CompetencyResponse> porCompetencia) {

    public static HistoryEntryResponse de(ResultadoRegistrado r) {
        return new HistoryEntryResponse(r.simulacroId(), r.iniciadoEn(), r.finalizadoEn(), r.duracionSegundos(),
                r.expirado(), r.total(), r.correctas(), r.incorrectas(), r.sinResponder(), r.puntaje(),
                r.porCompetencia().stream().map(c -> CompetencyResponse.de(
                        co.unicauca.bancopreguntas.tracking.domain.DesempenoPorCompetencia.de(
                                c.competencia(), c.correctas(), c.total()))).toList());
    }
}
