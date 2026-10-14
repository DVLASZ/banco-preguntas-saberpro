package co.unicauca.bancopreguntas.notification.infrastructure.messaging;

import co.unicauca.bancopreguntas.platform.events.EventTopology;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.Declarables;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

/**
 * Una cola propia por cada evento que este servicio escucha, enlazada al exchange común (suscripción
 * publicador/suscriptor). El exchange lo declara el módulo {@code bp-platform}.
 */
@Configuration
public class RabbitTopology {

    public static final String QUEUE_QUESTION_SUBMITTED = "notification.q.question-submitted";
    public static final String QUEUE_QUESTION_PUBLISHED = "notification.q.question-published";
    public static final String QUEUE_REVIEWERS_ASSIGNED = "notification.q.reviewers-assigned";
    public static final String QUEUE_REVIEW_COMPLETED = "notification.q.review-completed";
    public static final String QUEUE_EVALUATION_SUBMITTED = "notification.q.evaluation-submitted";

    @Bean
    Declarables notificationQueues() {
        List<org.springframework.amqp.core.Declarable> declarables = new ArrayList<>();
        enlazar(declarables, QUEUE_QUESTION_SUBMITTED, "question.submitted");
        enlazar(declarables, QUEUE_QUESTION_PUBLISHED, "question.published");
        enlazar(declarables, QUEUE_REVIEWERS_ASSIGNED, "review.reviewers-assigned");
        enlazar(declarables, QUEUE_REVIEW_COMPLETED, "review.completed");
        enlazar(declarables, QUEUE_EVALUATION_SUBMITTED, "review.evaluation-submitted");
        return new Declarables(declarables);
    }

    private static void enlazar(List<org.springframework.amqp.core.Declarable> destino, String cola, String routingKey) {
        Queue queue = QueueBuilder.durable(cola).build();
        destino.add(queue);
        destino.add(new Binding(cola, Binding.DestinationType.QUEUE, EventTopology.EXCHANGE, routingKey, null));
    }
}
