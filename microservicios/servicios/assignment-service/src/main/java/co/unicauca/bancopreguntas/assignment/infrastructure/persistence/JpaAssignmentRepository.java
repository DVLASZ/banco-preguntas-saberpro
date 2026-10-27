package co.unicauca.bancopreguntas.assignment.infrastructure.persistence;

import co.unicauca.bancopreguntas.assignment.application.port.AssignmentRepository;
import co.unicauca.bancopreguntas.assignment.domain.AsignacionDeRevisor;
import co.unicauca.bancopreguntas.assignment.domain.CasoDeAsignacion;
import co.unicauca.bancopreguntas.assignment.domain.CasoPendiente;
import co.unicauca.bancopreguntas.assignment.domain.Decision;
import co.unicauca.bancopreguntas.assignment.domain.EstadoRonda;
import co.unicauca.bancopreguntas.assignment.domain.FiltroAsignacion;
import co.unicauca.bancopreguntas.assignment.domain.Pagina;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.util.Optional;

/** Adaptador de persistencia del puerto {@link AssignmentRepository} (patrón Adapter). */
@Component
public class JpaAssignmentRepository implements AssignmentRepository {

    private final CaseJpaRepository repositorio;

    public JpaAssignmentRepository(CaseJpaRepository repositorio) {
        this.repositorio = repositorio;
    }

    @Override
    public void guardar(CasoDeAsignacion caso) {
        CaseEntity entidad = repositorio.findByQuestionId(caso.preguntaId()).orElseGet(CaseEntity::new);
        CaseMapper.sincronizar(caso, entidad);
        repositorio.saveAndFlush(entidad);
    }

    @Override
    public Optional<CasoDeAsignacion> buscarPorPreguntaId(String preguntaId) {
        return repositorio.findByQuestionId(preguntaId).map(CaseMapper::aDominio);
    }

    @Override
    public Optional<CasoDeAsignacion> buscarPorPreguntaIdParaActualizar(String preguntaId) {
        return repositorio.findByQuestionIdForUpdate(preguntaId).map(CaseMapper::aDominio);
    }

    @Override
    public Pagina<CasoPendiente> pendientesDeAsignacion(int pagina, int tamano) {
        Page<PendingRow> resultado = repositorio.pendingAssignment(PageRequest.of(pagina - 1, tamano));
        return new Pagina<>(resultado.getContent().stream()
                .map(f -> new CasoPendiente(f.questionId(), f.title(), f.competency(), f.authorId(), f.round(),
                        f.startedAt()))
                .toList(), pagina, tamano, (int) resultado.getTotalElements());
    }

    @Override
    public Pagina<AsignacionDeRevisor> asignadasA(String revisorId, FiltroAsignacion filtro, int pagina, int tamano) {
        Page<InboxRow> resultado = repositorio.inbox(revisorId, filtro.name(), PageRequest.of(pagina - 1, tamano));
        return new Pagina<>(resultado.getContent().stream()
                .map(f -> new AsignacionDeRevisor(f.questionId(), f.title(), f.competency(), f.authorId(), f.round(),
                        f.assignedAt(), EstadoRonda.valueOf(f.roundState()),
                        f.decision() == null ? null : Decision.valueOf(f.decision())))
                .toList(), pagina, tamano, (int) resultado.getTotalElements());
    }
}
