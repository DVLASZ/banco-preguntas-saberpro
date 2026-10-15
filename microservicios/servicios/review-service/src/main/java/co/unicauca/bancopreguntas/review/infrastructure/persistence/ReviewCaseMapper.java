package co.unicauca.bancopreguntas.review.infrastructure.persistence;

import co.unicauca.bancopreguntas.review.domain.Asignacion;
import co.unicauca.bancopreguntas.review.domain.CasoDeRevision;
import co.unicauca.bancopreguntas.review.domain.Decision;
import co.unicauca.bancopreguntas.review.domain.EstadoRonda;
import co.unicauca.bancopreguntas.review.domain.Evaluacion;
import co.unicauca.bancopreguntas.review.domain.RondaDeRevision;

import java.util.List;
import java.util.UUID;

/**
 * Convierte entre el agregado {@link CasoDeRevision} y sus filas. Al guardar, las asignaciones y las
 * evaluaciones solo se agregan: las que ya existen no se tocan.
 */
final class ReviewCaseMapper {

    private ReviewCaseMapper() {
    }

    static CasoDeRevision aDominio(ReviewCaseEntity entidad) {
        List<RondaDeRevision> rondas = entidad.getRounds().stream().map(ReviewCaseMapper::rondaADominio).toList();
        return CasoDeRevision.restaurar(entidad.getId(), entidad.getQuestionId(), entidad.getAuthorId(),
                entidad.getTitle(), entidad.getCompetency(), entidad.getCreatedAt(), entidad.getUpdatedAt(), rondas);
    }

    private static RondaDeRevision rondaADominio(ReviewRoundEntity r) {
        List<Asignacion> asignaciones = r.getAssignments().stream()
                .map(a -> new Asignacion(a.getReviewerId(), a.getAssignedBy(), a.getAssignedAt(), a.isAutomatic()))
                .toList();
        List<Evaluacion> evaluaciones = r.getEvaluations().stream()
                .map(e -> new Evaluacion(e.getReviewerId(), Decision.valueOf(e.getDecision()), e.getObservation(),
                        e.getSubmittedAt(), e.isLate()))
                .toList();
        return RondaDeRevision.restaurar(r.getNumber(), r.getQuestionVersion(), EstadoRonda.valueOf(r.getState()),
                r.getStartedAt(), r.getDecidedAt(), r.getClosedAt(), asignaciones, evaluaciones);
    }

    /** Copia el estado del agregado a la entidad (nueva o existente). */
    static void sincronizar(CasoDeRevision caso, ReviewCaseEntity entidad) {
        entidad.setId(caso.id());
        entidad.setQuestionId(caso.preguntaId());
        entidad.setAuthorId(caso.autorId());
        entidad.setTitle(caso.titulo());
        entidad.setCompetency(caso.competencia());
        entidad.setCreatedAt(caso.creadoEn());
        entidad.setUpdatedAt(caso.actualizadoEn());
        for (RondaDeRevision ronda : caso.rondas()) {
            ReviewRoundEntity fila = entidad.getRounds().stream()
                    .filter(r -> r.getNumber() == ronda.numero())
                    .findFirst()
                    .orElseGet(() -> {
                        ReviewRoundEntity nueva = new ReviewRoundEntity(UUID.randomUUID().toString(), entidad);
                        nueva.setNumber(ronda.numero());
                        entidad.getRounds().add(nueva);
                        return nueva;
                    });
            sincronizar(ronda, fila);
        }
    }

    private static void sincronizar(RondaDeRevision ronda, ReviewRoundEntity fila) {
        fila.setQuestionVersion(ronda.versionDePregunta());
        fila.setState(ronda.estado().name());
        fila.setStartedAt(ronda.iniciadaEn());
        fila.setDecidedAt(ronda.decididaEn().orElse(null));
        fila.setClosedAt(ronda.cerradaEn().orElse(null));
        for (Asignacion a : ronda.asignaciones()) {
            boolean existe = fila.getAssignments().stream().anyMatch(x -> x.getReviewerId().equals(a.revisorId()));
            if (!existe) {
                fila.getAssignments().add(
                        new AssignmentEntity(fila, a.revisorId(), a.asignadoPor(), a.asignadoEn(), a.automatica()));
            }
        }
        for (Evaluacion e : ronda.evaluaciones()) {
            boolean existe = fila.getEvaluations().stream().anyMatch(x -> x.getReviewerId().equals(e.revisorId()));
            if (!existe) {
                fila.getEvaluations().add(new EvaluationEntity(fila, e.revisorId(), e.decision().name(),
                        e.observacion(), e.enviadaEn(), e.tardia()));
            }
        }
    }
}
