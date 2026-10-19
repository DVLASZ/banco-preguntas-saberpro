package co.unicauca.bancopreguntas.simulation.domain;

import java.time.Instant;
import java.util.Locale;

/**
 * Una pregunta publicada tal como este servicio la conoce: la copia que recibió en el evento
 * {@code question.published}. El catálogo no consulta al servicio de preguntas, de modo que los simulacros siguen
 * funcionando aunque ese servicio esté caído.
 *
 * @param version    versión de la pregunta; una copia más vieja nunca reemplaza a una más nueva
 * @param disponible {@code false} cuando la pregunta se archivó y ya no entra en simulacros nuevos
 */
public record PreguntaDeCatalogo(String preguntaId, int version, String titulo, String competencia, String tema,
                                 String subtema, String dificultad, String contexto, String enunciado,
                                 String opcionA, String opcionB, String opcionC, String opcionD,
                                 char respuestaCorrecta, Instant publicadaEn, boolean disponible) {

    public PreguntaDeCatalogo {
        exigir(preguntaId, "El identificador de la pregunta es obligatorio");
        exigir(competencia, "La competencia de la pregunta es obligatoria");
        exigir(enunciado, "El enunciado de la pregunta es obligatorio");
        exigir(opcionA, "Las cuatro opciones son obligatorias");
        exigir(opcionB, "Las cuatro opciones son obligatorias");
        exigir(opcionC, "Las cuatro opciones son obligatorias");
        exigir(opcionD, "Las cuatro opciones son obligatorias");
        respuestaCorrecta = Character.toUpperCase(respuestaCorrecta);
        if ("ABCD".indexOf(respuestaCorrecta) < 0) {
            throw new IllegalArgumentException("La respuesta correcta debe ser A, B, C o D");
        }
        version = Math.max(1, version);
        competencia = competencia.trim().toUpperCase(Locale.ROOT);
        dificultad = dificultad == null || dificultad.isBlank() ? "INTERMEDIO" : dificultad.trim().toUpperCase(Locale.ROOT);
        tema = tema == null ? "" : tema.trim();
        subtema = subtema == null ? "" : subtema.trim();
        contexto = contexto == null ? "" : contexto;
        titulo = titulo == null ? "" : titulo;
    }

    private static void exigir(String valor, String mensaje) {
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException(mensaje);
        }
    }

    /** El texto de la opción de la letra dada. */
    public String opcion(char letra) {
        return switch (Character.toUpperCase(letra)) {
            case 'A' -> opcionA;
            case 'B' -> opcionB;
            case 'C' -> opcionC;
            case 'D' -> opcionD;
            default -> throw new IllegalArgumentException("La opción debe ser A, B, C o D");
        };
    }

    /** La misma pregunta, ya no disponible para simulacros nuevos. */
    public PreguntaDeCatalogo retirada() {
        return new PreguntaDeCatalogo(preguntaId, version, titulo, competencia, tema, subtema, dificultad, contexto,
                enunciado, opcionA, opcionB, opcionC, opcionD, respuestaCorrecta, publicadaEn, false);
    }
}
