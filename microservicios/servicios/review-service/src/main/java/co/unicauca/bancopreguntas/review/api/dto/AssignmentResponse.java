package co.unicauca.bancopreguntas.review.api.dto;

import co.unicauca.bancopreguntas.review.domain.AsignacionDeRevisor;
import co.unicauca.bancopreguntas.review.domain.Decision;
import co.unicauca.bancopreguntas.review.domain.EstadoRonda;

import java.time.Instant;

/** Una pregunta de la bandeja de un revisor. */
public record AssignmentResponse(String preguntaId, String titulo, String competencia, String autorId, int ronda,
                                 Instant asignadaEn, EstadoRonda estadoRonda, Decision miDecision, boolean pendiente) {

    public static AssignmentResponse de(AsignacionDeRevisor a) {
        return new AssignmentResponse(a.preguntaId(), a.titulo(), a.competencia(), a.autorId(), a.ronda(),
                a.asignadaEn(), a.estadoRonda(), a.miDecision(), a.pendiente());
    }
}
