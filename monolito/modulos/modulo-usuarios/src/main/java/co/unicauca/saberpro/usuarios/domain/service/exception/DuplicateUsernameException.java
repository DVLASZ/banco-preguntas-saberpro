package co.unicauca.saberpro.usuarios.domain.service.exception;

public class DuplicateUsernameException extends RuntimeException {
    public DuplicateUsernameException(String username) {
        super("El nombre de usuario '" + username + "' ya esta registrado");
    }
}
