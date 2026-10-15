package co.unicauca.bancopreguntas.user.application.port;

import co.unicauca.bancopreguntas.user.domain.EventoDeSesion;
import co.unicauca.bancopreguntas.user.domain.UsuarioExterno;

import java.time.Instant;
import java.util.List;

/**
 * Puerto de salida: el proveedor de identidad (Keycloak), fuente de verdad de los usuarios, sus roles y sus
 * inicios de sesión.
 */
public interface IdentityProvider {

    /** Todos los usuarios del banco con sus roles. */
    List<UsuarioExterno> usuarios();

    /** Los inicios de sesión, exitosos y fallidos, posteriores a {@code desde}, del más antiguo al más reciente. */
    List<EventoDeSesion> eventosDeSesion(Instant desde);
}
