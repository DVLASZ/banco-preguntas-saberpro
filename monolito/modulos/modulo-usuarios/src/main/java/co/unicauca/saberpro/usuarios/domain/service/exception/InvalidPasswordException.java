package co.unicauca.saberpro.usuarios.domain.service.exception;

import java.util.List;

public class InvalidPasswordException extends RuntimeException {

    private final List<String> violations;

    public InvalidPasswordException(List<String> violations) {
        super("La contrasena no cumple la politica de seguridad: " + String.join("; ", violations));
        this.violations = violations;
    }

    public List<String> getViolations() {
        return violations;
    }
}
