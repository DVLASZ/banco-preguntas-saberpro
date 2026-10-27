package co.unicauca.bancopreguntas.assignment.application;

import co.unicauca.bancopreguntas.assignment.application.port.AssignmentRepository;
import co.unicauca.bancopreguntas.assignment.domain.AsignacionDeRevisor;
import co.unicauca.bancopreguntas.assignment.domain.CasoDeAsignacion;
import co.unicauca.bancopreguntas.assignment.domain.CasoPendiente;
import co.unicauca.bancopreguntas.assignment.domain.EstadoRonda;
import co.unicauca.bancopreguntas.assignment.domain.FiltroAsignacion;
import co.unicauca.bancopreguntas.assignment.domain.Pagina;
import co.unicauca.bancopreguntas.assignment.domain.RondaDeAsignacion;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Doble de prueba del repositorio: guarda los casos en memoria y calcula las bandejas como lo haría la base. */
class InMemoryAssignmentRepository implements AssignmentRepository {

    private final Map<String, CasoDeAsignacion> casos = new LinkedHashMap<>();
    int guardados;

    @Override
    public void guardar(CasoDeAsignacion caso) {
        casos.put(caso.preguntaId(), caso);
        guardados++;
    }

    @Override
    public Optional<CasoDeAsignacion> buscarPorPreguntaId(String preguntaId) {
        return Optional.ofNullable(casos.get(preguntaId));
    }

    @Override
    public Optional<CasoDeAsignacion> buscarPorPreguntaIdParaActualizar(String preguntaId) {
        return buscarPorPreguntaId(preguntaId);
    }

    @Override
    public Pagina<CasoPendiente> pendientesDeAsignacion(int pagina, int tamano) {
        List<CasoPendiente> todos = casos.values().stream()
                .filter(c -> c.ultimaRonda().map(r -> r.estado() == EstadoRonda.PENDIENTE_ASIGNACION).orElse(false))
                .map(c -> new CasoPendiente(c.preguntaId(), c.titulo(), c.competencia(), c.autorId(),
                        c.rondaActual().numero(), c.rondaActual().iniciadaEn()))
                .sorted(Comparator.comparing(CasoPendiente::enviadaEn))
                .toList();
        return paginar(todos, pagina, tamano);
    }

    @Override
    public Pagina<AsignacionDeRevisor> asignadasA(String revisorId, FiltroAsignacion filtro, int pagina, int tamano) {
        List<AsignacionDeRevisor> todas = new ArrayList<>();
        for (CasoDeAsignacion c : casos.values()) {
            for (RondaDeAsignacion r : c.rondas()) {
                r.asignaciones().stream().filter(a -> a.revisorId().equals(revisorId)).findFirst().ifPresent(a -> {
                    AsignacionDeRevisor fila = new AsignacionDeRevisor(c.preguntaId(), c.titulo(), c.competencia(),
                            c.autorId(), r.numero(), a.asignadoEn(), r.estado(), r.decisionDe(revisorId).orElse(null));
                    boolean incluir = switch (filtro) {
                        case TODAS -> true;
                        case PENDIENTES -> fila.pendiente();
                        case EVALUADAS -> fila.miDecision() != null;
                    };
                    if (incluir) {
                        todas.add(fila);
                    }
                });
            }
        }
        todas.sort(Comparator.comparing(AsignacionDeRevisor::asignadaEn).reversed());
        return paginar(todas, pagina, tamano);
    }

    private static <T> Pagina<T> paginar(List<T> todos, int pagina, int tamano) {
        int desde = Math.min((pagina - 1) * tamano, todos.size());
        int hasta = Math.min(desde + tamano, todos.size());
        return new Pagina<>(todos.subList(desde, hasta), pagina, tamano, todos.size());
    }
}
