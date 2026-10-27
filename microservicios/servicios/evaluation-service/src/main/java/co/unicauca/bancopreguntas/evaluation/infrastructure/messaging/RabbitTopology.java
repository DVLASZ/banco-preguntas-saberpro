package co.unicauca.bancopreguntas.evaluation.infrastructure.messaging;

import co.unicauca.bancopreguntas.platform.events.EventTopology;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Colas propias de este servicio, con las que escucha al servicio de preguntas (envíos y reaperturas) y a
 * assignment-service (revisores asignados). El exchange común lo declara el módulo {@code bp-platform}.
 */
@Configuration
public class RabbitTopology {

    public static final String QUEUE_QUESTION_SUBMITTED = "evaluation.q.question-submitted";
    public static final String QUEUE_QUESTION_REOPENED = "evaluation.q.question-reopened";
    public static final String QUEUE_REVIEWERS_ASSIGNED = "evaluation.q.reviewers-assigned";
    public static final String KEY_QUESTION_SUBMITTED = "question.submitted";
    public static final String KEY_QUESTION_REOPENED = "question.reopened";
    public static final String KEY_REVIEWERS_ASSIGNED = "review.reviewers-assigned";

    @Bean
    Queue evaluationQuestionSubmittedQueue() {
        return QueueBuilder.durable(QUEUE_QUESTION_SUBMITTED).build();
    }

    @Bean
    Queue evaluationQuestionReopenedQueue() {
        return QueueBuilder.durable(QUEUE_QUESTION_REOPENED).build();
    }

    @Bean
    Queue evaluationReviewersAssignedQueue() {
        return QueueBuilder.durable(QUEUE_REVIEWERS_ASSIGNED).build();
    }

    @Bean
    Binding evaluationQuestionSubmittedBinding(Queue evaluationQuestionSubmittedQueue) {
        return enlazar(evaluationQuestionSubmittedQueue, KEY_QUESTION_SUBMITTED);
    }

    @Bean
    Binding evaluationQuestionReopenedBinding(Queue evaluationQuestionReopenedQueue) {
        return enlazar(evaluationQuestionReopenedQueue, KEY_QUESTION_REOPENED);
    }

    @Bean
    Binding evaluationReviewersAssignedBinding(Queue evaluationReviewersAssignedQueue) {
        return enlazar(evaluationReviewersAssignedQueue, KEY_REVIEWERS_ASSIGNED);
    }

    private static Binding enlazar(Queue cola, String routingKey) {
        return new Binding(cola.getName(), Binding.DestinationType.QUEUE, EventTopology.EXCHANGE, routingKey, null);
    }
}
