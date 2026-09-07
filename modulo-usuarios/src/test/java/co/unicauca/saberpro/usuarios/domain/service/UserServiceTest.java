package co.unicauca.saberpro.usuarios.domain.service;

import co.unicauca.saberpro.usuarios.domain.Role;
import co.unicauca.saberpro.usuarios.domain.User;
import co.unicauca.saberpro.usuarios.domain.UserStatus;
import co.unicauca.saberpro.usuarios.domain.access.IUserRepository;
import co.unicauca.saberpro.usuarios.domain.security.IPasswordHasher;
import co.unicauca.saberpro.usuarios.domain.service.exception.AuthenticationException;
import co.unicauca.saberpro.usuarios.domain.service.exception.DuplicateUsernameException;
import co.unicauca.saberpro.usuarios.domain.service.exception.InvalidPasswordException;
import co.unicauca.saberpro.usuarios.domain.validation.IPasswordPolicy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Pruebas del caso de uso de usuarios. Gracias a que {@link UserService}
 * depende de abstracciones (DIP), sus tres colaboradores se pueden
 * reemplazar por dobles de prueba (mocks) sin necesitar una base de datos
 * SQLite real ni calcular hashes Argon2 de verdad.
 */
@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private IUserRepository userRepository;
    @Mock
    private IPasswordHasher passwordHasher;
    @Mock
    private IPasswordPolicy passwordPolicy;

    private UserService userService;

    @BeforeEach
    void setUp() {
        userService = new UserService(userRepository, passwordHasher, passwordPolicy);
    }

    @Test
    void registerShouldHashPasswordAndSaveUser() {
        when(passwordPolicy.validate("Abcdef1$")).thenReturn(List.of());
        when(userRepository.existsByUsername("jperez")).thenReturn(false);
        when(passwordHasher.hash("Abcdef1$")).thenReturn("hashed-value");
        when(userRepository.save(any(User.class))).thenReturn(true);

        User result = userService.register("jperez", "Juan Perez", Role.ESTUDIANTE,
                UserStatus.ACTIVO, "Abcdef1$");

        assertEquals("jperez", result.getUsername());
        assertEquals("hashed-value", result.getPasswordHash());
        verify(userRepository).save(any(User.class));
    }

    @Test
    void registerShouldRejectInvalidPasswordWithoutTouchingRepository() {
        when(passwordPolicy.validate("weak")).thenReturn(List.of("La contrasena debe tener al menos 6 caracteres"));

        assertThrows(InvalidPasswordException.class,
                () -> userService.register("jperez", "Juan Perez", Role.ESTUDIANTE, UserStatus.ACTIVO, "weak"));

        verifyNoInteractions(userRepository);
        verifyNoInteractions(passwordHasher);
    }

    @Test
    void registerShouldRejectDuplicateUsername() {
        when(passwordPolicy.validate("Abcdef1$")).thenReturn(List.of());
        when(userRepository.existsByUsername("jperez")).thenReturn(true);

        assertThrows(DuplicateUsernameException.class,
                () -> userService.register("jperez", "Juan Perez", Role.ESTUDIANTE, UserStatus.ACTIVO, "Abcdef1$"));

        verify(userRepository, never()).save(any());
    }

    @Test
    void authenticateShouldReturnUserWhenCredentialsMatchAndUserIsActive() {
        User storedUser = new User(1L, "jperez", "Juan Perez", Role.ESTUDIANTE, UserStatus.ACTIVO, "hashed-value");
        when(userRepository.findByUsername("jperez")).thenReturn(Optional.of(storedUser));
        when(passwordHasher.matches("Abcdef1$", "hashed-value")).thenReturn(true);

        User result = userService.authenticate("jperez", "Abcdef1$");

        assertSame(storedUser, result);
    }

    @Test
    void authenticateShouldFailWhenUsernameDoesNotExist() {
        when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        assertThrows(AuthenticationException.class, () -> userService.authenticate("ghost", "Abcdef1$"));
    }

    @Test
    void authenticateShouldFailWhenPasswordDoesNotMatch() {
        User storedUser = new User(1L, "jperez", "Juan Perez", Role.ESTUDIANTE, UserStatus.ACTIVO, "hashed-value");
        when(userRepository.findByUsername("jperez")).thenReturn(Optional.of(storedUser));
        when(passwordHasher.matches("wrong", "hashed-value")).thenReturn(false);

        assertThrows(AuthenticationException.class, () -> userService.authenticate("jperez", "wrong"));
    }

    @Test
    void authenticateShouldFailWhenUserIsInactive() {
        User inactiveUser = new User(1L, "jperez", "Juan Perez", Role.ESTUDIANTE, UserStatus.INACTIVO, "hashed-value");
        when(userRepository.findByUsername("jperez")).thenReturn(Optional.of(inactiveUser));
        when(passwordHasher.matches(eq("Abcdef1$"), eq("hashed-value"))).thenReturn(true);

        assertThrows(AuthenticationException.class, () -> userService.authenticate("jperez", "Abcdef1$"));
    }
}
