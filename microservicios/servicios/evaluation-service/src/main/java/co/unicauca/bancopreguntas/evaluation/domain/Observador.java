package co.unicauca.bancopreguntas.evaluation.domain;

/** Quien consulta una revisión: determina qué parte de ella puede ver. */
public record Observador(String id, boolean administrador) {

    public Observador {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("El identificador del observador es obligatorio");
        }
    }
}
