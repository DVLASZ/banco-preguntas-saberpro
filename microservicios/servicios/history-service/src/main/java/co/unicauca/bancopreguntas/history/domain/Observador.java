package co.unicauca.bancopreguntas.history.domain;

/**
 * Quien consulta el historial y con qué roles: determina qué parte puede ver. El administrador ve todo; el
 * autor, sus propias preguntas; el revisor, las preguntas que revisa o revisó.
 */
public record Observador(String usuarioId, boolean administrador, boolean autor, boolean revisor) {

    public Observador {
        if (usuarioId == null || usuarioId.isBlank()) {
            throw new IllegalArgumentException("El usuario es obligatorio");
        }
    }
}
