package co.unicauca.bancopreguntas.assignment.infrastructure.persistence;

import co.unicauca.bancopreguntas.assignment.domain.Asignacion;
import co.unicauca.bancopreguntas.assignment.domain.CasoDeAsignacion;
import co.unicauca.bancopreguntas.assignment.domain.Decision;
import co.unicauca.bancopreguntas.assignment.domain.EstadoRonda;
import co.unicauca.bancopreguntas.assignment.domain.RondaDeAsignacion;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Convierte entre el agregado {@link CasoDeAsignacion} y sus filas. Al guardar, las asignaciones solo se agregan: las
 * que ya existen conservan sus datos y solo reciben la decisión del revisor cuando llega.
 */
final class CaseMapper {

    private CaseMapper() {
    }

    static CasoDeAsignacion aDominio(CaseEntity entidad) {
        List<RondaDeAsignacion> rondas = entidad.getRounds().stream().map(CaseMapper::rondaADominio).toList();
        return CasoDeAsignacion.restaurar(entidad.getId(), entidad.getQuestionId(), entidad.getAuthorId(),
                entidad.getTitle(), entidad.getCompetency(), entidad.getCreatedAt(), entidad.getUpdatedAt(), rondas);
    }

    private static RondaDeAsignacion rondaADominio(RoundEntity r) {
        List<Asignacion> asignaciones = r.getAssignments().stream()
                .map(a -> new Asignacion(a.getReviewerId(), a.getAssignedBy(), a.getAssignedAt(), a.isAutomatic()))
                .toList();
        Map<String, Decision> decisiones = new LinkedHashMap<>();
        r.getAssignments().stream().filter(a -> a.getDecision() != null)
                .forEach(a -> decisiones.put(a.getReviewerId(), Decision.valueOf(a.getDecision())));
        return RondaDeAsignacion.restaurar(r.getNumber(), r.getQuestionVersion(), EstadoRonda.valueOf(r.getState()),
                r.getStartedAt(), r.getDecidedAt(), r.getClosedAt(), asignaciones, decisiones);
    }

    /** Copia el estado del agregado a la entidad (nueva o existente). */
    static void sincronizar(CasoDeAsignacion caso, CaseEntity entidad) {
        entidad.setId(caso.id());
        entidad.setQuestionId(caso.preguntaId());
        entidad.setAuthorId(caso.autorId());
        entidad.setTitle(caso.titulo());
        entidad.setCompetency(caso.competencia());
        entidad.setCreatedAt(caso.creadoEn());
        entidad.setUpdatedAt(caso.actualizadoEn());
        for (RondaDeAsignacion ronda : caso.rondas()) {
            RoundEntity fila = entidad.getRounds().stream()
                    .filter(r -> r.getNumber() == ronda.numero())
                    .findFirst()
                    .orElseGet(() -> {
                        RoundEntity nueva = new RoundEntity(UUID.randomUUID().toString(), entidad);
                        nueva.setNumber(ronda.numero());
                        entidad.getRounds().add(nueva);
                        return nueva;
                    });
            sincronizar(ronda, fila);
        }
    }

    private static void sincronizar(RondaDeAsignacion ronda, RoundEntity fila) {
        fila.setQuestionVersion(ronda.versionDePregunta());
        fila.setState(ronda.estado().name());
        fila.setStartedAt(ronda.iniciadaEn());
        fila.setDecidedAt(ronda.decididaEn().orElse(null));
        fila.setClosedAt(ronda.cerradaEn().orElse(null));
        for (Asignacion a : ronda.asignaciones()) {
            AssignmentEntity existente = fila.getAssignments().stream()
                    .filter(x -> x.getReviewerId().equals(a.revisorId()))
                    .findFirst()
                    .orElseGet(() -> {
                        AssignmentEntity nueva = new AssignmentEntity(fila, a.revisorId(), a.asignadoPor(), a.asignadoEn(),
                                a.automatica());
                        fila.getAssignments().add(nueva);
                        return nueva;
                    });
            ronda.decisionDe(a.revisorId()).ifPresent(d -> existente.setDecision(d.name()));
        }
    }
}
