package co.unicauca.bancopreguntas.review.api.dto;

import co.unicauca.bancopreguntas.review.domain.CasoPendiente;

import java.time.Instant;

/** Una pregunta que espera que se le asignen revisores. */
public record PendingCaseResponse(String preguntaId, String titulo, String competencia, String autorId, int ronda,
                                  Instant enviadaEn) {

    public static PendingCaseResponse de(CasoPendiente c) {
        return new PendingCaseResponse(c.preguntaId(), c.titulo(), c.competencia(), c.autorId(), c.ronda(), c.enviadaEn());
    }
}
