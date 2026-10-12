package co.unicauca.bancopreguntas.review.application.port;

import co.unicauca.bancopreguntas.review.domain.AsignacionDeRevisor;
import co.unicauca.bancopreguntas.review.domain.CasoDeRevision;
import co.unicauca.bancopreguntas.review.domain.CasoPendiente;
import co.unicauca.bancopreguntas.review.domain.FiltroAsignacion;
import co.unicauca.bancopreguntas.review.domain.Pagina;

import java.util.Optional;

/**
 * Puerto de salida: persistencia de las revisiones. El dominio y la aplicación dependen de esta abstracción;
 * el adaptador JPA la implementa.
 */
public interface ReviewCaseRepository {

    /** Crea o actualiza el caso completo (rondas, asignaciones y evaluaciones). */
    void guardar(CasoDeRevision caso);

    /** Busca el caso de una pregunta, solo para consultarlo. */
    Optional<CasoDeRevision> buscarPorPreguntaId(String preguntaId);

    /**
     * Busca el caso de una pregunta para modificarlo y bloquea su registro hasta el fin de la transacción:
     * así dos revisores que evalúan a la vez no deciden la ronda con datos viejos.
     */
    Optional<CasoDeRevision> buscarPorPreguntaIdParaActualizar(String preguntaId);

    /** Las preguntas cuya ronda espera que el administrador asigne revisores, las más antiguas primero. */
    Pagina<CasoPendiente> pendientesDeAsignacion(int pagina, int tamano);

    /** La bandeja de un revisor, las asignaciones más recientes primero. */
    Pagina<AsignacionDeRevisor> asignadasA(String revisorId, FiltroAsignacion filtro, int pagina, int tamano);
}
