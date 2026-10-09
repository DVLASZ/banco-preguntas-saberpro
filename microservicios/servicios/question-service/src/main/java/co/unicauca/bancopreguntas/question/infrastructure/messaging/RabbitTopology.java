package co.unicauca.bancopreguntas.question.infrastructure.messaging;

import co.unicauca.bancopreguntas.platform.events.EventTopology;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Colas propias de este servicio, con las que escucha al servicio de revisión. El exchange común lo
 * declara el módulo {@code bp-platform}.
 */
@Configuration
public class RabbitTopology {

    public static final String QUEUE_REVIEWERS_ASSIGNED = "question.q.reviewers-assigned";
    public static final String QUEUE_REVIEW_COMPLETED = "question.q.review-completed";
    public static final String KEY_REVIEWERS_ASSIGNED = "review.reviewers-assigned";
    public static final String KEY_REVIEW_COMPLETED = "review.completed";

    @Bean
    Queue reviewersAssignedQueue() {
        return QueueBuilder.durable(QUEUE_REVIEWERS_ASSIGNED).build();
    }

    @Bean
    Queue reviewCompletedQueue() {
        return QueueBuilder.durable(QUEUE_REVIEW_COMPLETED).build();
    }

    @Bean
    Binding reviewersAssignedBinding(Queue reviewersAssignedQueue) {
        return new Binding(reviewersAssignedQueue.getName(), Binding.DestinationType.QUEUE,
                EventTopology.EXCHANGE, KEY_REVIEWERS_ASSIGNED, null);
    }

    @Bean
    Binding reviewCompletedBinding(Queue reviewCompletedQueue) {
        return new Binding(reviewCompletedQueue.getName(), Binding.DestinationType.QUEUE,
                EventTopology.EXCHANGE, KEY_REVIEW_COMPLETED, null);
    }
}
