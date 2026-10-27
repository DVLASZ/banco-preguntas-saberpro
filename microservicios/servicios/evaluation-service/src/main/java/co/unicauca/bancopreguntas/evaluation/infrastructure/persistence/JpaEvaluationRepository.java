package co.unicauca.bancopreguntas.evaluation.infrastructure.persistence;

import co.unicauca.bancopreguntas.evaluation.application.port.EvaluationRepository;
import co.unicauca.bancopreguntas.evaluation.domain.CasoDeEvaluacion;
import org.springframework.stereotype.Component;

import java.util.Optional;

/** Adaptador de persistencia del puerto {@link EvaluationRepository} (patrón Adapter). */
@Component
public class JpaEvaluationRepository implements EvaluationRepository {

    private final ReviewCaseJpaRepository repositorio;

    public JpaEvaluationRepository(ReviewCaseJpaRepository repositorio) {
        this.repositorio = repositorio;
    }

    @Override
    public void guardar(CasoDeEvaluacion caso) {
        ReviewCaseEntity entidad = repositorio.findByQuestionId(caso.preguntaId()).orElseGet(ReviewCaseEntity::new);
        ReviewCaseMapper.sincronizar(caso, entidad);
        repositorio.saveAndFlush(entidad);
    }

    @Override
    public Optional<CasoDeEvaluacion> buscarPorPreguntaId(String preguntaId) {
        return repositorio.findByQuestionId(preguntaId).map(ReviewCaseMapper::aDominio);
    }

    @Override
    public Optional<CasoDeEvaluacion> buscarPorPreguntaIdParaActualizar(String preguntaId) {
        return repositorio.findByQuestionIdForUpdate(preguntaId).map(ReviewCaseMapper::aDominio);
    }
}
