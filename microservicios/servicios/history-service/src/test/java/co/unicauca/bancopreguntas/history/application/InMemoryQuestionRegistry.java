package co.unicauca.bancopreguntas.history.application;

import co.unicauca.bancopreguntas.history.application.port.QuestionRegistry;
import co.unicauca.bancopreguntas.history.domain.InfoPregunta;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/** Doble de prueba del registro de preguntas y revisores. */
class InMemoryQuestionRegistry implements QuestionRegistry {

    private final Map<String, InfoPregunta> preguntas = new HashMap<>();
    private final Set<String> revisores = new HashSet<>();

    @Override
    public void guardar(InfoPregunta info) {
        preguntas.put(info.preguntaId(), info);
    }

    @Override
    public Optional<InfoPregunta> buscar(String preguntaId) {
        return Optional.ofNullable(preguntas.get(preguntaId));
    }

    @Override
    public void agregarRevisores(String preguntaId, Collection<String> ids) {
        ids.forEach(id -> revisores.add(preguntaId + "|" + id));
    }

    @Override
    public boolean esRevisor(String preguntaId, String usuarioId) {
        return revisores.contains(preguntaId + "|" + usuarioId);
    }
}
