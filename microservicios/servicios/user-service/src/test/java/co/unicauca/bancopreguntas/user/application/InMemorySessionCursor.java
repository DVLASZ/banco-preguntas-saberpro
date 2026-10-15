package co.unicauca.bancopreguntas.user.application;

import co.unicauca.bancopreguntas.user.application.port.SessionCursor;

import java.time.Instant;
import java.util.Optional;

/** Doble de prueba del cursor: puede simular que otra réplica lo tiene tomado. */
class InMemorySessionCursor implements SessionCursor {

    Instant valor;
    boolean tomadoPorOtraReplica;

    InMemorySessionCursor(Instant inicial) {
        this.valor = inicial;
    }

    @Override
    public Optional<Instant> tomar() {
        return tomadoPorOtraReplica ? Optional.empty() : Optional.of(valor);
    }

    @Override
    public void avanzarA(Instant momento) {
        valor = momento;
    }
}
