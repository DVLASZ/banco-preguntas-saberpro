package co.unicauca.bancopreguntas.review.domain.event;

import java.util.List;

/**
 * Se asignaron revisores a una ronda. {@code reviewerIds} son los asignados <b>en este acto</b> (al agregar
 * más a una ronda en curso, solo los nuevos). {@code automatic} indica que el sistema reasignó solo a los
 * mismos revisores al empezar una ronda nueva; en ese caso {@code assignedBy} es {@code "system"}.
 */
public record ReviewersAssigned(String questionId, String authorId, int round, List<String> reviewerIds,
                                String assignedBy, boolean automatic) implements DomainEvent {

    public static final String TYPE = "review.reviewers-assigned";

    public ReviewersAssigned {
        reviewerIds = List.copyOf(reviewerIds);
    }

    @Override
    public String type() {
        return TYPE;
    }
}
