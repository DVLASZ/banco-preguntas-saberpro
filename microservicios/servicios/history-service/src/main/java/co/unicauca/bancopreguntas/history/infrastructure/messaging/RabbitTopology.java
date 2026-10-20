package co.unicauca.bancopreguntas.history.infrastructure.messaging;

import co.unicauca.bancopreguntas.platform.events.EventTopology;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.Declarable;
import org.springframework.amqp.core.Declarables;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

/**
 * Una cola propia por cada evento que este servicio registra, enlazada al exchange común. El historial
 * escucha a todos los servicios sin que ellos lo conozcan (patrón publicador/suscriptor).
 */
@Configuration
public class RabbitTopology {

    public static final String QUEUE_QUESTION_SUBMITTED = "history.q.question-submitted";
    public static final String QUEUE_QUESTION_REOPENED = "history.q.question-reopened";
    public static final String QUEUE_QUESTION_PUBLISHED = "history.q.question-published";
    public static final String QUEUE_QUESTION_STATE_CHANGED = "history.q.question-state-changed";
    public static final String QUEUE_REVIEWERS_ASSIGNED = "history.q.reviewers-assigned";
    public static final String QUEUE_EVALUATION_SUBMITTED = "history.q.evaluation-submitted";
    public static final String QUEUE_REVIEW_COMPLETED = "history.q.review-completed";
    public static final String QUEUE_LOGIN_SUCCEEDED = "history.q.login-succeeded";
    public static final String QUEUE_LOGIN_FAILED = "history.q.login-failed";

    @Bean
    Declarables historyQueues() {
        List<Declarable> declarables = new ArrayList<>();
        enlazar(declarables, QUEUE_QUESTION_SUBMITTED, "question.submitted");
        enlazar(declarables, QUEUE_QUESTION_REOPENED, "question.reopened");
        enlazar(declarables, QUEUE_QUESTION_PUBLISHED, "question.published");
        enlazar(declarables, QUEUE_QUESTION_STATE_CHANGED, "question.state-changed");
        enlazar(declarables, QUEUE_REVIEWERS_ASSIGNED, "review.reviewers-assigned");
        enlazar(declarables, QUEUE_EVALUATION_SUBMITTED, "review.evaluation-submitted");
        enlazar(declarables, QUEUE_REVIEW_COMPLETED, "review.completed");
        enlazar(declarables, QUEUE_LOGIN_SUCCEEDED, "auth.login-succeeded");
        enlazar(declarables, QUEUE_LOGIN_FAILED, "auth.login-failed");
        return new Declarables(declarables);
    }

    private static void enlazar(List<Declarable> destino, String cola, String routingKey) {
        Queue queue = QueueBuilder.durable(cola).build();
        destino.add(queue);
        destino.add(new Binding(cola, Binding.DestinationType.QUEUE, EventTopology.EXCHANGE, routingKey, null));
    }
}
