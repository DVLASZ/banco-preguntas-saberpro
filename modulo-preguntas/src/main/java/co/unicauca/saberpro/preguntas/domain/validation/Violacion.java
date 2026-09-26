package co.unicauca.saberpro.preguntas.domain.validation;

/**
 * Un incumplimiento de la validación estructural de una pregunta: el
 * {@code campo} del formulario afectado (para poder resaltarlo en la
 * interfaz) y el {@code mensaje} que se le muestra al Autor.
 */
public record Violacion(String campo, String mensaje) {
}
