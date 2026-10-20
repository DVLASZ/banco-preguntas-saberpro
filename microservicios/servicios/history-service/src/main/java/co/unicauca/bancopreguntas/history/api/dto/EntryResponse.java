package co.unicauca.bancopreguntas.history.api.dto;

import co.unicauca.bancopreguntas.history.domain.EntradaDeHistorial;
import co.unicauca.bancopreguntas.history.domain.TipoEntrada;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.Instant;

/** Una entrada del historial. {@code detalles} es el cuerpo del evento original. */
public record EntryResponse(String id, TipoEntrada tipo, String preguntaId, String actorId, String resumen,
                            JsonNode detalles, Instant ocurridaEn) {

    private static final ObjectMapper JSON = new ObjectMapper();

    public static EntryResponse de(EntradaDeHistorial e) {
        return new EntryResponse(e.id(), e.tipo(), e.preguntaId(), e.actorId(), e.resumen(), leer(e.detalles()),
                e.ocurridaEn());
    }

    private static JsonNode leer(String texto) {
        if (texto == null || texto.isBlank()) {
            return null;
        }
        try {
            return JSON.readTree(texto);
        } catch (JsonProcessingException ex) {
            return JSON.getNodeFactory().textNode(texto);
        }
    }
}
