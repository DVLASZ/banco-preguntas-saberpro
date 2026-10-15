package co.unicauca.bancopreguntas.user.application;

import co.unicauca.bancopreguntas.user.application.port.IdentityProvider;
import co.unicauca.bancopreguntas.user.domain.EventoDeSesion;
import co.unicauca.bancopreguntas.user.domain.UsuarioExterno;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/** Doble de prueba de Keycloak: devuelve lo que la prueba le indique o falla si se le pide. */
class FakeIdentityProvider implements IdentityProvider {

    List<UsuarioExterno> usuarios = new ArrayList<>();
    List<EventoDeSesion> eventos = new ArrayList<>();
    boolean caido;
    Instant ultimaConsultaDesde;

    @Override
    public List<UsuarioExterno> usuarios() {
        if (caido) {
            throw new IllegalStateException("Keycloak caído");
        }
        return List.copyOf(usuarios);
    }

    @Override
    public List<EventoDeSesion> eventosDeSesion(Instant desde) {
        if (caido) {
            throw new IllegalStateException("Keycloak caído");
        }
        ultimaConsultaDesde = desde;
        return List.copyOf(eventos);
    }
}
