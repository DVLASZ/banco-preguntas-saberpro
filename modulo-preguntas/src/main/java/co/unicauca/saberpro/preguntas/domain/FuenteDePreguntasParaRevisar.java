package co.unicauca.saberpro.preguntas.domain;

import java.util.List;

/**
 * De dónde saca el Revisor las preguntas que puede abrir para evaluar.
 *
 * <p>{@link TodasLasPreguntasEnRevision} es la fuente por defecto (todas las
 * preguntas que esperan revisión). Con la asignación de revisores (HU-04) se
 * reemplaza por una fuente que devuelve solo las preguntas asignadas al
 * revisor, sin que {@code GUIRevisor} tenga que cambiar.
 */
public interface FuenteDePreguntasParaRevisar {

    /** Las preguntas que el revisor con ese nombre de usuario puede abrir. */
    List<Question> paraRevisor(String usuarioRevisor);
}
