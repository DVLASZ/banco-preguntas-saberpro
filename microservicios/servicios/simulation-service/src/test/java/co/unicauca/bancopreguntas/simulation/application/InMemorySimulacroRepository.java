package co.unicauca.bancopreguntas.simulation.application;

import co.unicauca.bancopreguntas.simulation.application.port.SimulacroRepository;
import co.unicauca.bancopreguntas.simulation.domain.EstadoSimulacro;
import co.unicauca.bancopreguntas.simulation.domain.Pagina;
import co.unicauca.bancopreguntas.simulation.domain.Simulacro;

import java.time.Instant;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Doble de prueba de los simulacros, en memoria. */
class InMemorySimulacroRepository implements SimulacroRepository {

    private final Map<String, Simulacro> simulacros = new LinkedHashMap<>();
    int guardados;

    @Override
    public void guardar(Simulacro simulacro) {
        simulacros.put(simulacro.getId(), simulacro);
        guardados++;
    }

    @Override
    public Optional<Simulacro> buscar(String id) {
        return Optional.ofNullable(simulacros.get(id));
    }

    @Override
    public Pagina<Simulacro> delEstudiante(String estudianteId, int pagina, int tamano) {
        List<Simulacro> suyos = simulacros.values().stream().filter(s -> s.getEstudianteId().equals(estudianteId))
                .sorted(Comparator.comparing(Simulacro::getIniciadoEn).reversed()).toList();
        int desde = Math.min((pagina - 1) * tamano, suyos.size());
        return new Pagina<>(suyos.subList(desde, Math.min(desde + tamano, suyos.size())), pagina, tamano, suyos.size());
    }

    @Override
    public List<Simulacro> vencidos(Instant ahora, int limite) {
        return simulacros.values().stream()
                .filter(s -> s.getEstado() == EstadoSimulacro.EN_CURSO && s.getVenceEn().isBefore(ahora))
                .limit(limite).toList();
    }
}
