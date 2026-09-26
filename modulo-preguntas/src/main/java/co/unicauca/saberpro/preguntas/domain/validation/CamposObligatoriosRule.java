package co.unicauca.saberpro.preguntas.domain.validation;

import co.unicauca.saberpro.preguntas.domain.ContenidoPregunta;

import java.util.ArrayList;
import java.util.List;

/** Toda pregunta debe traer el resto de sus campos obligatorios (HU-01): nombre, pregunta directa, respuesta, justificación, bibliografía, competencia, tema, subtema y dificultad. */
public class CamposObligatoriosRule implements ValidationRule {

    @Override
    public List<Violacion> validar(ContenidoPregunta c) {
        List<Violacion> violaciones = new ArrayList<>();
        exigirTexto(violaciones, "nombre", c.nombre(), "El nombre de la pregunta es obligatorio");
        exigirTexto(violaciones, "enunciado", c.enunciado(), "La pregunta directa es obligatoria");
        exigirTexto(violaciones, "respuestaCorrecta", c.respuestaCorrecta(), "Debe indicar la respuesta correcta");
        exigirTexto(violaciones, "justificacion", c.justificacion(), "La justificación de la respuesta es obligatoria");
        exigirTexto(violaciones, "bibliografia", c.bibliografia(), "La bibliografía es obligatoria");
        exigirTexto(violaciones, "tema", c.tema(), "El tema es obligatorio");
        exigirTexto(violaciones, "subtema", c.subtema(), "El subtema es obligatorio");
        if (c.competencia() == null) {
            violaciones.add(new Violacion("competencia", "La competencia es obligatoria"));
        }
        if (c.dificultad() == null) {
            violaciones.add(new Violacion("dificultad", "El nivel de dificultad es obligatorio"));
        }
        return violaciones;
    }

    private static void exigirTexto(List<Violacion> violaciones, String campo, String valor, String mensaje) {
        if (Texto.vacio(valor)) {
            violaciones.add(new Violacion(campo, mensaje));
        }
    }
}
