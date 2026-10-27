package co.unicauca.bancopreguntas.assignment.application;

import co.unicauca.bancopreguntas.assignment.application.port.ProcessedEvents;

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
