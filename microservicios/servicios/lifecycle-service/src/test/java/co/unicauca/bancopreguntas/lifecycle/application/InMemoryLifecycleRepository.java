package co.unicauca.bancopreguntas.lifecycle.application;

import co.unicauca.bancopreguntas.lifecycle.application.port.LifecycleRepository;
import co.unicauca.bancopreguntas.lifecycle.domain.CambioEstado;
import co.unicauca.bancopreguntas.lifecycle.domain.CicloDeVida;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Doble de prueba del puerto de persistencia, en memoria. */
class InMemoryLifecycleRepository implements LifecycleRepository {

    private final Map<String, CicloDeVida> ciclos = new HashMap<>();
    private final Map<String, List<CambioEstado>> historiales = new HashMap<>();

    @Override
    public void guardar(CicloDeVida ciclo) {
        ciclos.put(ciclo.getPreguntaId(), ciclo);
    }

    @Override
    public Optional<CicloDeVida> buscar(String preguntaId) {
        return Optional.ofNullable(ciclos.get(preguntaId));
    }

    @Override
    public void registrarCambioEstado(String preguntaId, CambioEstado cambio) {
        historiales.computeIfAbsent(preguntaId, k -> new ArrayList<>()).add(cambio);
    }

    @Override
    public List<CambioEstado> historial(String preguntaId) {
        return List.copyOf(historiales.getOrDefault(preguntaId, List.of()));
    }
}
