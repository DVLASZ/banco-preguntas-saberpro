package co.unicauca.bancopreguntas.assignment.domain;

/** La operación no es posible en el estado actual de la revisión. */
public class OperacionNoPermitidaException extends RuntimeException {

    public OperacionNoPermitidaException(String mensaje) {
        super(mensaje);
    }
}
