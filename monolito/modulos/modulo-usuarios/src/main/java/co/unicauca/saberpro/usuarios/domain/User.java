package co.unicauca.saberpro.usuarios.domain;

import java.util.Objects;

/**
 * Entidad de dominio Usuario. No conoce SQLite, Swing ni el algoritmo de
 * hashing utilizado: solo modela los datos y reglas propias de un usuario
 * (RF-01 a RF-03). El acoplamiento a tecnologias concretas vive en otras
 * capas (repository, security), respetando el Principio de Responsabilidad
 * Unica (SRP).
 */
public class User {

    private Long id;
    private final String username;
    private final String fullName;
    private final Role role;
    private UserStatus status;
    private final String passwordHash;

    public User(Long id, String username, String fullName, Role role,
                UserStatus status, String passwordHash) {
        this.id = id;
        this.username = Objects.requireNonNull(username, "username no puede ser nulo");
        this.fullName = Objects.requireNonNull(fullName, "fullName no puede ser nulo");
        this.role = Objects.requireNonNull(role, "role no puede ser nulo");
        this.status = Objects.requireNonNull(status, "status no puede ser nulo");
        this.passwordHash = Objects.requireNonNull(passwordHash, "passwordHash no puede ser nulo");
    }

    public static User newUser(String username, String fullName, Role role,
                                UserStatus status, String passwordHash) {
        return new User(null, username, fullName, role, status, passwordHash);
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public String getFullName() {
        return fullName;
    }

    public Role getRole() {
        return role;
    }

    public UserStatus getStatus() {
        return status;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public boolean isActive() {
        return status == UserStatus.ACTIVO;
    }

    public void activate() {
        this.status = UserStatus.ACTIVO;
    }

    public void deactivate() {
        this.status = UserStatus.INACTIVO;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof User)) return false;
        User user = (User) o;
        return username.equalsIgnoreCase(user.username);
    }

    @Override
    public int hashCode() {
        return Objects.hash(username.toLowerCase());
    }

    @Override
    public String toString() {
        return "User{username='" + username + "', fullName='" + fullName +
                "', role=" + role + ", status=" + status + "}";
    }
}
