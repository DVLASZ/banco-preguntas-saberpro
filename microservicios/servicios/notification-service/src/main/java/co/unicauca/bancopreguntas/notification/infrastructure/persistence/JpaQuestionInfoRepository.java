package co.unicauca.bancopreguntas.notification.infrastructure.persistence;

import co.unicauca.bancopreguntas.notification.application.port.QuestionInfoRepository;
import co.unicauca.bancopreguntas.notification.domain.InfoPregunta;
import org.springframework.stereotype.Component;

import java.util.Optional;

/** Adaptador de persistencia del puerto {@link QuestionInfoRepository}. */
@Component
public class JpaQuestionInfoRepository implements QuestionInfoRepository {

    private final QuestionInfoJpaRepository repositorio;

    public JpaQuestionInfoRepository(QuestionInfoJpaRepository repositorio) {
        this.repositorio = repositorio;
    }

    @Override
    public void guardar(InfoPregunta info) {
        QuestionInfoEntity entidad = repositorio.findById(info.preguntaId())
                .orElseGet(() -> new QuestionInfoEntity(info.preguntaId(), info.titulo(), info.autorId()));
        entidad.setTitle(info.titulo());
        entidad.setAuthorId(info.autorId());
        entidad.setAdminId(info.administradorId());
        repositorio.saveAndFlush(entidad);
    }

    @Override
    public Optional<InfoPregunta> buscar(String preguntaId) {
        return repositorio.findById(preguntaId)
                .map(e -> new InfoPregunta(e.getQuestionId(), e.getTitle(), e.getAuthorId(), e.getAdminId()));
    }
}
