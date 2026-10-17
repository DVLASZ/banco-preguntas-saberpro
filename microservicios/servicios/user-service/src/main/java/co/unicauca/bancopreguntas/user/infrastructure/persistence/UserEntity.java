package co.unicauca.bancopreguntas.user.infrastructure.persistence;

import co.unicauca.bancopreguntas.user.domain.Usuario;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.Arrays;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

/** Fila de {@code user_profile}. Es un detalle de persistencia: el dominio usa {@code Usuario}. */
@Entity
@Table(name = "user_profile")
public class UserEntity {

    @Id
    @Column(length = 100)
    private String username;

    @Column(name = "keycloak_id")
    private String keycloakId;

    @Column(name = "full_name", nullable = false)
    private String fullName;

    @Column
    private String email;

    /** Roles entre barras ({@code |ADMIN|REVIEWER|}) para poder buscar uno exacto con LIKE. */
    @Column(nullable = false)
    private String roles = "|";

    @Column(nullable = false)
    private boolean active;

    @Column(name = "last_login_at")
    private Instant lastLoginAt;

    @Column(name = "synced_at", nullable = false)
    private Instant syncedAt;

    protected UserEntity() {
        // requerido por JPA
    }

    static UserEntity de(Usuario u) {
        UserEntity e = new UserEntity();
        e.copiar(u);
        return e;
    }

    void copiar(Usuario u) {
        username = u.id();
        keycloakId = u.keycloakId();
        fullName = u.nombre();
        email = u.correo();
        roles = u.roles().isEmpty() ? "|" : "|" + String.join("|", new TreeSet<>(u.roles())) + "|";
        active = u.activo();
        lastLoginAt = u.ultimoInicioDeSesion();
        syncedAt = u.sincronizadoEn();
    }

    Usuario aDominio() {
        Set<String> conjunto = Arrays.stream(roles.split("\\|")).filter(r -> !r.isBlank()).collect(Collectors.toSet());
        return new Usuario(username, keycloakId, fullName, email, conjunto, active, lastLoginAt, syncedAt);
    }

    public String getUsername() { return username; }
}
