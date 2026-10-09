package co.unicauca.saberpro.preguntas.simulacro.domain.exception;

/**
 * Se lanza al intentar generar un simulacro (HU-12) cuando ninguna
 * pregunta publicada coincide con los filtros de competencia, tema y/o
 * dificultad seleccionados por el Docente.
 */
public class SinPreguntasDisponiblesException extends RuntimeException {
    public SinPreguntasDisponiblesException(String message) {
        super(message);
    }
}
