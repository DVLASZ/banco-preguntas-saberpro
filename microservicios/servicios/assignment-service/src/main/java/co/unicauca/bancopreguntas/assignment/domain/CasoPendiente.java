package co.unicauca.bancopreguntas.assignment.domain;

import java.time.Instant;

/** Una pregunta que espera que el administrador le asigne revisores (HU-04). */
public record CasoPendiente(String preguntaId, String titulo, String competencia, String autorId, int ronda,
                            Instant enviadaEn) {
}
