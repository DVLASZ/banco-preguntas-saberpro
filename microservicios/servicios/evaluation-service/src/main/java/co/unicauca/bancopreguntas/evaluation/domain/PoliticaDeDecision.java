package co.unicauca.bancopreguntas.evaluation.domain;

import java.util.Collection;

/**
 * Cómo se decide el resultado de una ronda a partir de las evaluaciones (patrón Strategy). Hoy se usa la
 * {@link Unanimidad}; cambiar a mayoría u otra regla no toca el flujo de la revisión.
 */
public interface PoliticaDeDecision {

    /**
     * @param revisoresAsignados los revisores de la ronda
     * @param evaluaciones       las evaluaciones recibidas hasta ahora
     */
    ResultadoDeDecision decidir(Collection<String> revisoresAsignados, Collection<Evaluacion> evaluaciones);
}
