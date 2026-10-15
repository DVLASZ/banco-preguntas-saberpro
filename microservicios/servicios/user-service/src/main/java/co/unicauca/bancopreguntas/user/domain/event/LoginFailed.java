package co.unicauca.bancopreguntas.user.domain.event;

import java.time.Instant;

/** Falló un intento de inicio de sesión con el nombre de usuario {@code userId}. */
public record LoginFailed(String userId, String ip, String reason, Instant attemptedAt) implements DomainEvent {

    public static final String TYPE = "auth.login-failed";

    @Override
    public String type() {
        return TYPE;
    }
}
