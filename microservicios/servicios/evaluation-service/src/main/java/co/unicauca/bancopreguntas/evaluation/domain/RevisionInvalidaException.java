package co.unicauca.bancopreguntas.evaluation.domain;

/** Los datos de la operación no cumplen las reglas de la revisión (por ejemplo, demasiados revisores). */
public class RevisionInvalidaException extends RuntimeException {

    public RevisionInvalidaException(String mensaje) {
        super(mensaje);
    }
}
