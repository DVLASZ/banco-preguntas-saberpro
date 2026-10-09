package co.unicauca.saberpro.preguntas.domain.validation;

import co.unicauca.saberpro.preguntas.domain.ContenidoPregunta;

import java.util.List;

/**
 * Una regla de la validación estructural. Cada requisito de HU03 es una
 * implementación independiente: agregar o quitar un criterio de calidad es
 * registrar otra regla en el {@link QuestionValidator}, sin modificar las
 * existentes (RNF-13).
 */
public interface ValidationRule {

    /** Retorna las violaciones encontradas; una lista vacía si el contenido cumple la regla. */
    List<Violacion> validar(ContenidoPregunta contenido);
}
