package co.unicauca.bancopreguntas.simulation.domain;

/** La operación no se puede hacer en el estado actual del simulacro (por ejemplo, finalizarlo dos veces). */
public class OperacionNoPermitidaException extends RuntimeException {

    public OperacionNoPermitidaException(String mensaje) {
        super(mensaje);
    }
}
