package co.unicauca.bancopreguntas.evaluation.application.port.in;

import co.unicauca.bancopreguntas.evaluation.domain.Decision;
import co.unicauca.bancopreguntas.evaluation.domain.ResultadoDeEvaluacion;

/** Puerto de entrada: un revisor envía su evaluación de una pregunta (HU-05). */
public interface EvaluarPregunta {

    /**
     * Registra la evaluación del revisor sobre la ronda vigente de la pregunta. Si con ella se decide la ronda, se
     * avisa a los demás servicios.
     */
    ResultadoDeEvaluacion evaluar(String preguntaId, String revisorId, Decision decision, String observacion);
}
