package co.unicauca.bancopreguntas.tracking.domain;

/**
 * Cuándo una competencia se considera fortaleza o debilidad (RF-27).
 *
 * @param fortaleza        porcentaje de aciertos desde el que es una fortaleza
 * @param debilidad        porcentaje de aciertos por debajo del cual es una debilidad
 * @param minimoDePreguntas preguntas que debe haber visto el estudiante en la competencia para opinar sobre ella
 */
public record Umbrales(double fortaleza, double debilidad, int minimoDePreguntas) {

    public static final Umbrales POR_DEFECTO = new Umbrales(70, 50, 5);

    public Umbrales {
        if (debilidad < 0 || fortaleza > 100 || debilidad > fortaleza) {
            throw new IllegalArgumentException("Los umbrales deben cumplir 0 <= debilidad <= fortaleza <= 100");
        }
        if (minimoDePreguntas < 1) {
            throw new IllegalArgumentException("El mínimo de preguntas debe ser al menos 1");
        }
    }
}
