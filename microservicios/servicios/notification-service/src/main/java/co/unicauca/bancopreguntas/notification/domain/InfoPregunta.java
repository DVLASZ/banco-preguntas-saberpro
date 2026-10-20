package co.unicauca.bancopreguntas.notification.domain;

/**
 * Lo mínimo que este servicio recuerda de una pregunta para redactar sus avisos: el título, el autor y el
 * administrador que asignó los revisores. Se aprende de {@code question.submitted} y de
 * {@code review.reviewers-assigned}, de modo que no hace falta consultar a los otros servicios.
 *
 * @param administradorId quien asignó los revisores; {@code null} mientras no se hayan asignado
 */
public record InfoPregunta(String preguntaId, String titulo, String autorId, String administradorId) {

    public InfoPregunta(String preguntaId, String titulo, String autorId) {
        this(preguntaId, titulo, autorId, null);
    }

    public InfoPregunta conAdministrador(String administrador) {
        return new InfoPregunta(preguntaId, titulo, autorId, administrador);
    }
}
