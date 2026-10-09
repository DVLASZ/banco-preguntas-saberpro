package co.unicauca.saberpro.preguntas.domain;

import java.util.List;

/**
 * Una página de resultados de una búsqueda paginada.
 *
 * @param elementos       los elementos de esta página
 * @param numero          número de esta página, desde 1
 * @param tamano          máximo de elementos por página
 * @param totalElementos  total de elementos que cumplen la búsqueda, en todas las páginas
 */
public record Pagina<T>(List<T> elementos, int numero, int tamano, int totalElementos) {

    public Pagina {
        elementos = List.copyOf(elementos);
    }

    /** Siempre al menos 1, para poder mostrar "Página 1 de 1" aunque no haya resultados. */
    public int totalPaginas() {
        return Math.max(1, (totalElementos + tamano - 1) / tamano);
    }

    public boolean haySiguiente() {
        return numero < totalPaginas();
    }

    public boolean hayAnterior() {
        return numero > 1;
    }
}
