package co.unicauca.bancopreguntas.email.infrastructure.messaging;

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
 * La cola propia de este servicio, enlazada al exchange común: solo escucha {@code notification.created}. El exchange
 * lo declara el módulo {@code bp-platform}.
 */
@Configuration
public class RabbitTopology {

    public static final String QUEUE_NOTIFICATION_CREATED = "email.q.notification-created";

    @Bean
    Declarables emailQueues() {
        List<org.springframework.amqp.core.Declarable> declarables = new ArrayList<>();
        Queue queue = QueueBuilder.durable(QUEUE_NOTIFICATION_CREATED).build();
        declarables.add(queue);
        declarables.add(new Binding(QUEUE_NOTIFICATION_CREATED, Binding.DestinationType.QUEUE, EventTopology.EXCHANGE,
                "notification.created", null));
        return new Declarables(declarables);
    }
}
