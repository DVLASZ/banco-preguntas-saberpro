package co.unicauca.bancopreguntas.review.domain;

/** El usuario no participa en esta revisión, así que no puede verla ni actuar sobre ella. */
public class AccesoNoPermitidoException extends RuntimeException {

    public AccesoNoPermitidoException(String mensaje) {
        super(mensaje);
    }
}
