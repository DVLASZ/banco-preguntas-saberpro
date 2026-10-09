package co.unicauca.bancopreguntas.platform.security;

/** El usuario está identificado, pero su rol no le permite la operación. Responde 403. */
public class ForbiddenException extends RuntimeException {

    public ForbiddenException(String mensaje) {
        super(mensaje);
    }
}
