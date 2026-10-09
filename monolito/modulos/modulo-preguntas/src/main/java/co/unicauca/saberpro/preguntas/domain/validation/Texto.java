package co.unicauca.saberpro.preguntas.domain.validation;

import co.unicauca.saberpro.preguntas.domain.ContenidoPregunta;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;

/** Utilidades de texto compartidas por las reglas. */
final class Texto {

    private Texto() {
    }

    static boolean vacio(String texto) {
        return texto == null || texto.isBlank();
    }

    /** Minúsculas, sin tildes y con los espacios colapsados, para comparar sin importar la escritura. */
    static String normalizar(String texto) {
        if (texto == null) {
            return "";
        }
        String sinTildes = Normalizer.normalize(texto, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return sinTildes.toLowerCase(Locale.ROOT).trim().replaceAll("\\s+", " ");
    }

    /** Las cuatro opciones de respuesta, en orden A-D. */
    static List<String> opciones(ContenidoPregunta contenido) {
        return List.of(nulo(contenido.opcionA()), nulo(contenido.opcionB()),
                nulo(contenido.opcionC()), nulo(contenido.opcionD()));
    }

    /** Nombre del campo del formulario de la opción en la posición dada (0 = A). */
    static String campoOpcion(int indice) {
        return "opcion" + (char) ('A' + indice);
    }

    static char letra(int indice) {
        return (char) ('A' + indice);
    }

    private static String nulo(String texto) {
        return texto == null ? "" : texto;
    }
}
