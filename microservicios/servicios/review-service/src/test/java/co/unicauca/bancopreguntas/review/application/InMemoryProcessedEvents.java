package co.unicauca.bancopreguntas.review.application;

import co.unicauca.bancopreguntas.review.application.port.ProcessedEvents;

import java.util.HashSet;
import java.util.Set;

/** Doble de prueba del registro de eventos procesados. */
class InMemoryProcessedEvents implements ProcessedEvents {

    private final Set<String> vistos = new HashSet<>();

    @Override
    public boolean registrarSiEsNuevo(String eventId) {
        return vistos.add(eventId);
    }
}
