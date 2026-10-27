package co.unicauca.bancopreguntas.question.infrastructure.messaging;

import co.unicauca.bancopreguntas.platform.events.EventTopology;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Cola propia de este servicio, con la que escucha los cambios de estado que informa lifecycle-service. El exchange
 * común lo declara el módulo {@code bp-platform}.
 */
@Configuration
public class RabbitTopology {

    public static final String QUEUE_STATE_CHANGED = "question.q.state-changed";
    public static final String KEY_STATE_CHANGED = "question.state-changed";

    @Bean
    Queue stateChangedQueue() {
        return QueueBuilder.durable(QUEUE_STATE_CHANGED).build();
    }

    @Bean
    Binding stateChangedBinding(Queue stateChangedQueue) {
        return new Binding(stateChangedQueue.getName(), Binding.DestinationType.QUEUE,
                EventTopology.EXCHANGE, KEY_STATE_CHANGED, null);
    }
}
