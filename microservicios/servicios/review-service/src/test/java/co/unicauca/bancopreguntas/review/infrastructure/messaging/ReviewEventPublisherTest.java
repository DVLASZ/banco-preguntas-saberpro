package co.unicauca.bancopreguntas.review.infrastructure.messaging;

import co.unicauca.bancopreguntas.platform.events.AmqpEventPublisher;
import co.unicauca.bancopreguntas.review.domain.event.EvaluationSubmitted;
import co.unicauca.bancopreguntas.review.domain.event.ReviewCompleted;
import co.unicauca.bancopreguntas.review.domain.event.ReviewersAssigned;
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

@DisplayName("ReviewEventPublisher")
class ReviewEventPublisherTest {

    private final RabbitTemplate rabbit = mock(RabbitTemplate.class);
    private final ObjectMapper json = new ObjectMapper();
    private final ReviewEventPublisher publicador = new ReviewEventPublisher(new AmqpEventPublisher(
            rabbit, json, Clock.fixed(Instant.parse("2026-10-08T15:00:00Z"), ZoneOffset.UTC)));

    private JsonNode publicado(String routingKey) throws Exception {
        ArgumentCaptor<Message> mensaje = ArgumentCaptor.forClass(Message.class);
        verify(rabbit).send(eq("bancopreguntas.events"), eq(routingKey), mensaje.capture());
        return json.readTree(mensaje.getValue().getBody());
    }

    @Test
    @DisplayName("review.reviewers-assigned lleva los revisores, quién los asignó y si fue automático")
    void asignados() throws Exception {
        publicador.publicar(new ReviewersAssigned("Q-1", "autor-1", 2, List.of("r1", "r2"), "system", true));

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
        assertThat(datos.has("type")).as("el tipo va solo en el sobre").isFalse();
    }

    @Test
    @DisplayName("review.evaluation-submitted marca las observaciones tardías")
    void evaluacionEnviada() throws Exception {
        publicador.publicar(new EvaluationSubmitted("Q-1", "autor-1", 1, "r2", "APROBAR", "Bien", true));

        JsonNode datos = publicado("review.evaluation-submitted").get("data");

        assertThat(datos.get("reviewerId").asText()).isEqualTo("r2");
        assertThat(datos.get("decision").asText()).isEqualTo("APROBAR");
        assertThat(datos.get("observation").asText()).isEqualTo("Bien");
        assertThat(datos.get("late").asBoolean()).isTrue();
    }

    @Test
    @DisplayName("review.completed lleva el resultado y las observaciones recibidas, con texto nulo si no hubo")
    void completada() throws Exception {
        publicador.publicar(new ReviewCompleted("Q-1", "autor-1", 1, ReviewCompleted.REJECTED, List.of(
                new ReviewCompleted.Observation("r1", "RECHAZAR", "Mal"),
                new ReviewCompleted.Observation("r2", "APROBAR", null))));

        JsonNode datos = publicado("review.completed").get("data");

        assertThat(datos.get("result").asText()).isEqualTo("REJECTED");
        assertThat(datos.get("round").asInt()).isEqualTo(1);
        assertThat(datos.get("observations")).hasSize(2);
        assertThat(datos.get("observations").get(0).get("text").asText()).isEqualTo("Mal");
        assertThat(datos.get("observations").get(1).get("text").isNull()).isTrue();
    }
}
