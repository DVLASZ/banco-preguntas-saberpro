package co.unicauca.bancopreguntas.simulation.domain;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/** Califica automáticamente las respuestas de un simulacro (RF-24): una pregunta sin contestar no suma. */
public final class Calificador {

    private Calificador() {
    }

    public static ResultadoDeSimulacro calificar(List<ItemDeSimulacro> items) {
        int correctas = 0;
        int incorrectas = 0;
        int sinResponder = 0;
        Map<String, int[]> porCompetencia = new TreeMap<>();
        for (ItemDeSimulacro item : items) {
            int[] cuenta = porCompetencia.computeIfAbsent(item.pregunta().competencia(), c -> new int[2]);
            cuenta[1]++;
            if (!item.respondida()) {
                sinResponder++;
            } else if (item.acertada()) {
                correctas++;
                cuenta[0]++;
            } else {
                incorrectas++;
            }
        }
        double puntaje = items.isEmpty() ? 0 : Math.round(correctas * 10000.0 / items.size()) / 100.0;
        List<ResultadoPorCompetencia> detalle = porCompetencia.entrySet().stream()
                .map(e -> new ResultadoPorCompetencia(e.getKey(), e.getValue()[0], e.getValue()[1])).toList();
        return new ResultadoDeSimulacro(items.size(), correctas, incorrectas, sinResponder, puntaje, detalle);
    }
}
