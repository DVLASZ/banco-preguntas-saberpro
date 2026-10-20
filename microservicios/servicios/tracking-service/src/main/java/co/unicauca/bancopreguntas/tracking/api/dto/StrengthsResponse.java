package co.unicauca.bancopreguntas.tracking.api.dto;

import co.unicauca.bancopreguntas.tracking.domain.AnalisisDeCompetencias;
import co.unicauca.bancopreguntas.tracking.domain.Umbrales;

import java.util.List;

/** Fortalezas y debilidades de un estudiante por competencia (RF-27), con los umbrales usados. */
public record StrengthsResponse(String estudianteId, double umbralFortaleza, double umbralDebilidad,
                                int minimoDePreguntas, List<CompetencyResponse> fortalezas,
                                List<CompetencyResponse> debilidades, List<CompetencyResponse> intermedias,
                                List<CompetencyResponse> sinDatosSuficientes) {

    public static StrengthsResponse de(AnalisisDeCompetencias a, Umbrales u) {
        return new StrengthsResponse(a.estudianteId(), u.fortaleza(), u.debilidad(), u.minimoDePreguntas(),
                lista(a.fortalezas()), lista(a.debilidades()), lista(a.intermedias()), lista(a.sinDatosSuficientes()));
    }

    private static List<CompetencyResponse> lista(List<co.unicauca.bancopreguntas.tracking.domain.DesempenoPorCompetencia> l) {
        return l.stream().map(CompetencyResponse::de).toList();
    }
}
