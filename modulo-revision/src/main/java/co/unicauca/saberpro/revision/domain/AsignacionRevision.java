package co.unicauca.saberpro.revision.domain;

import java.time.LocalDateTime;

/**
 * Asignación de un revisor a una pregunta (HU-04, RF-16): quién la revisa,
 * quién la asignó y cuándo.
 */
public class AsignacionRevision {

    private final String idPregunta;
    private final String usuarioRevisor;
    private final String asignadoPor;
    private final LocalDateTime fecha;

    public AsignacionRevision(String idPregunta, String usuarioRevisor, String asignadoPor, LocalDateTime fecha) {
        if (idPregunta == null || idPregunta.isBlank()) {
            throw new IllegalArgumentException("El id de la pregunta es obligatorio");
        }
        if (usuarioRevisor == null || usuarioRevisor.isBlank()) {
            throw new IllegalArgumentException("El revisor es obligatorio");
        }
        if (asignadoPor == null || asignadoPor.isBlank()) {
            throw new IllegalArgumentException("Quien asigna es obligatorio");
        }
        if (fecha == null) {
            throw new IllegalArgumentException("La fecha de la asignación es obligatoria");
        }
        this.idPregunta = idPregunta;
        this.usuarioRevisor = usuarioRevisor;
        this.asignadoPor = asignadoPor;
        this.fecha = fecha;
    }

    public String getIdPregunta() {
        return idPregunta;
    }

    public String getUsuarioRevisor() {
        return usuarioRevisor;
    }

    public String getAsignadoPor() {
        return asignadoPor;
    }

    public LocalDateTime getFecha() {
        return fecha;
    }
}
