package co.unicauca.bancopreguntas.user.infrastructure.persistence;

import co.unicauca.bancopreguntas.user.application.port.SessionCursor;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Optional;

/** Adaptador de persistencia del puerto {@link SessionCursor}. */
@Component
public class JpaSessionCursor implements SessionCursor {

    private final SessionCursorJpaRepository repositorio;

    public JpaSessionCursor(SessionCursorJpaRepository repositorio) {
        this.repositorio = repositorio;
    }

    @Override
    public Optional<Instant> tomar() {
        return repositorio.tomarSinEsperar().map(SessionCursorEntity::getLastEventAt);
    }

    @Override
    public void avanzarA(Instant momento) {
        SessionCursorEntity fila = repositorio.findById(SessionCursorEntity.ID)
                .orElseThrow(() -> new IllegalStateException("Falta la fila del cursor de sesiones"));
        fila.setLastEventAt(momento);
        repositorio.saveAndFlush(fila);
    }
}
