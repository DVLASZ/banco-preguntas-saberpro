package co.unicauca.bancopreguntas.assignment.api.dto;

import co.unicauca.bancopreguntas.assignment.domain.Asignacion;
import co.unicauca.bancopreguntas.assignment.domain.CasoDeAsignacion;
import co.unicauca.bancopreguntas.assignment.domain.Decision;
import co.unicauca.bancopreguntas.assignment.domain.EstadoRonda;
import co.unicauca.bancopreguntas.assignment.domain.RondaDeAsignacion;

import java.time.Instant;
import java.util.List;

/** Las rondas de una pregunta con los revisores asignados (sin las evaluaciones, que son de evaluation-service). */
public record CaseAssignmentsResponse(String preguntaId, String autorId, String titulo, String competencia,
                                      List<RoundResponse> rondas) {

    public static CaseAssignmentsResponse de(CasoDeAsignacion c) {
        return new CaseAssignmentsResponse(c.preguntaId(), c.autorId(), c.titulo(), c.competencia(),
                c.rondas().stream().map(RoundResponse::de).toList());
    }

    public record RoundResponse(int numero, int versionDePregunta, EstadoRonda estado, Instant iniciadaEn,
                                Instant decididaEn, Instant cerradaEn, int totalDeRevisores,
                                List<ReviewerResponse> revisores) {

        static RoundResponse de(RondaDeAsignacion r) {
            return new RoundResponse(r.numero(), r.versionDePregunta(), r.estado(), r.iniciadaEn(),
                    r.decididaEn().orElse(null), r.cerradaEn().orElse(null), r.cantidadDeRevisores(),
                    r.asignaciones().stream().map(a -> ReviewerResponse.de(a, r.decisionDe(a.revisorId()).orElse(null))).toList());
        }
    }

    /** Un revisor de la ronda; {@code decision} es nula mientras no haya evaluado. */
    public record ReviewerResponse(String revisorId, String asignadoPor, Instant asignadoEn, boolean automatica,
                                   Decision decision) {

        static ReviewerResponse de(Asignacion a, Decision decision) {
            return new ReviewerResponse(a.revisorId(), a.asignadoPor(), a.asignadoEn(), a.automatica(), decision);
        }
    }
}
