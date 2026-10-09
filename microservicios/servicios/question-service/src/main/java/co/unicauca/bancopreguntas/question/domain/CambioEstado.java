package co.unicauca.bancopreguntas.question.domain;

import java.time.Instant;

/**
 * Una entrada del historial de estados de una pregunta (trazabilidad, RF-29 y RF-30).
 *
 * @param desde        estado anterior, o {@code null} cuando es la creación de la pregunta
 * @param hacia        estado al que pasó
 * @param cambiadoPor  usuario responsable, o {@code null} cuando el cambio lo provocó un evento
 *                     de otro servicio (por ejemplo, el resultado de la revisión)
 * @param fecha        momento del cambio
 */
public record CambioEstado(EstadoPregunta desde, EstadoPregunta hacia, String cambiadoPor, Instant fecha) {

    public CambioEstado {
        if (hacia == null) {
            throw new IllegalArgumentException("El estado de destino es obligatorio");
        }
        if (fecha == null) {
            throw new IllegalArgumentException("La fecha del cambio es obligatoria");
        }
    }
}
