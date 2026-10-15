package co.unicauca.bancopreguntas.review.api.dto;

/** Límites de revisores por pregunta: el mínimo es siempre 1 y el máximo es configurable. */
public record ReviewConfigResponse(int minimoDeRevisores, int maximoDeRevisores) {
}
