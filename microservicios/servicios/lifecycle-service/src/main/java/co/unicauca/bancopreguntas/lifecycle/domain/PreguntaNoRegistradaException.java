package co.unicauca.bancopreguntas.lifecycle.domain;

/** No hay ciclo de vida registrado para la pregunta indicada. */
public class PreguntaNoRegistradaException extends RuntimeException {

    public PreguntaNoRegistradaException(String preguntaId) {
        super("No existe la pregunta " + preguntaId);
    }
}
