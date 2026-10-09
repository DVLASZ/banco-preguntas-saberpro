package co.unicauca.bancopreguntas.question.infrastructure.messaging;

import co.unicauca.bancopreguntas.platform.events.AmqpEventPublisher;
import co.unicauca.bancopreguntas.question.domain.event.QuestionPublished;
import co.unicauca.bancopreguntas.question.domain.event.QuestionReopened;
import co.unicauca.bancopreguntas.question.domain.event.QuestionStateChanged;
import co.unicauca.bancopreguntas.question.domain.event.QuestionSubmitted;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@DisplayName("QuestionEventPublisher")
class QuestionEventPublisherTest {

    private final RabbitTemplate rabbit = mock(RabbitTemplate.class);
    private final ObjectMapper json = new ObjectMapper();
    private final QuestionEventPublisher publicador = new QuestionEventPublisher(new AmqpEventPublisher(
            rabbit, json, Clock.fixed(Instant.parse("2026-10-08T15:00:00Z"), ZoneOffset.UTC)));

    private JsonNode publicado(String routingKey) throws Exception {
        ArgumentCaptor<Message> mensaje = ArgumentCaptor.forClass(Message.class);
        verify(rabbit).send(eq("bancopreguntas.events"), eq(routingKey), mensaje.capture());
        return json.readTree(mensaje.getValue().getBody());
    }

    @Test
    @DisplayName("question.submitted lleva el título, la competencia y la versión")
    void enviada() throws Exception {
        publicador.publicar(new QuestionSubmitted("Q-1", "autor-1", "Observer", "INGLES", 2));

        JsonNode sobre = publicado("question.submitted");

        assertThat(sobre.get("type").asText()).isEqualTo("question.submitted");
        JsonNode datos = sobre.get("data");
        assertThat(datos.get("questionId").asText()).isEqualTo("Q-1");
        assertThat(datos.get("authorId").asText()).isEqualTo("autor-1");
        assertThat(datos.get("title").asText()).isEqualTo("Observer");
        assertThat(datos.get("competency").asText()).isEqualTo("INGLES");
        assertThat(datos.get("version").asInt()).isEqualTo(2);
        assertThat(datos.has("type")).as("el tipo va solo en el sobre").isFalse();
    }

    @Test
    @DisplayName("question.state-changed conserva el origen, el destino y un usuario nulo")
    void cambioDeEstado() throws Exception {
        publicador.publicar(new QuestionStateChanged("Q-1", "PENDIENTE_REVISION", "EN_REVISION", null));

        JsonNode datos = publicado("question.state-changed").get("data");

        assertThat(datos.get("from").asText()).isEqualTo("PENDIENTE_REVISION");
        assertThat(datos.get("to").asText()).isEqualTo("EN_REVISION");
        assertThat(datos.get("changedBy").isNull()).isTrue();
    }

    @Test
    @DisplayName("question.reopened lleva la versión nueva")
    void reabierta() throws Exception {
        publicador.publicar(new QuestionReopened("Q-1", "autor-1", 3));

        JsonNode datos = publicado("question.reopened").get("data");

        assertThat(datos.get("questionId").asText()).isEqualTo("Q-1");
        assertThat(datos.get("authorId").asText()).isEqualTo("autor-1");
        assertThat(datos.get("version").asInt()).isEqualTo(3);
    }

    @Test
    @DisplayName("question.published lleva quién publicó")
    void publicada() throws Exception {
        publicador.publicar(new QuestionPublished("Q-1", "admin-1"));

        JsonNode datos = publicado("question.published").get("data");

        assertThat(datos.get("publishedBy").asText()).isEqualTo("admin-1");
    }
}
