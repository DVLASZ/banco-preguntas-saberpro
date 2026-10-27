package co.unicauca.bancopreguntas.evaluation.api.dto;

import co.unicauca.bancopreguntas.evaluation.domain.Decision;
import co.unicauca.bancopreguntas.evaluation.domain.EstadoRonda;
import co.unicauca.bancopreguntas.evaluation.domain.Evaluacion;
import co.unicauca.bancopreguntas.evaluation.domain.VistaDelCaso;

import java.time.Instant;
import java.util.List;

/** La revisión de una pregunta tal como la ve quien la consulta. */
public record ReviewResponse(String preguntaId, String autorId, String titulo, String competencia,
                             List<RoundResponse> rondas) {

    public static ReviewResponse de(VistaDelCaso v) {
        return new ReviewResponse(v.preguntaId(), v.autorId(), v.titulo(), v.competencia(),
                v.rondas().stream().map(RoundResponse::de).toList());
    }

    public record RoundResponse(int numero, int versionDePregunta, EstadoRonda estado, Instant iniciadaEn,
                                Instant decididaEn, Instant cerradaEn, int totalDeRevisores, int totalDeEvaluadas,
                                boolean detalleVisible, List<ReviewerResponse> revisores) {

        static RoundResponse de(VistaDelCaso.VistaDeRonda r) {
            return new RoundResponse(r.numero(), r.versionDePregunta(), r.estado(), r.iniciadaEn(), r.decididaEn(),
                    r.cerradaEn(), r.totalDeRevisores(), r.totalDeEvaluadas(), r.detalleVisible(),
                    r.revisores().stream().map(ReviewerResponse::de).toList());
        }
    }

    public record ReviewerResponse(String revisorId, String asignadoPor, Instant asignadoEn, boolean automatica,
                                   EvaluationResponse evaluacion) {

        static ReviewerResponse de(VistaDelCaso.VistaDeRevisor r) {
            return new ReviewerResponse(r.revisorId(), r.asignadoPor(), r.asignadoEn(), r.automatica(),
                    r.evaluacion() == null ? null : EvaluationResponse.de(r.evaluacion()));
        }
    }

    public record EvaluationResponse(Decision decision, String observacion, Instant enviadaEn, boolean tardia) {

        static EvaluationResponse de(Evaluacion e) {
            return new EvaluationResponse(e.decision(), e.observacion(), e.enviadaEn(), e.tardia());
        }
    }
}
