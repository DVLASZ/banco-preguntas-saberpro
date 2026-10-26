package co.unicauca.bancopreguntas.audit.domain;

import java.util.List;

/**
 * Una página de resultados de una búsqueda paginada.
 *
 * @param numero número de esta página, desde 1
 */
public record Pagina<T>(List<T> elementos, int numero, int tamano, int totalElementos) {

    public Pagina {
        elementos = List.copyOf(elementos);
    }

    /** Siempre al menos 1, para poder mostrar "Página 1 de 1" aunque no haya resultados. */
    public int totalPaginas() {
        return Math.max(1, (totalElementos + tamano - 1) / tamano);
    }
}
