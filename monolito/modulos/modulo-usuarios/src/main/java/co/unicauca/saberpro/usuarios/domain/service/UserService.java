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

import java.util.List;

/**
 * Orquesta el registro y la autenticacion de usuarios (RF-01, RF-02).
 * <p>
 * Al igual que {@code Service} en el ejemplo 5 de Inversion de
 * Dependencias visto en la teoria, esta clase (modulo de "alto nivel") no
 * depende de detalles concretos: recibe sus tres colaboradores como
 * abstracciones por el constructor (inyeccion de dependencias manual) y
 * nunca sabe si detras hay SQLite, Argon2 u otra tecnologia.
 */
public class UserService {

    // Ahora hay una dependencia de abstracciones, no de algo concreto;
    // no sabe como estan implementadas.
    private final IUserRepository repository;
    private final IPasswordHasher passwordHasher;
    private final IPasswordPolicy passwordPolicy;

    /**
     * Inyeccion de dependencias en el constructor. Ya no conviene que el
     * mismo servicio cree sus propias implementaciones concretas.
     */
    public UserService(IUserRepository repository, IPasswordHasher passwordHasher, IPasswordPolicy passwordPolicy) {
        this.repository = repository;
        this.passwordHasher = passwordHasher;
        this.passwordPolicy = passwordPolicy;
    }

    public User register(String username, String fullName, Role role, UserStatus status, String plainPassword) {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("El nombre de usuario es obligatorio");
        }
        if (fullName == null || fullName.isBlank()) {
            throw new IllegalArgumentException("El nombre completo es obligatorio");
        }

        List<String> violations = passwordPolicy.validate(plainPassword);
        if (!violations.isEmpty()) {
            throw new InvalidPasswordException(violations);
        }

        if (repository.existsByUsername(username)) {
            throw new DuplicateUsernameException(username);
        }

        String passwordHash = passwordHasher.hash(plainPassword);
        User newUser = User.newUser(username, fullName, role, status, passwordHash);
        repository.save(newUser);
        return newUser;
    }

    public User authenticate(String username, String plainPassword) {
        User user = repository.findByUsername(username)
                .orElseThrow(() -> new AuthenticationException("Usuario o contrasena incorrectos"));

        if (!passwordHasher.matches(plainPassword, user.getPasswordHash())) {
            throw new AuthenticationException("Usuario o contrasena incorrectos");
        }
        if (!user.isActive()) {
            throw new AuthenticationException("El usuario '" + username + "' se encuentra inactivo");
        }
        return user;
    }

    public List<User> listUsers() {
        return repository.list();
    }
}
