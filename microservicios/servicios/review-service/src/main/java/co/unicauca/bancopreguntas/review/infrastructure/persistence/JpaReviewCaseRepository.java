package co.unicauca.bancopreguntas.review.infrastructure.persistence;

import co.unicauca.bancopreguntas.review.application.port.ReviewCaseRepository;
import co.unicauca.bancopreguntas.review.domain.AsignacionDeRevisor;
import co.unicauca.bancopreguntas.review.domain.CasoDeRevision;
import co.unicauca.bancopreguntas.review.domain.CasoPendiente;
import co.unicauca.bancopreguntas.review.domain.Decision;
import co.unicauca.bancopreguntas.review.domain.EstadoRonda;
import co.unicauca.bancopreguntas.review.domain.FiltroAsignacion;
import co.unicauca.bancopreguntas.review.domain.Pagina;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.util.Optional;

/** Adaptador de persistencia del puerto {@link ReviewCaseRepository}. */
@Component
public class JpaReviewCaseRepository implements ReviewCaseRepository {

    private final ReviewCaseJpaRepository repositorio;

    public JpaReviewCaseRepository(ReviewCaseJpaRepository repositorio) {
        this.repositorio = repositorio;
    }

    @Override
    public void guardar(CasoDeRevision caso) {
        ReviewCaseEntity entidad = repositorio.findByQuestionId(caso.preguntaId()).orElseGet(ReviewCaseEntity::new);
        ReviewCaseMapper.sincronizar(caso, entidad);
        repositorio.saveAndFlush(entidad);
    }

    @Override
    public Optional<CasoDeRevision> buscarPorPreguntaId(String preguntaId) {
        return repositorio.findByQuestionId(preguntaId).map(ReviewCaseMapper::aDominio);
    }

    @Override
    public Optional<CasoDeRevision> buscarPorPreguntaIdParaActualizar(String preguntaId) {
        return repositorio.findByQuestionIdForUpdate(preguntaId).map(ReviewCaseMapper::aDominio);
    }

    @Override
    public Pagina<CasoPendiente> pendientesDeAsignacion(int pagina, int tamano) {
        Page<PendingRow> resultado =
                repositorio.pendingAssignment(PageRequest.of(pagina - 1, tamano));
        return new Pagina<>(resultado.getContent().stream()
                .map(f -> new CasoPendiente(f.questionId(), f.title(), f.competency(), f.authorId(), f.round(),
                        f.startedAt()))
                .toList(), pagina, tamano, (int) resultado.getTotalElements());
    }

    @Override
    public Pagina<AsignacionDeRevisor> asignadasA(String revisorId, FiltroAsignacion filtro, int pagina, int tamano) {
        Page<InboxRow> resultado =
                repositorio.inbox(revisorId, filtro.name(), PageRequest.of(pagina - 1, tamano));
        return new Pagina<>(resultado.getContent().stream()
                .map(f -> new AsignacionDeRevisor(f.questionId(), f.title(), f.competency(), f.authorId(), f.round(),
                        f.assignedAt(), EstadoRonda.valueOf(f.roundState()),
                        f.decision() == null ? null : Decision.valueOf(f.decision())))
                .toList(), pagina, tamano, (int) resultado.getTotalElements());
    }
}
