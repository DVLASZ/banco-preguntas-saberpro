package co.unicauca.bancopreguntas.question.infrastructure.persistence;

import co.unicauca.bancopreguntas.question.application.port.ProcessedEvents;
import org.springframework.stereotype.Component;

import java.time.Clock;

/** Adaptador de persistencia del puerto {@link ProcessedEvents} (idempotencia de los consumidores). */
@Component
public class JpaProcessedEvents implements ProcessedEvents {

    private final ProcessedEventJpaRepository repositorio;
    private final Clock reloj;

    public JpaProcessedEvents(ProcessedEventJpaRepository repositorio, Clock reloj) {
        this.repositorio = repositorio;
        this.reloj = reloj;
    }

    @Override
    public boolean registrarSiEsNuevo(String eventId) {
        return repositorio.insertarSiNoExiste(eventId, reloj.instant()) == 1;
    }
}
