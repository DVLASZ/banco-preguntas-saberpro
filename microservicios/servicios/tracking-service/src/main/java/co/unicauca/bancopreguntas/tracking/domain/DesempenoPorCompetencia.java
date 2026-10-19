package co.unicauca.bancopreguntas.tracking.domain;

/**
 * Desempeño acumulado en una competencia.
 *
 * @param porcentaje aciertos sobre las preguntas que se le mostraron, de 0 a 100 con dos decimales
 */
public record DesempenoPorCompetencia(String competencia, int correctas, int total, double porcentaje) {

    public static DesempenoPorCompetencia de(String competencia, int correctas, int total) {
        double porcentaje = total == 0 ? 0 : Math.round(correctas * 10000.0 / total) / 100.0;
        return new DesempenoPorCompetencia(competencia, correctas, total, porcentaje);
    }
}
