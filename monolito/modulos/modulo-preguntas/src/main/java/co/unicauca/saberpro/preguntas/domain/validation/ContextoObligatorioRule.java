package co.unicauca.saberpro.preguntas.domain.validation;

import co.unicauca.saberpro.preguntas.domain.ContenidoPregunta;

import java.util.List;

/** RF-08: toda pregunta contiene un contexto. */
public class ContextoObligatorioRule implements ValidationRule {

    @Override
    public List<Violacion> validar(ContenidoPregunta contenido) {
        if (Texto.vacio(contenido.contexto())) {
            return List.of(new Violacion("contexto", "El contexto es obligatorio"));
        }
        return List.of();
    }
}
