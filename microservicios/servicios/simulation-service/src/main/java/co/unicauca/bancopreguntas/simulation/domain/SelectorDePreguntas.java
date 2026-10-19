package co.unicauca.bancopreguntas.simulation.domain;

import java.util.List;

/**
 * Cómo se escogen las preguntas de un simulacro entre las candidatas (patrón Strategy). Hoy se usa la
 * {@link SeleccionAleatoria} (RF-22); otra estrategia, como equilibrar por competencia, no toca el caso de uso.
 */
public interface SelectorDePreguntas {

    /**
     * @param candidatas preguntas que ya cumplen los criterios
     * @param cantidad   cuántas escoger; nunca es mayor que las candidatas
     */
    List<PreguntaDeCatalogo> elegir(List<PreguntaDeCatalogo> candidatas, int cantidad);
}
