package co.unicauca.bancopreguntas.tracking.domain;

import java.time.Instant;
import java.util.List;

/**
 * Estadísticas individuales de un estudiante (RF-26), calculadas sobre su historial de simulacros.
 *
 * @param ultimoPuntaje   puntaje del simulacro más reciente, o {@code null} si no ha hecho ninguno
 * @param ultimoSimulacro cuándo terminó el más reciente
 */
public record EstadisticasIndividuales(String estudianteId, int simulacros, int expirados, double puntajePromedio,
                                       double mejorPuntaje, double peorPuntaje, Double ultimoPuntaje,
                                       Instant ultimoSimulacro, long tiempoPromedioSegundos, Tendencia tendencia,
                                       List<DesempenoPorCompetencia> porCompetencia) {

    /** Diferencia de puntos entre los simulacros recientes y los anteriores desde la que hay tendencia. */
    static final double CAMBIO_SIGNIFICATIVO = 5.0;
    static final int VENTANA = 3;

    public EstadisticasIndividuales {
        porCompetencia = List.copyOf(porCompetencia);
    }

    public static EstadisticasIndividuales de(String estudianteId, List<ResultadoRegistrado> resultados) {
        List<ResultadoRegistrado> ordenados = Desempeno.porFecha(resultados);
        if (ordenados.isEmpty()) {
            return new EstadisticasIndividuales(estudianteId, 0, 0, 0, 0, 0, null, null, 0, Tendencia.SIN_DATOS, List.of());
        }
        ResultadoRegistrado ultimo = ordenados.get(ordenados.size() - 1);
        return new EstadisticasIndividuales(estudianteId, ordenados.size(),
                (int) ordenados.stream().filter(ResultadoRegistrado::expirado).count(),
                Desempeno.promedio(ordenados),
                ordenados.stream().mapToDouble(ResultadoRegistrado::puntaje).max().orElse(0),
                ordenados.stream().mapToDouble(ResultadoRegistrado::puntaje).min().orElse(0),
                ultimo.puntaje(), ultimo.finalizadoEn(),
                Math.round(ordenados.stream().mapToLong(ResultadoRegistrado::duracionSegundos).average().orElse(0)),
                tendencia(ordenados), Desempeno.porCompetencia(ordenados));
    }

    /**
     * Compara el promedio de los últimos simulacros (hasta 3) con el de los anteriores (hasta 3); hace falta al menos
     * un simulacro en cada grupo, es decir, dos en total.
     */
    static Tendencia tendencia(List<ResultadoRegistrado> ordenados) {
        if (ordenados.size() < 2) {
            return Tendencia.SIN_DATOS;
        }
        int corte = ordenados.size() - Math.min(VENTANA, ordenados.size() - 1);
        List<ResultadoRegistrado> recientes = ordenados.subList(corte, ordenados.size());
        List<ResultadoRegistrado> anteriores = ordenados.subList(Math.max(0, corte - VENTANA), corte);
        double cambio = Desempeno.promedio(recientes) - Desempeno.promedio(anteriores);
        if (cambio >= CAMBIO_SIGNIFICATIVO) {
            return Tendencia.MEJORANDO;
        }
        return cambio <= -CAMBIO_SIGNIFICATIVO ? Tendencia.BAJANDO : Tendencia.ESTABLE;
    }
}
