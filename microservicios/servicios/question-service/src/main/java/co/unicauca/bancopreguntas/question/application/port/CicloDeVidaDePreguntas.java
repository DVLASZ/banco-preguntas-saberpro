package co.unicauca.bancopreguntas.question.application.port;

import co.unicauca.bancopreguntas.question.domain.CambioEstado;
import co.unicauca.bancopreguntas.question.domain.DependenciaNoDisponibleException;
import co.unicauca.bancopreguntas.question.domain.Question;

import java.util.List;

/**
 * Puerto de salida: el ciclo de vida de las preguntas (RF-14 y RF-15), que lleva lifecycle-service. Cada operación
 * pide la transición al otro servicio, que es quien la autoriza, la anota en el historial y publica sus eventos.
 *
 * <p>Todas pueden lanzar {@link DependenciaNoDisponibleException}; además, {@code AccesoDenegadoException},
 * {@code OperacionNoPermitidaException} y {@code QuestionNotFoundException} cuando el otro servicio las rechaza.
 */
public interface CicloDeVidaDePreguntas {

    /** Registra la pregunta recién creada en Borrador a nombre de su autor. */
    EstadoDeCiclo registrar(String preguntaId);

    /** Borrador → Pendiente de revisión. */
    EstadoDeCiclo enviarARevision(Question pregunta);

    /** Rechazada → Borrador, como una versión nueva. */
    EstadoDeCiclo reabrir(String preguntaId);

    /** Aprobada → Publicada. Lleva el contenido que viajará en el evento {@code question.published}. */
    EstadoDeCiclo publicar(Question pregunta);

    /** El historial de estados, del más antiguo al más reciente. */
    List<CambioEstado> historial(String preguntaId);
}
