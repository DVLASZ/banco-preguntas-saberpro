package co.unicauca.bancopreguntas.history.domain;

/** Lo que este servicio recuerda de una pregunta para decidir quién puede ver su historial. */
public record InfoPregunta(String preguntaId, String titulo, String autorId) {
}
