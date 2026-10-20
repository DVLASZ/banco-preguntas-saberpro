package co.unicauca.bancopreguntas.simulation.infrastructure.messaging;

import co.unicauca.bancopreguntas.platform.events.EventTopology;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Colas propias de este servicio, con las que escucha al servicio de preguntas. */
@Configuration
public class RabbitTopology {

    public static final String QUEUE_QUESTION_PUBLISHED = "simulation.q.question-published";
    public static final String QUEUE_QUESTION_STATE_CHANGED = "simulation.q.question-state-changed";
    public static final String KEY_QUESTION_PUBLISHED = "question.published";
    public static final String KEY_QUESTION_STATE_CHANGED = "question.state-changed";

    @Bean
    Queue simulationQuestionPublishedQueue() {
        return QueueBuilder.durable(QUEUE_QUESTION_PUBLISHED).build();
    }

    @Bean
    Queue simulationQuestionStateChangedQueue() {
        return QueueBuilder.durable(QUEUE_QUESTION_STATE_CHANGED).build();
    }

    @Bean
    Binding simulationQuestionPublishedBinding(Queue simulationQuestionPublishedQueue) {
        return new Binding(simulationQuestionPublishedQueue.getName(), Binding.DestinationType.QUEUE,
                EventTopology.EXCHANGE, KEY_QUESTION_PUBLISHED, null);
    }

    @Bean
    Binding simulationQuestionStateChangedBinding(Queue simulationQuestionStateChangedQueue) {
        return new Binding(simulationQuestionStateChangedQueue.getName(), Binding.DestinationType.QUEUE,
                EventTopology.EXCHANGE, KEY_QUESTION_STATE_CHANGED, null);
    }
}
