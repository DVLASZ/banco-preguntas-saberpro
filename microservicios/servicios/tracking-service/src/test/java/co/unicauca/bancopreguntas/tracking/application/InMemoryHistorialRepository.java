package co.unicauca.bancopreguntas.tracking.application;

import co.unicauca.bancopreguntas.tracking.application.port.HistorialRepository;
import co.unicauca.bancopreguntas.tracking.domain.Pagina;
import co.unicauca.bancopreguntas.tracking.domain.ResultadoRegistrado;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Doble de prueba del historial, en memoria. */
class InMemoryHistorialRepository implements HistorialRepository {

    private final Map<String, ResultadoRegistrado> resultados = new LinkedHashMap<>();

    @Override
    public void guardar(ResultadoRegistrado resultado) {
        resultados.putIfAbsent(resultado.simulacroId(), resultado);
    }

    @Override
    public Pagina<ResultadoRegistrado> delEstudiante(String estudianteId, int pagina, int tamano) {
        List<ResultadoRegistrado> suyos = todosDelEstudiante(estudianteId).stream()
                .sorted(Comparator.comparing(ResultadoRegistrado::finalizadoEn).reversed()).toList();
        int desde = Math.min((pagina - 1) * tamano, suyos.size());
        return new Pagina<>(suyos.subList(desde, Math.min(desde + tamano, suyos.size())), pagina, tamano, suyos.size());
    }

    @Override
    public List<ResultadoRegistrado> todosDelEstudiante(String estudianteId) {
        return resultados.values().stream().filter(r -> r.estudianteId().equals(estudianteId)).toList();
    }

    @Override
    public List<ResultadoRegistrado> todos() {
        return List.copyOf(resultados.values());
    }
}
