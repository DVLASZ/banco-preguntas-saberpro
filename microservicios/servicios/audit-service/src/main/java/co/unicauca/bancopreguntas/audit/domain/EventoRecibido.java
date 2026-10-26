package co.unicauca.bancopreguntas.audit.domain;

import java.time.Instant;

/** Los datos comunes del sobre de un evento: su identificador, cuándo ocurrió y su cuerpo en JSON. */
public record EventoRecibido(String eventId, Instant ocurridaEn, String detalles) {
}
