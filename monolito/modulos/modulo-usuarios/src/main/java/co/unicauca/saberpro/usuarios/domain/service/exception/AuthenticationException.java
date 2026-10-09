package co.unicauca.saberpro.usuarios.domain.service.exception;

public class AuthenticationException extends RuntimeException {
    public AuthenticationException(String message) {
        super(message);
    }
}
