package co.unicauca.bancopreguntas.audit.domain;

import java.time.Instant;

/** Criterios opcionales para consultar la auditoría; los nulos no filtran. */
public record FiltroAuditoria(TipoEntrada tipo, String actorId, String preguntaId, Instant desde, Instant hasta) {

    public FiltroAuditoria {
        if (desde != null && hasta != null && hasta.isBefore(desde)) {
            throw new IllegalArgumentException("La fecha final no puede ser anterior a la inicial");
        }
        actorId = actorId == null || actorId.isBlank() ? null : actorId.trim();
        preguntaId = preguntaId == null || preguntaId.isBlank() ? null : preguntaId.trim();
    }

    public static FiltroAuditoria sinFiltros() {
        return new FiltroAuditoria(null, null, null, null, null);
    }
}
