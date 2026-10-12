package co.unicauca.bancopreguntas.review.domain;

/** No hay una revisión para la pregunta pedida. */
public class CasoNoEncontradoException extends RuntimeException {

    public CasoNoEncontradoException(String preguntaId) {
        super("No hay una revisión para la pregunta " + preguntaId);
    }
}
