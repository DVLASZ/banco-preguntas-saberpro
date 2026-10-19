package co.unicauca.bancopreguntas.tracking.domain;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/** Cálculos comunes de desempeño sobre una lista de resultados. */
final class Desempeno {

    private Desempeno() {
    }

    /** Suma los aciertos por competencia de todos los resultados, en orden alfabético. */
    static List<DesempenoPorCompetencia> porCompetencia(List<ResultadoRegistrado> resultados) {
        Map<String, int[]> acumulado = new TreeMap<>();
        for (ResultadoRegistrado r : resultados) {
            for (ResultadoPorCompetencia c : r.porCompetencia()) {
                int[] cuenta = acumulado.computeIfAbsent(c.competencia(), k -> new int[2]);
                cuenta[0] += c.correctas();
                cuenta[1] += c.total();
            }
        }
        return acumulado.entrySet().stream()
                .map(e -> DesempenoPorCompetencia.de(e.getKey(), e.getValue()[0], e.getValue()[1])).toList();
    }

    static double promedio(List<ResultadoRegistrado> resultados) {
        return resultados.isEmpty() ? 0 : redondear(resultados.stream().mapToDouble(ResultadoRegistrado::puntaje).average().orElse(0));
    }

    static List<ResultadoRegistrado> porFecha(List<ResultadoRegistrado> resultados) {
        return resultados.stream().sorted(Comparator.comparing(ResultadoRegistrado::finalizadoEn)).toList();
    }

    static double redondear(double valor) {
        return Math.round(valor * 100.0) / 100.0;
    }
}
