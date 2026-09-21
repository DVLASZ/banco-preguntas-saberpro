package co.unicauca.saberpro.preguntas.presentation;

import co.unicauca.saberpro.preguntas.domain.EstadoPregunta;
import co.unicauca.saberpro.preguntas.domain.Question;

import java.util.List;

/**
 * Lo que el {@link RevisionController} necesita de la ventana del Revisor:
 * pintar sus preguntas y el resultado de sus decisiones. No conoce Swing.
 */
public interface RevisionVista {

    /** Las preguntas que el Revisor puede tomar para evaluar. */
    void mostrarPreguntasPorRevisar(List<Question> preguntas);

    /** Pinta la pregunta elegida; solo se puede aprobar o rechazar si {@code puedeDecidir}. */
    void mostrarPregunta(Question pregunta, boolean puedeDecidir);

    /** La decisión quedó registrada: la pregunta pasó a {@code decision}. */
    void mostrarDecision(String idPregunta, EstadoPregunta decision);

    void mostrarError(String titulo, String mensaje);
}
