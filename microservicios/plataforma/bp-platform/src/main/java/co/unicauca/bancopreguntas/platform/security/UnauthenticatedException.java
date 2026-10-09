package co.unicauca.bancopreguntas.platform.security;

/** La petición no trae la identidad del usuario. Responde 401. */
public class UnauthenticatedException extends RuntimeException {

    public UnauthenticatedException(String mensaje) {
        super(mensaje);
    }
}
