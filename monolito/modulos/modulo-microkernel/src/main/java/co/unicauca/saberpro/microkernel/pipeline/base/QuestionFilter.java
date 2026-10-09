package co.unicauca.saberpro.microkernel.pipeline.base;

import co.unicauca.saberpro.microkernel.common.entities.QuestionRequest;

/**
 * Un filtro de validación del pipeline (Taller 5: Tuberías y Filtros).
 * Cada filtro tiene una única responsabilidad y no conoce a los demás —
 * es {@code QuestionPipeline} quien los encadena.
 */
public interface QuestionFilter {

    /** @return {@code true} si la solicitud pasa esta validación. */
    boolean process(QuestionRequest request);
}
