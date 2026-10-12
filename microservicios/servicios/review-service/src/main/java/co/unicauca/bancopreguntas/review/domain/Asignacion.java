package co.unicauca.bancopreguntas.review.domain;

import java.time.Instant;

/**
 * Un revisor asignado a una ronda.
 *
 * @param asignadoPor quién lo asignó: un administrador, o {@value #SISTEMA} cuando se reasignó solo en una ronda nueva
 * @param automatica  si el sistema lo reasignó automáticamente al empezar una nueva ronda
 */
public record Asignacion(String revisorId, String asignadoPor, Instant asignadoEn, boolean automatica) {

    public static final String SISTEMA = "system";

    public Asignacion {
        if (revisorId == null || revisorId.isBlank()) {
            throw new IllegalArgumentException("El revisor es obligatorio");
        }
        if (asignadoPor == null || asignadoPor.isBlank()) {
            throw new IllegalArgumentException("Quién asigna es obligatorio");
        }
        if (asignadoEn == null) {
            throw new IllegalArgumentException("La fecha de asignación es obligatoria");
        }
    }
}
