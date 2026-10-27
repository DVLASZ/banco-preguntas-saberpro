package co.unicauca.bancopreguntas.evaluation.application.port;

import co.unicauca.bancopreguntas.evaluation.domain.CasoDeEvaluacion;

import java.util.Optional;

/**
 * Puerto de salida: persistencia de las evaluaciones. El dominio y la aplicación dependen de esta abstracción;
 * el adaptador JPA la implementa.
 */
public interface EvaluationRepository {

    /** Crea o actualiza el caso completo (rondas, revisores y evaluaciones). */
    void guardar(CasoDeEvaluacion caso);

    /** Busca el caso de una pregunta, solo para consultarlo. */
    Optional<CasoDeEvaluacion> buscarPorPreguntaId(String preguntaId);

    /**
     * Busca el caso de una pregunta para modificarlo y bloquea su registro hasta el fin de la transacción:
     * así dos revisores que evalúan a la vez no deciden la ronda con datos viejos.
     */
    Optional<CasoDeEvaluacion> buscarPorPreguntaIdParaActualizar(String preguntaId);
}
