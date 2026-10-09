package co.unicauca.bancopreguntas.question.application;

import co.unicauca.bancopreguntas.question.application.port.QuestionRepository;
import co.unicauca.bancopreguntas.question.domain.CambioEstado;
import co.unicauca.bancopreguntas.question.domain.FiltroPreguntas;
import co.unicauca.bancopreguntas.question.domain.Pagina;
import co.unicauca.bancopreguntas.question.domain.Question;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Doble de prueba del puerto de persistencia: guarda en memoria y ordena como la consulta real. */
class InMemoryQuestionRepository implements QuestionRepository {

    private final Map<String, Question> preguntas = new LinkedHashMap<>();
    private final Map<String, List<CambioEstado>> historiales = new LinkedHashMap<>();

    @Override
    public Question guardar(Question pregunta) {
        preguntas.put(pregunta.getId(), pregunta);
        return pregunta;
    }

    @Override
    public Optional<Question> buscarPorId(String id) {
        return Optional.ofNullable(preguntas.get(id));
    }

    @Override
    public Pagina<Question> buscar(String autor, FiltroPreguntas filtro, int pagina, int tamano) {
        List<Question> coincidentes = preguntas.values().stream()
                .filter(p -> autor == null || p.getAutor().equals(autor))
                .filter(filtro::coincide)
                .sorted(Comparator.comparing(Question::getCreadaEn).reversed().thenComparing(Question::getId))
                .toList();
        int desde = Math.min((pagina - 1) * tamano, coincidentes.size());
        int hasta = Math.min(desde + tamano, coincidentes.size());
        return new Pagina<>(new ArrayList<>(coincidentes.subList(desde, hasta)), pagina, tamano, coincidentes.size());
    }

    @Override
    public void registrarCambioEstado(String preguntaId, CambioEstado cambio) {
        historiales.computeIfAbsent(preguntaId, id -> new ArrayList<>()).add(cambio);
    }

    @Override
    public List<CambioEstado> historial(String preguntaId) {
        return List.copyOf(historiales.getOrDefault(preguntaId, List.of()));
    }

    int cantidad() {
        return preguntas.size();
    }
}
