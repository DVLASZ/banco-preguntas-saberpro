package co.unicauca.bancopreguntas.question.api.dto;

import co.unicauca.bancopreguntas.question.domain.CambioEstado;
import co.unicauca.bancopreguntas.question.domain.EstadoPregunta;

import java.time.Instant;

/** Una entrada del historial de estados de una pregunta. */
public record CambioEstadoResponse(EstadoPregunta desde, EstadoPregunta hacia, String cambiadoPor, Instant fecha) {

    public static CambioEstadoResponse de(CambioEstado c) {
        return new CambioEstadoResponse(c.desde(), c.hacia(), c.cambiadoPor(), c.fecha());
    }
}
