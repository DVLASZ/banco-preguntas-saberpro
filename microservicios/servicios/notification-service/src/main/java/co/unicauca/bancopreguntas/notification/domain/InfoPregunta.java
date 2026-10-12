package co.unicauca.bancopreguntas.notification.domain;

/**
 * Lo mínimo que este servicio recuerda de una pregunta para redactar sus avisos: el título y el autor. Se
 * aprende de {@code question.submitted}, de modo que no hace falta consultar al servicio de preguntas.
 */
public record InfoPregunta(String preguntaId, String titulo, String autorId) {
}
