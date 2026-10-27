package co.unicauca.bancopreguntas.assignment.domain.event;

import java.util.List;

/**
 * Se asignaron revisores a una ronda. {@code reviewerIds} son los asignados <b>en este acto</b> (al agregar
 * más a una ronda en curso, solo los nuevos). {@code automatic} indica que el sistema reasignó solo a los
 * mismos revisores al empezar una ronda nueva; en ese caso {@code assignedBy} es {@code "system"}.
 *
 * <p>Lleva también la versión, el título y la competencia de la pregunta (transferencia de estado por evento): quien
 * evalúa (evaluation-service) arma así su copia de la ronda sin consultar a nadie, aunque este evento llegue antes
 * que el aviso de que la pregunta se envió a revisión.
 */
public record ReviewersAssigned(String questionId, String authorId, int round, List<String> reviewerIds,
                                String assignedBy, boolean automatic, int questionVersion, String title,
                                String competency) implements DomainEvent {

    public static final String TYPE = "review.reviewers-assigned";

    public ReviewersAssigned {
        reviewerIds = List.copyOf(reviewerIds);
    }

    @Override
    public String type() {
        return TYPE;
    }
}
