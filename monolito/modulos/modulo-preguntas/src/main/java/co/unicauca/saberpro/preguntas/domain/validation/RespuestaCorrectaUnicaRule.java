package co.unicauca.saberpro.preguntas.domain.validation;

import co.unicauca.saberpro.preguntas.domain.ContenidoPregunta;

import java.util.List;

/** RF-11: existe únicamente una respuesta correcta, marcada con una sola letra entre A y D. */
public class RespuestaCorrectaUnicaRule implements ValidationRule {

    @Override
    public List<Violacion> validar(ContenidoPregunta contenido) {
        String respuesta = contenido.respuestaCorrecta();
        if (Texto.vacio(respuesta)) {
            return List.of();
        }
        if (!respuesta.trim().matches("(?i)[A-D]")) {
            return List.of(new Violacion("respuestaCorrecta",
                    "La respuesta correcta debe ser una sola letra: A, B, C o D"));
        }
        return List.of();
    }
}
