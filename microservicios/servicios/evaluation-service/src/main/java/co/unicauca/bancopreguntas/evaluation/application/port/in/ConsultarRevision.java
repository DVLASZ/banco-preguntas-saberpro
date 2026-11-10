package co.unicauca.bancopreguntas.evaluation.application.port.in;

import co.unicauca.bancopreguntas.evaluation.domain.Observador;
import co.unicauca.bancopreguntas.evaluation.domain.VistaDelCaso;

/** Puerto de entrada: ver la revisión de una pregunta con sus rondas y observaciones (HU-05 y HU-06). */
public interface ConsultarRevision {

    /** La revisión tal como la ve el observador: cada rol ve lo que le corresponde. */
    VistaDelCaso caso(String preguntaId, Observador observador);
}
