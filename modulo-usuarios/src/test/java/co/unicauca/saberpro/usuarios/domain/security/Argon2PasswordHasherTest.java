package co.unicauca.saberpro.usuarios.domain.security;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class Argon2PasswordHasherTest {

    private final Argon2PasswordHasher hasher = new Argon2PasswordHasher();

    @Test
    void hashShouldNotEqualPlainPassword() {
        String hash = hasher.hash("Abcdef1$");
        assertNotEquals("Abcdef1$", hash);
    }

    @Test
    void matchesShouldReturnTrueForCorrectPassword() {
        String hash = hasher.hash("Abcdef1$");
        assertTrue(hasher.matches("Abcdef1$", hash));
    }

    @Test
    void matchesShouldReturnFalseForIncorrectPassword() {
        String hash = hasher.hash("Abcdef1$");
        assertFalse(hasher.matches("otraClave1$", hash));
    }
}
