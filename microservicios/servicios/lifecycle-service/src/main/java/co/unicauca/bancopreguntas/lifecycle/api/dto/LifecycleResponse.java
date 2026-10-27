package co.unicauca.bancopreguntas.lifecycle.api.dto;

import co.unicauca.bancopreguntas.lifecycle.domain.CicloDeVida;
import co.unicauca.bancopreguntas.lifecycle.domain.EstadoPregunta;

import java.time.Instant;

/** El estado de una pregunta: en qué fase está, qué versión de su contenido, y si todavía se puede editar. */
public record LifecycleResponse(String preguntaId, String autorId, EstadoPregunta estado, int version, int secuencia,
                                boolean editable, Instant actualizadoEn) {

    public static LifecycleResponse de(CicloDeVida c) {
        return new LifecycleResponse(c.getPreguntaId(), c.getAutorId(), c.getEstado(), c.getVersion(), c.getSecuencia(),
                c.esEditable(), c.getActualizadoEn());
    }
}
