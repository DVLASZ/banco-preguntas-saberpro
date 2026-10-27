package co.unicauca.bancopreguntas.assignment.infrastructure.messaging;

import co.unicauca.bancopreguntas.platform.events.EventTopology;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Colas propias de este servicio, con las que escucha al servicio de preguntas (envíos y reaperturas) y a
 * evaluation-service (evaluaciones y resultados). El exchange común lo declara el módulo {@code bp-platform}.
 */
@Configuration
public class RabbitTopology {

    public static final String QUEUE_QUESTION_SUBMITTED = "assignment.q.question-submitted";
    public static final String QUEUE_QUESTION_REOPENED = "assignment.q.question-reopened";
    public static final String QUEUE_EVALUATION_SUBMITTED = "assignment.q.evaluation-submitted";
    public static final String QUEUE_REVIEW_COMPLETED = "assignment.q.review-completed";
    public static final String KEY_QUESTION_SUBMITTED = "question.submitted";
    public static final String KEY_QUESTION_REOPENED = "question.reopened";
    public static final String KEY_EVALUATION_SUBMITTED = "review.evaluation-submitted";
    public static final String KEY_REVIEW_COMPLETED = "review.completed";

    @Bean
    Queue assignmentQuestionSubmittedQueue() {
        return QueueBuilder.durable(QUEUE_QUESTION_SUBMITTED).build();
    }

    @Bean
    Queue assignmentQuestionReopenedQueue() {
        return QueueBuilder.durable(QUEUE_QUESTION_REOPENED).build();
    }

    @Bean
    Queue assignmentEvaluationSubmittedQueue() {
        return QueueBuilder.durable(QUEUE_EVALUATION_SUBMITTED).build();
    }

    @Bean
    Queue assignmentReviewCompletedQueue() {
        return QueueBuilder.durable(QUEUE_REVIEW_COMPLETED).build();
    }

    @Bean
    Binding assignmentQuestionSubmittedBinding(Queue assignmentQuestionSubmittedQueue) {
        return enlazar(assignmentQuestionSubmittedQueue, KEY_QUESTION_SUBMITTED);
    }

    @Bean
    Binding assignmentQuestionReopenedBinding(Queue assignmentQuestionReopenedQueue) {
        return enlazar(assignmentQuestionReopenedQueue, KEY_QUESTION_REOPENED);
    }

    @Bean
    Binding assignmentEvaluationSubmittedBinding(Queue assignmentEvaluationSubmittedQueue) {
        return enlazar(assignmentEvaluationSubmittedQueue, KEY_EVALUATION_SUBMITTED);
    }

    @Bean
    Binding assignmentReviewCompletedBinding(Queue assignmentReviewCompletedQueue) {
        return enlazar(assignmentReviewCompletedQueue, KEY_REVIEW_COMPLETED);
    }

    private static Binding enlazar(Queue cola, String routingKey) {
        return new Binding(cola.getName(), Binding.DestinationType.QUEUE, EventTopology.EXCHANGE, routingKey, null);
    }
}
