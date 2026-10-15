package co.unicauca.bancopreguntas.review.infrastructure.messaging;

import co.unicauca.bancopreguntas.platform.events.EventTopology;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Colas propias de este servicio, con las que escucha al servicio de preguntas. */
@Configuration
public class RabbitTopology {

    public static final String QUEUE_QUESTION_SUBMITTED = "review.q.question-submitted";
    public static final String QUEUE_QUESTION_REOPENED = "review.q.question-reopened";
    public static final String KEY_QUESTION_SUBMITTED = "question.submitted";
    public static final String KEY_QUESTION_REOPENED = "question.reopened";

    @Bean
    Queue questionSubmittedQueue() {
        return QueueBuilder.durable(QUEUE_QUESTION_SUBMITTED).build();
    }

    @Bean
    Queue questionReopenedQueue() {
        return QueueBuilder.durable(QUEUE_QUESTION_REOPENED).build();
    }

    @Bean
    Binding questionSubmittedBinding(Queue questionSubmittedQueue) {
        return new Binding(questionSubmittedQueue.getName(), Binding.DestinationType.QUEUE,
                EventTopology.EXCHANGE, KEY_QUESTION_SUBMITTED, null);
    }

    @Bean
    Binding questionReopenedBinding(Queue questionReopenedQueue) {
        return new Binding(questionReopenedQueue.getName(), Binding.DestinationType.QUEUE,
                EventTopology.EXCHANGE, KEY_QUESTION_REOPENED, null);
    }
}
