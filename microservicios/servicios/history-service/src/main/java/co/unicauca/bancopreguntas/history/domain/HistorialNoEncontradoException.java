package co.unicauca.bancopreguntas.history.domain;

/** No hay historial de la pregunta, o quien pregunta no tiene relación con ella (no se distingue). */
public class HistorialNoEncontradoException extends RuntimeException {

    public HistorialNoEncontradoException(String preguntaId) {
        super("No hay historial de la pregunta " + preguntaId);
    }
}
