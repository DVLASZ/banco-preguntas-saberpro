package co.unicauca.bancopreguntas.user.domain.event;

import java.time.Instant;

/** Un usuario inició sesión. {@code loggedInAt} es el momento real del inicio, no el de la publicación. */
public record LoginSucceeded(String userId, String ip, Instant loggedInAt) implements DomainEvent {

    public static final String TYPE = "auth.login-succeeded";

    @Override
    public String type() {
        return TYPE;
    }
}
