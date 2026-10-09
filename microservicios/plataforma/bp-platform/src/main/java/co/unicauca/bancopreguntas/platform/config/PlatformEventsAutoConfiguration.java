package co.unicauca.bancopreguntas.platform.config;

import co.unicauca.bancopreguntas.platform.events.AmqpEventPublisher;
import co.unicauca.bancopreguntas.platform.events.EventPublisher;
import co.unicauca.bancopreguntas.platform.events.EventTopology;
import co.unicauca.bancopreguntas.platform.events.IncomingEventParser;
import co.unicauca.bancopreguntas.platform.events.OutboxEventPublisher;
import co.unicauca.bancopreguntas.platform.events.OutboxRelay;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.jdbc.JdbcTemplateAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.PlatformTransactionManager;

import java.time.Clock;
import java.time.Duration;

/**
 * Parte de mensajería de la configuración transversal: el exchange común, el publicador y el lector.
 *
 * <p>Si el servicio tiene base de datos, los eventos se publican con el patrón Outbox (ver
 * {@link OutboxEventPublisher}); necesita la tabla {@code outbox_event} en la base del servicio. Con
 * {@code bancopreguntas.events.outbox.enabled=false} se publican directamente después del commit.
 */
@AutoConfiguration(after = {RabbitAutoConfiguration.class, JdbcTemplateAutoConfiguration.class,
        org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration.class,
        org.springframework.boot.autoconfigure.transaction.TransactionAutoConfiguration.class,
        org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration.class})
@ConditionalOnClass(RabbitTemplate.class)
public class PlatformEventsAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    TopicExchange bancoPreguntasEventsExchange() {
        return new TopicExchange(EventTopology.EXCHANGE, true, false);
    }

    @Bean
    @ConditionalOnMissingBean
    AmqpEventPublisher amqpEventPublisher(RabbitTemplate rabbit, ObjectMapper json, Clock reloj) {
        return new AmqpEventPublisher(rabbit, json, reloj);
    }

    @Bean
    @ConditionalOnMissingBean
    IncomingEventParser incomingEventParser(ObjectMapper json) {
        return new IncomingEventParser(json);
    }

    /** El Outbox: el publicador que guarda en la base y el relevo que envía al broker. */
    @Configuration(proxyBeanMethods = false)
    @EnableScheduling
    @ConditionalOnClass(JdbcTemplate.class)
    @ConditionalOnBean({JdbcTemplate.class, PlatformTransactionManager.class})
    @ConditionalOnProperty(name = "bancopreguntas.events.outbox.enabled", havingValue = "true", matchIfMissing = true)
    static class Outbox {

        @Bean
        @Primary
        OutboxEventPublisher outboxEventPublisher(JdbcTemplate jdbc, ObjectMapper json, Clock reloj) {
            return new OutboxEventPublisher(jdbc, json, reloj);
        }

        @Bean
        OutboxRelay outboxRelay(JdbcTemplate jdbc, PlatformTransactionManager transacciones, RabbitTemplate rabbit,
                                Clock reloj,
                                @Value("${bancopreguntas.events.outbox.batch-size:100}") int lote,
                                @Value("${bancopreguntas.events.outbox.keep-sent:PT24H}") Duration conservarEnviados) {
            return new OutboxRelay(jdbc, transacciones, rabbit, reloj, lote, conservarEnviados);
        }
    }
}
