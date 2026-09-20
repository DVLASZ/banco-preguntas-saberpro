package co.unicauca.saberpro.revision.access;

import co.unicauca.saberpro.revision.domain.AsignacionRevision;
import co.unicauca.saberpro.revision.domain.AsignacionRevisionRepository;

import java.util.List;

/**
 * Implementación en memoria de {@link AsignacionRevisionRepository}, igual que
 * {@code QuestionImplRepository} en modulo-preguntas (una estructura simple; si
 * queda tiempo se puede pasar a SQLite siguiendo el patrón de modulo-usuarios).
 *
 * <p>TODO(HU-04): guardar las asignaciones en una lista y filtrarlas por
 * pregunta o por revisor.
 */
public class AsignacionRevisionImplRepository implements AsignacionRevisionRepository {

    @Override
    public void guardar(AsignacionRevision asignacion) {
        throw new UnsupportedOperationException("HU-04: implementar guardar");
    }

    @Override
    public List<AsignacionRevision> obtenerPorPregunta(String idPregunta) {
        throw new UnsupportedOperationException("HU-04: implementar obtenerPorPregunta");
    }

    @Override
    public List<AsignacionRevision> obtenerPorRevisor(String usuarioRevisor) {
        throw new UnsupportedOperationException("HU-04: implementar obtenerPorRevisor");
    }
}
