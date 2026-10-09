package co.unicauca.saberpro.microkernel.pipeline.filters;

import co.unicauca.saberpro.microkernel.common.entities.QuestionRequest;
import co.unicauca.saberpro.microkernel.pipeline.base.QuestionFilter;

import java.util.List;

/** Valida que existan exactamente 4 opciones de respuesta, todas con contenido. */
public class OptionsValidationFilter implements QuestionFilter {

    private static final int OPCIONES_REQUERIDAS = 4;

    @Override
    public boolean process(QuestionRequest request) {
        List<String> opciones = request.getOptions();
        if (opciones == null || opciones.size() != OPCIONES_REQUERIDAS) {
            return false;
        }
        for (String opcion : opciones) {
            if (opcion == null || opcion.isBlank()) {
                return false;
            }
        }
        return true;
    }
}
