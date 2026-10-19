package co.unicauca.bancopreguntas.tracking.domain;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.ArrayList;

/**
 * Reporte para docentes y coordinadores (RF-28): cómo va el conjunto de estudiantes que han hecho simulacros, con
 * el desempeño del grupo por competencia y una fila por estudiante, del mejor promedio al peor.
 */
public record ReporteDeGrupo(Instant generadoEn, int estudiantes, int simulacros, double promedioGeneral,
                             List<DesempenoPorCompetencia> porCompetencia, List<ResumenDeEstudiante> detalle) {

    public ReporteDeGrupo {
        porCompetencia = List.copyOf(porCompetencia);
        detalle = List.copyOf(detalle);
    }

    public static ReporteDeGrupo de(List<ResultadoRegistrado> todos, Umbrales umbrales, Instant ahora) {
        Map<String, List<ResultadoRegistrado>> porEstudiante = new TreeMap<>();
        todos.forEach(r -> porEstudiante.computeIfAbsent(r.estudianteId(), k -> new ArrayList<>()).add(r));
        List<ResumenDeEstudiante> filas = new ArrayList<>();
        porEstudiante.forEach((id, resultados) -> {
            EstadisticasIndividuales e = EstadisticasIndividuales.de(id, resultados);
            List<String> debilidades = AnalisisDeCompetencias.de(id, resultados, umbrales).debilidades().stream()
                    .map(DesempenoPorCompetencia::competencia).toList();
            filas.add(new ResumenDeEstudiante(id, e.simulacros(), e.puntajePromedio(), e.mejorPuntaje(),
                    e.ultimoPuntaje() == null ? 0 : e.ultimoPuntaje(), e.tendencia(), debilidades));
        });
        filas.sort(Comparator.comparingDouble(ResumenDeEstudiante::promedio).reversed()
                .thenComparing(ResumenDeEstudiante::estudianteId));
        return new ReporteDeGrupo(ahora, porEstudiante.size(), todos.size(), Desempeno.promedio(todos),
                Desempeno.porCompetencia(todos), filas);
    }

    /** El detalle por estudiante en CSV, para abrirlo en una hoja de cálculo. */
    public String aCsv() {
        StringBuilder csv = new StringBuilder("estudiante,simulacros,promedio,mejor,ultimo,tendencia,debilidades\n");
        for (ResumenDeEstudiante f : detalle) {
            csv.append(escapar(f.estudianteId())).append(',').append(f.simulacros()).append(',')
                    .append(numero(f.promedio())).append(',').append(numero(f.mejorPuntaje())).append(',')
                    .append(numero(f.ultimoPuntaje())).append(',').append(f.tendencia()).append(',')
                    .append(escapar(String.join(" | ", f.debilidades()))).append('\n');
        }
        return csv.toString();
    }

    private static String numero(double valor) {
        return String.format(Locale.ROOT, "%.2f", valor);
    }

    private static String escapar(String texto) {
        boolean necesita = texto.contains(",") || texto.contains("\"") || texto.contains("\n");
        return necesita ? "\"" + texto.replace("\"", "\"\"") + "\"" : texto;
    }
}
