package co.unicauca.bancopreguntas.question.domain;

/** La operación no está permitida en el estado actual de la pregunta (RF-06, RF-15). */
public class OperacionNoPermitidaException extends IllegalStateException {

    public OperacionNoPermitidaException(String mensaje) {
        super(mensaje);
    }
}
