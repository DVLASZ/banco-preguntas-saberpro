package co.unicauca.bancopreguntas.tracking.domain;

import java.util.List;

/** Una fila del reporte de grupo: cómo va un estudiante. */
public record ResumenDeEstudiante(String estudianteId, int simulacros, double promedio, double mejorPuntaje,
                                  double ultimoPuntaje, Tendencia tendencia, List<String> debilidades) {

    public ResumenDeEstudiante {
        debilidades = List.copyOf(debilidades);
    }
}
