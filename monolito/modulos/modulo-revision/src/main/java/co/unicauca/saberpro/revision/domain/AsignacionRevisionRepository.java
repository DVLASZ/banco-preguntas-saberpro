package co.unicauca.saberpro.revision.domain;

import java.util.List;

/**
 * Contrato de persistencia de las asignaciones. El servicio depende de esta
 * abstracción, nunca de una implementación concreta (Inversión de Dependencias),
 * igual que {@code QuestionRepository} en modulo-preguntas.
 */
public interface AsignacionRevisionRepository {

    /** Guarda una asignación nueva. */
    void guardar(AsignacionRevision asignacion);

    /** Todas las asignaciones de una pregunta (puede tener varios revisores). */
    List<AsignacionRevision> obtenerPorPregunta(String idPregunta);

    /** Todas las asignaciones de un revisor. */
    List<AsignacionRevision> obtenerPorRevisor(String usuarioRevisor);
}
