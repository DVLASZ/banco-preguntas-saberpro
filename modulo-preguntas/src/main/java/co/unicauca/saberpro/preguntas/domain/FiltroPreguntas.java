package co.unicauca.saberpro.preguntas.domain;

import java.text.Normalizer;
import java.util.Locale;

/**
 * Criterios para buscar preguntas (RF-07). Todos son opcionales y se combinan
 * con "y": una pregunta los cumple si coincide con cada criterio presente.
 *
 * @param estado      estado exacto, o {@code null} para cualquiera
 * @param competencia competencia exacta, o {@code null} para cualquiera
 * @param texto       fragmento que debe aparecer en el nombre, la pregunta directa,
 *                    el tema o el subtema, sin distinguir mayúsculas ni tildes;
 *                    {@code null} o vacío para no filtrar por texto
 */
public record FiltroPreguntas(EstadoPregunta estado, Competencia competencia, String texto) {

    /** Sin ningún criterio: todas las preguntas. */
    public static FiltroPreguntas sinFiltros() {
        return new FiltroPreguntas(null, null, null);
    }

    public boolean coincide(Question pregunta) {
        if (estado != null && pregunta.getEstado() != estado) {
            return false;
        }
        if (competencia != null && pregunta.getCompetencia() != competencia) {
            return false;
        }
        String buscado = normalizar(texto);
        if (buscado.isEmpty()) {
            return true;
        }
        return normalizar(pregunta.getNombre()).contains(buscado)
                || normalizar(pregunta.getEnunciado()).contains(buscado)
                || normalizar(pregunta.getTema()).contains(buscado)
                || normalizar(pregunta.getSubtema()).contains(buscado);
    }

    private static String normalizar(String texto) {
        if (texto == null) {
            return "";
        }
        String sinTildes = Normalizer.normalize(texto, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return sinTildes.toLowerCase(Locale.ROOT).trim();
    }
}
