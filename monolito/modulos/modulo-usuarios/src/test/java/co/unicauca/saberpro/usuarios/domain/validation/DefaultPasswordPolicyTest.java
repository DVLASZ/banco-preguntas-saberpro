package co.unicauca.saberpro.usuarios.domain.validation;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DefaultPasswordPolicyTest {

    private final DefaultPasswordPolicy policy = new DefaultPasswordPolicy();

    @Test
    void validPasswordHasNoViolations() {
        List<String> violations = policy.validate("Abcdef1$");
        assertTrue(violations.isEmpty());
        assertTrue(policy.isValid("Abcdef1$"));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "abc12$",   // sin mayuscula
            "ABCDEF1",  // sin caracter especial
            "Abcdef$",  // sin digito
            "Ab1$",     // muy corta
    })
    void invalidPasswordsAreRejected(String password) {
        assertFalse(policy.isValid(password));
    }

    @Test
    void nullPasswordIsRejectedWithAllViolations() {
        List<String> violations = policy.validate(null);
        assertFalse(violations.isEmpty());
    }
}
