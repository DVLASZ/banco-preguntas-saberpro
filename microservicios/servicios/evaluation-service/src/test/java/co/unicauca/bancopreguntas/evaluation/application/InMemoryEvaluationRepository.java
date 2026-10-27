package co.unicauca.bancopreguntas.evaluation.application;

import co.unicauca.bancopreguntas.evaluation.application.port.EvaluationRepository;
import co.unicauca.bancopreguntas.evaluation.domain.CasoDeEvaluacion;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/** Doble de prueba del repositorio: guarda los casos en memoria. */
class InMemoryEvaluationRepository implements EvaluationRepository {

    private final Map<String, CasoDeEvaluacion> casos = new LinkedHashMap<>();
    int guardados;

    @Override
    public void guardar(CasoDeEvaluacion caso) {
        casos.put(caso.preguntaId(), caso);
        guardados++;
    }

    @Override
    public Optional<CasoDeEvaluacion> buscarPorPreguntaId(String preguntaId) {
        return Optional.ofNullable(casos.get(preguntaId));
    }

    @Override
    public Optional<CasoDeEvaluacion> buscarPorPreguntaIdParaActualizar(String preguntaId) {
        return buscarPorPreguntaId(preguntaId);
    }
}
