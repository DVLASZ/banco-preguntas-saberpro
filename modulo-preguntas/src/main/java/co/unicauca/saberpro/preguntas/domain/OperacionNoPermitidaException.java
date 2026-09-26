package co.unicauca.saberpro.preguntas.domain;

/** La operación no está permitida en el estado actual de la pregunta (RF-06, RF-15). */
public class OperacionNoPermitidaException extends IllegalStateException {

    public OperacionNoPermitidaException(String mensaje) {
        super(mensaje);
    }
}
