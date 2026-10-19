package co.unicauca.bancopreguntas.tracking.domain;

import java.util.List;

/**
 * Fortalezas y debilidades de un estudiante por competencia (RF-27).
 *
 * @param fortalezas         competencias con aciertos desde el umbral de fortaleza
 * @param debilidades        competencias con aciertos por debajo del umbral de debilidad, de la peor a la mejor
 * @param intermedias        las que quedan entre los dos umbrales
 * @param sinDatosSuficientes competencias de las que el estudiante ha visto menos preguntas que el mínimo
 */
public record AnalisisDeCompetencias(String estudianteId, List<DesempenoPorCompetencia> fortalezas,
                                     List<DesempenoPorCompetencia> debilidades,
                                     List<DesempenoPorCompetencia> intermedias,
                                     List<DesempenoPorCompetencia> sinDatosSuficientes) {

    public AnalisisDeCompetencias {
        fortalezas = List.copyOf(fortalezas);
        debilidades = List.copyOf(debilidades);
        intermedias = List.copyOf(intermedias);
        sinDatosSuficientes = List.copyOf(sinDatosSuficientes);
    }

    public static AnalisisDeCompetencias de(String estudianteId, List<ResultadoRegistrado> resultados, Umbrales umbrales) {
        return clasificar(estudianteId, Desempeno.porCompetencia(resultados), umbrales);
    }

    static AnalisisDeCompetencias clasificar(String estudianteId, List<DesempenoPorCompetencia> desempeno, Umbrales umbrales) {
        var fortalezas = new java.util.ArrayList<DesempenoPorCompetencia>();
        var debilidades = new java.util.ArrayList<DesempenoPorCompetencia>();
        var intermedias = new java.util.ArrayList<DesempenoPorCompetencia>();
        var pocas = new java.util.ArrayList<DesempenoPorCompetencia>();
        for (DesempenoPorCompetencia d : desempeno) {
            if (d.total() < umbrales.minimoDePreguntas()) {
                pocas.add(d);
            } else if (d.porcentaje() >= umbrales.fortaleza()) {
                fortalezas.add(d);
            } else if (d.porcentaje() < umbrales.debilidad()) {
                debilidades.add(d);
            } else {
                intermedias.add(d);
            }
        }
        fortalezas.sort(java.util.Comparator.comparingDouble(DesempenoPorCompetencia::porcentaje).reversed());
        debilidades.sort(java.util.Comparator.comparingDouble(DesempenoPorCompetencia::porcentaje));
        return new AnalisisDeCompetencias(estudianteId, fortalezas, debilidades, intermedias, pocas);
    }
}
