package co.unicauca.bancopreguntas.assignment.domain;

/** No hay una revisión para la pregunta pedida. */
public class CasoNoEncontradoException extends RuntimeException {

    public CasoNoEncontradoException(String preguntaId) {
        super("No hay una revisión para la pregunta " + preguntaId);
    }
}
