package co.unicauca.bancopreguntas.simulation.domain;

import java.text.Normalizer;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

/**
 * Lo que pide el estudiante al generar un simulacro (RF-22): cuántas preguntas, cuánto tiempo y, opcionalmente,
 * de qué competencias, temas y niveles de dificultad. Un conjunto vacío significa «cualquiera».
 *
 * @param duracionMinutos tiempo máximo del simulacro (RF-23)
 */
public record CriteriosDeSimulacro(Set<String> competencias, Set<String> temas, Set<String> dificultades,
                                   int cantidad, int duracionMinutos) {

    public static final int DURACION_MAXIMA_MINUTOS = 240;

    public CriteriosDeSimulacro {
        competencias = normalizar(competencias, true);
        dificultades = normalizar(dificultades, true);
        temas = normalizar(temas, false);
        if (cantidad < 1) {
            throw new CriteriosInvalidosException("El simulacro debe tener al menos una pregunta");
        }
        if (duracionMinutos < 1 || duracionMinutos > DURACION_MAXIMA_MINUTOS) {
            throw new CriteriosInvalidosException(
                    "La duración debe estar entre 1 y " + DURACION_MAXIMA_MINUTOS + " minutos");
        }
    }

    /** Si la pregunta cumple los criterios de competencia, tema y dificultad. */
    public boolean acepta(PreguntaDeCatalogo pregunta) {
        return (competencias.isEmpty() || competencias.contains(pregunta.competencia()))
                && (dificultades.isEmpty() || dificultades.contains(pregunta.dificultad()))
                && (temas.isEmpty() || temas.contains(sinTildes(pregunta.tema())) || temas.contains(sinTildes(pregunta.subtema())));
    }

    private static Set<String> normalizar(Collection<String> valores, boolean mayusculas) {
        Set<String> resultado = new LinkedHashSet<>();
        if (valores != null) {
            for (String valor : valores) {
                if (valor != null && !valor.isBlank()) {
                    resultado.add(mayusculas ? valor.trim().toUpperCase(Locale.ROOT) : sinTildes(valor));
                }
            }
        }
        return Set.copyOf(resultado);
    }

    /** Minúsculas y sin tildes, para comparar temas sin importar cómo se escriban. */
    static String sinTildes(String texto) {
        String limpio = Normalizer.normalize(texto == null ? "" : texto, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return limpio.toLowerCase(Locale.ROOT).trim();
    }
}
