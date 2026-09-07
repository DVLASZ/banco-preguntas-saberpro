package co.unicauca.saberpro.preguntas.domain;

/**
 * Conjunto de las 4 opciones de respuesta (A, B, C, D) de una pregunta de
 * selección múltiple. Una es la respuesta correcta y las otras tres son
 * los distractores.
 */
public class QuestionDistractors {

    private final String opcionA;
    private final String opcionB;
    private final String opcionC;
    private final String opcionD;

    public QuestionDistractors(String opcionA, String opcionB, String opcionC, String opcionD) {
        if (opcionA == null || opcionA.isBlank()
                || opcionB == null || opcionB.isBlank()
                || opcionC == null || opcionC.isBlank()
                || opcionD == null || opcionD.isBlank()) {
            throw new IllegalArgumentException("Las 4 opciones son obligatorias");
        }
        this.opcionA = opcionA;
        this.opcionB = opcionB;
        this.opcionC = opcionC;
        this.opcionD = opcionD;
    }

    public String getOpcionA() {
        return opcionA;
    }

    public String getOpcionB() {
        return opcionB;
    }

    public String getOpcionC() {
        return opcionC;
    }

    public String getOpcionD() {
        return opcionD;
    }

    /** Retorna el texto de la opción identificada por su letra (A-D). */
    public String obtenerOpcion(char letra) {
        return switch (Character.toUpperCase(letra)) {
            case 'A' -> opcionA;
            case 'B' -> opcionB;
            case 'C' -> opcionC;
            case 'D' -> opcionD;
            default -> throw new IllegalArgumentException("Opción inválida: " + letra);
        };
    }
}
