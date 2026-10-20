package co.unicauca.bancopreguntas.tracking.api.dto;

import co.unicauca.bancopreguntas.tracking.domain.ReporteDeGrupo;

import java.time.Instant;
import java.util.List;

/** Reporte para docentes y coordinadores (RF-28). */
public record GroupReportResponse(Instant generadoEn, int estudiantes, int simulacros, double promedioGeneral,
                                  List<CompetencyResponse> porCompetencia, List<StudentRow> estudiantesDetalle) {

    public static GroupReportResponse de(ReporteDeGrupo r) {
        return new GroupReportResponse(r.generadoEn(), r.estudiantes(), r.simulacros(), r.promedioGeneral(),
                r.porCompetencia().stream().map(CompetencyResponse::de).toList(),
                r.detalle().stream().map(f -> new StudentRow(f.estudianteId(), f.simulacros(), f.promedio(),
                        f.mejorPuntaje(), f.ultimoPuntaje(), f.tendencia().name(), f.debilidades())).toList());
    }

    /** Una fila por estudiante, del mejor promedio al peor. */
    public record StudentRow(String estudianteId, int simulacros, double promedio, double mejorPuntaje,
                             double ultimoPuntaje, String tendencia, List<String> debilidades) {
    }
}
