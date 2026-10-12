package co.unicauca.bancopreguntas.notification.application;

import co.unicauca.bancopreguntas.notification.application.port.QuestionInfoRepository;
import co.unicauca.bancopreguntas.notification.domain.InfoPregunta;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/** Doble de prueba del repositorio de información de preguntas. */
class InMemoryQuestionInfoRepository implements QuestionInfoRepository {

    private final Map<String, InfoPregunta> datos = new HashMap<>();

    @Override
    public void guardar(InfoPregunta info) {
        datos.put(info.preguntaId(), info);
    }

    @Override
    public Optional<InfoPregunta> buscar(String preguntaId) {
        return Optional.ofNullable(datos.get(preguntaId));
    }
}
