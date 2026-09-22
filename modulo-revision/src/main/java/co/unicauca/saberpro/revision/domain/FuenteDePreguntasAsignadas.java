package co.unicauca.saberpro.revision.domain;

import co.unicauca.saberpro.preguntas.domain.FuenteDePreguntasParaRevisar;
import co.unicauca.saberpro.preguntas.domain.Question;

import java.util.List;

/**
 * Fuente de preguntas del Revisor una vez existe la asignación (HU-04): solo
 * las que el Administrador le asignó. Reemplaza a
 * {@code TodasLasPreguntasEnRevision} en {@code MainApp} cuando esté lista.
 */
public class FuenteDePreguntasAsignadas implements FuenteDePreguntasParaRevisar {

    private final AsignacionRevisionService servicio;

    public FuenteDePreguntasAsignadas(AsignacionRevisionService servicio) {
        this.servicio = servicio;
    }

    @Override
    public List<Question> paraRevisor(String usuarioRevisor) {
        return servicio.preguntasAsignadas(usuarioRevisor);
    }
}

