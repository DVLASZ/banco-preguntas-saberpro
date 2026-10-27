package co.unicauca.bancopreguntas.assignment.infrastructure.messaging;

import co.unicauca.bancopreguntas.platform.events.AmqpEventPublisher;
import co.unicauca.bancopreguntas.assignment.domain.event.ReviewersAssigned;
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
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@DisplayName("AssignmentEventPublisher")
class AssignmentEventPublisherTest {

    private final RabbitTemplate rabbit = mock(RabbitTemplate.class);
    private final ObjectMapper json = new ObjectMapper();
    private final AssignmentEventPublisher publicador = new AssignmentEventPublisher(new AmqpEventPublisher(
            rabbit, json, Clock.fixed(Instant.parse("2026-10-08T15:00:00Z"), ZoneOffset.UTC)));

    private JsonNode publicado(String routingKey) throws Exception {
        ArgumentCaptor<Message> mensaje = ArgumentCaptor.forClass(Message.class);
        verify(rabbit).send(eq("bancopreguntas.events"), eq(routingKey), mensaje.capture());
        return json.readTree(mensaje.getValue().getBody());
    }

    @Test
    @DisplayName("review.reviewers-assigned lleva los revisores, quién los asignó, si fue automático y los datos de la pregunta")
    void asignados() throws Exception {
        publicador.publicar(new ReviewersAssigned("Q-1", "autor-1", 2, List.of("r1", "r2"), "system", true, 3, "Observer", "INGLES"));

        JsonNode sobre = publicado("review.reviewers-assigned");

        assertThat(sobre.get("type").asText()).isEqualTo("review.reviewers-assigned");
        assertThat(sobre.get("eventId").asText()).isNotBlank();
        assertThat(sobre.get("occurredAt").asText()).isEqualTo("2026-10-08T15:00:00Z");
        JsonNode datos = sobre.get("data");
        assertThat(datos.get("questionId").asText()).isEqualTo("Q-1");
        assertThat(datos.get("authorId").asText()).isEqualTo("autor-1");
        assertThat(datos.get("round").asInt()).isEqualTo(2);
        assertThat(datos.get("reviewerIds")).extracting(JsonNode::asText).containsExactly("r1", "r2");
        assertThat(datos.get("assignedBy").asText()).isEqualTo("system");
        assertThat(datos.get("automatic").asBoolean()).isTrue();
        assertThat(datos.get("questionVersion").asInt()).isEqualTo(3);
        assertThat(datos.get("title").asText()).isEqualTo("Observer");
        assertThat(datos.get("competency").asText()).isEqualTo("INGLES");
        assertThat(datos.has("type")).as("el tipo va solo en el sobre").isFalse();
    }

}
