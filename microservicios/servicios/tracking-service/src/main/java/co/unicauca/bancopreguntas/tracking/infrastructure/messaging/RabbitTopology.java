package co.unicauca.bancopreguntas.tracking.infrastructure.messaging;

import co.unicauca.bancopreguntas.platform.events.EventTopology;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Cola propia de este servicio, con la que escucha al servicio de simulacros. */
@Configuration
public class RabbitTopology {

    public static final String QUEUE_SIMULATION_COMPLETED = "tracking.q.simulation-completed";
    public static final String KEY_SIMULATION_COMPLETED = "simulation.completed";

    @Bean
    Queue simulationCompletedQueue() {
        return QueueBuilder.durable(QUEUE_SIMULATION_COMPLETED).build();
    }

    @Bean
    Binding simulationCompletedBinding(Queue simulationCompletedQueue) {
        return new Binding(simulationCompletedQueue.getName(), Binding.DestinationType.QUEUE,
                EventTopology.EXCHANGE, KEY_SIMULATION_COMPLETED, null);
    }
}
