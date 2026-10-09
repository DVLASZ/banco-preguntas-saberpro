package co.unicauca.saberpro.microkernel.common.interfaces;

import co.unicauca.saberpro.microkernel.common.entities.QuestionRequest;
import co.unicauca.saberpro.preguntas.domain.Question;

/**
 * Contrato que debe cumplir todo plugin de generación de preguntas
 * (Taller 5: patrón Microkernel). El núcleo ({@code QuestionMicrokernel})
 * solo conoce esta interfaz — nunca las clases concretas de los plugins,
 * que se cargan dinámicamente por Reflexión desde {@code plugins.properties}.
 */
public interface QuestionPlugin {

    /** Nombre identificador del plugin (para mostrarlo en la interfaz, logs, etc.). */
    String getName();

    /** Indica si este plugin sabe generar preguntas del tipo solicitado. */
    boolean supports(String type);

    /**
     * Genera una pregunta real del banco a partir de la solicitud, o
     * {@code null} si la solicitud no pasa el pipeline de validación
     * (Tuberías y Filtros).
     */
    Question generate(QuestionRequest request);
}
