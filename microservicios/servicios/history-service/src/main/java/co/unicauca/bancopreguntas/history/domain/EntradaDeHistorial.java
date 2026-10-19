package co.unicauca.bancopreguntas.history.domain;

import java.time.Instant;

/**
 * Un hecho del historial: quién hizo qué y cuándo. Nunca se modifica ni se borra.
 *
 * @param eventId    el evento del que viene; evita registrar dos veces el mismo hecho
 * @param preguntaId la pregunta a la que se refiere, o nulo si es de seguridad (inicios de sesión)
 * @param actorId    quien lo hizo, o nulo si fue el sistema
 * @param detalles   el cuerpo del evento tal como llegó, en JSON
 */
public record EntradaDeHistorial(String id, String eventId, String preguntaId, String actorId, TipoEntrada tipo,
                                 String resumen, String detalles, Instant ocurridaEn, Instant registradaEn) {

    public EntradaDeHistorial {
        if (id == null || id.isBlank() || eventId == null || eventId.isBlank()) {
            throw new IllegalArgumentException("El identificador de la entrada y del evento son obligatorios");
        }
        if (tipo == null) {
            throw new IllegalArgumentException("El tipo de entrada es obligatorio");
        }
        if (resumen == null || resumen.isBlank()) {
            throw new IllegalArgumentException("El resumen es obligatorio");
        }
        if (ocurridaEn == null || registradaEn == null) {
            throw new IllegalArgumentException("Las fechas son obligatorias");
        }
        if (preguntaId == null && !tipo.esDeSeguridad()) {
            throw new IllegalArgumentException("Una entrada de pregunta debe indicar la pregunta");
        }
    }
}
