package co.unicauca.bancopreguntas.audit.api.dto;

import co.unicauca.bancopreguntas.audit.domain.Pagina;

import java.util.List;
import java.util.function.Function;

/** Una página de resultados. {@code pagina} cuenta desde 0, como el parámetro {@code page} de la consulta. */
public record PageResponse<T>(List<T> contenido, int pagina, int tamano, long totalElementos, int totalPaginas) {

    public static <D, T> PageResponse<T> de(Pagina<D> p, Function<D, T> conversion) {
        return new PageResponse<>(p.elementos().stream().map(conversion).toList(), p.numero() - 1, p.tamano(),
                p.totalElementos(), p.totalPaginas());
    }
}
