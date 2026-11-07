package co.unicauca.bancopreguntas.evaluation.domain;

import java.util.Optional;

/**
 * Objeto de valor: el comentario que un revisor deja junto a su decisión. Siempre llega sin espacios sobrantes, no
 * está vacío y no pasa del máximo. Dos observaciones con el mismo texto son la misma.
 */
public record Observacion(String texto) {

    /** Longitud máxima de una observación. */
    public static final int MAXIMO = 4000;

    public Observacion {
        if (texto == null || texto.isBlank()) {
            throw new RevisionInvalidaException("La observación no puede estar vacía");
        }
        texto = texto.trim();
        if (texto.length() > MAXIMO) {
            throw new RevisionInvalidaException("La observación no puede superar " + MAXIMO + " caracteres");
        }
    }

    /**
     * La observación que corresponde a un texto escrito por el revisor; un texto vacío o en blanco significa que no
     * dejó ninguna.
     *
     * @throws RevisionInvalidaException si el texto es demasiado largo
     */
    public static Optional<Observacion> de(String texto) {
        return texto == null || texto.isBlank() ? Optional.empty() : Optional.of(new Observacion(texto));
    }
}
