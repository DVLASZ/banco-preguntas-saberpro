package co.unicauca.bancopreguntas.history.application.port;

import co.unicauca.bancopreguntas.history.domain.EntradaDeHistorial;
import co.unicauca.bancopreguntas.history.domain.Observador;
import co.unicauca.bancopreguntas.history.domain.Pagina;
import co.unicauca.bancopreguntas.history.domain.TipoEntrada;

import java.util.List;
import java.util.Set;

/** Puerto de salida: persistencia del historial. Solo se agrega, nunca se modifica ni se borra. */
public interface HistoryRepository {

    void guardar(EntradaDeHistorial entrada);

    /**
     * El rastro de una pregunta, en orden cronológico.
     *
     * @param tipos los tipos que se incluyen; {@code null} para todos
     */
    List<EntradaDeHistorial> lineaDeTiempo(String preguntaId, Set<TipoEntrada> tipos);

    /** Todo lo que el observador puede ver de todas las preguntas, lo más reciente primero. */
    Pagina<EntradaDeHistorial> visiblesPara(Observador observador, int pagina, int tamano);
}
