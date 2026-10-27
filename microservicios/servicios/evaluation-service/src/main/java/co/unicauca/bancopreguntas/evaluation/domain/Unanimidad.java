package co.unicauca.bancopreguntas.evaluation.domain;

import java.util.Collection;

/**
 * Política de unanimidad: la pregunta se aprueba solo si <b>todos</b> los revisores asignados la aprueban, y
 * un solo rechazo basta para devolverla al autor, sin esperar a los demás.
 */
public class Unanimidad implements PoliticaDeDecision {

    @Override
    public ResultadoDeDecision decidir(Collection<String> revisoresAsignados, Collection<Evaluacion> evaluaciones) {
        boolean hayRechazo = evaluaciones.stream().anyMatch(e -> e.decision() == Decision.RECHAZAR);
        if (hayRechazo) {
            return ResultadoDeDecision.RECHAZADA;
        }
        long aprobaciones = evaluaciones.stream()
                .filter(e -> e.decision() == Decision.APROBAR)
                .map(Evaluacion::revisorId)
                .filter(revisoresAsignados::contains)
                .distinct()
                .count();
        boolean todosAprobaron = !revisoresAsignados.isEmpty() && aprobaciones == revisoresAsignados.size();
        return todosAprobaron ? ResultadoDeDecision.APROBADA : ResultadoDeDecision.PENDIENTE;
    }
}
