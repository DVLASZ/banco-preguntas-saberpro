package co.unicauca.saberpro.microkernel.pipeline.base;

import co.unicauca.saberpro.microkernel.common.entities.QuestionRequest;

import java.util.ArrayList;
import java.util.List;

/**
 * Coordina la ejecución secuencial de los filtros de validación
 * (Taller 5: Tuberías y Filtros). Se detiene en el primer filtro que
 * falla — no tiene sentido seguir validando una solicitud que ya se sabe
 * inválida.
 */
public class QuestionPipeline {

    private final List<QuestionFilter> filters = new ArrayList<>();
    private String ultimoFiltroFallido;

    public void addFilter(QuestionFilter filter) {
        filters.add(filter);
    }

    public boolean execute(QuestionRequest request) {
        ultimoFiltroFallido = null;
        for (QuestionFilter filter : filters) {
            if (!filter.process(request)) {
                ultimoFiltroFallido = filter.getClass().getSimpleName();
                System.err.println("La validación falló en el filtro: " + ultimoFiltroFallido);
                return false;
            }
        }
        return true;
    }

    /** Nombre simple del filtro que hizo fallar la última ejecución, o {@code null} si pasó (o no se ha ejecutado). */
    public String getUltimoFiltroFallido() {
        return ultimoFiltroFallido;
    }
}
