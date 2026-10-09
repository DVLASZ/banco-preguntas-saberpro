package co.unicauca.bancopreguntas.question.application.port;

import co.unicauca.bancopreguntas.question.domain.CambioEstado;
import co.unicauca.bancopreguntas.question.domain.FiltroPreguntas;
import co.unicauca.bancopreguntas.question.domain.Pagina;
import co.unicauca.bancopreguntas.question.domain.Question;

import java.util.List;
import java.util.Optional;

/**
 * Puerto de salida: persistencia de preguntas. El dominio y la aplicación dependen de esta
 * abstracción; el adaptador JPA la implementa (Inversión de Dependencias).
 */
public interface QuestionRepository {

    /** Crea o actualiza la pregunta (el id decide). */
    Question guardar(Question pregunta);

    Optional<Question> buscarPorId(String id);

    /**
     * Busca preguntas con filtros, de a una página.
     *
     * @param autor  restringe a las preguntas de ese autor; {@code null} para todos
     * @param pagina número de página desde 1
     */
    Pagina<Question> buscar(String autor, FiltroPreguntas filtro, int pagina, int tamano);

    void registrarCambioEstado(String preguntaId, CambioEstado cambio);

    /** Historial de estados, del más antiguo al más reciente. */
    List<CambioEstado> historial(String preguntaId);
}
