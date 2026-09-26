package co.unicauca.saberpro.revision.access;

import co.unicauca.saberpro.revision.domain.AsignacionRevision;
import co.unicauca.saberpro.revision.domain.AsignacionRevisionRepository;

import java.util.ArrayList;
import java.util.List;

/**
 * Implementación en memoria de {@link AsignacionRevisionRepository}, igual que
 * {@code QuestionImplRepository} en modulo-preguntas (una estructura simple; si
 * queda tiempo se puede pasar a SQLite siguiendo el patrón de modulo-usuarios).
 */
public class AsignacionRevisionImplRepository implements AsignacionRevisionRepository {

    private final List<AsignacionRevision> asignaciones = new ArrayList<>();

    @Override
    public void guardar(AsignacionRevision asignacion) {
        asignaciones.add(asignacion);
    }

    @Override
    public List<AsignacionRevision> obtenerPorPregunta(String idPregunta) {
        List<AsignacionRevision> resultado = new ArrayList<>();
        for (AsignacionRevision asignacion : asignaciones) {
            if (asignacion.getIdPregunta().equals(idPregunta)) {
                resultado.add(asignacion);
            }
        }
        return resultado;
    }

    @Override
    public List<AsignacionRevision> obtenerPorRevisor(String usuarioRevisor) {
        List<AsignacionRevision> resultado = new ArrayList<>();
        for (AsignacionRevision asignacion : asignaciones) {
            if (asignacion.getUsuarioRevisor().equals(usuarioRevisor)) {
                resultado.add(asignacion);
            }
        }
        return resultado;
    }
}
