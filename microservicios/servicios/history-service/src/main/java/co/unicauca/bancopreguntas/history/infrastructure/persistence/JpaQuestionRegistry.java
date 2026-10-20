package co.unicauca.bancopreguntas.history.infrastructure.persistence;

import co.unicauca.bancopreguntas.history.application.port.QuestionRegistry;
import co.unicauca.bancopreguntas.history.domain.InfoPregunta;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Optional;

/** Adaptador de persistencia del puerto {@link QuestionRegistry}. */
@Component
public class JpaQuestionRegistry implements QuestionRegistry {

    private final QuestionInfoJpaRepository preguntas;
    private final QuestionReviewerJpaRepository revisores;

    public JpaQuestionRegistry(QuestionInfoJpaRepository preguntas, QuestionReviewerJpaRepository revisores) {
        this.preguntas = preguntas;
        this.revisores = revisores;
    }

    @Override
    public void guardar(InfoPregunta info) {
        QuestionInfoEntity entidad = preguntas.findById(info.preguntaId())
                .orElseGet(() -> new QuestionInfoEntity(info.preguntaId(), info.titulo(), info.autorId()));
        entidad.setTitle(info.titulo());
        entidad.setAuthorId(info.autorId());
        preguntas.saveAndFlush(entidad);
    }

    @Override
    public Optional<InfoPregunta> buscar(String preguntaId) {
        return preguntas.findById(preguntaId)
                .map(e -> new InfoPregunta(e.getQuestionId(), e.getTitle(), e.getAuthorId()));
    }

    @Override
    public void agregarRevisores(String preguntaId, Collection<String> ids) {
        ids.forEach(id -> revisores.agregar(preguntaId, id));
    }

    @Override
    public boolean esRevisor(String preguntaId, String usuarioId) {
        return revisores.existsByQuestionIdAndReviewerId(preguntaId, usuarioId);
    }
}
