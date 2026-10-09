package co.unicauca.bancopreguntas.question.infrastructure.messaging;

import co.unicauca.bancopreguntas.platform.events.IncomingEventParser;
import co.unicauca.bancopreguntas.question.application.QuestionApplicationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@DisplayName("ReviewEventsListener")
class ReviewEventsListenerTest {

    private final QuestionApplicationService preguntas = mock(QuestionApplicationService.class);
    private final ReviewEventsListener escucha = new ReviewEventsListener(preguntas, new IncomingEventParser(new ObjectMapper()));

    private static Message mensaje(String json) {
        return MessageBuilder.withBody(json.getBytes(StandardCharsets.UTF_8)).build();
    }

    @Test
    @DisplayName("review.reviewers-assigned aplica la asignación a la pregunta indicada")
    void alAsignarRevisores() {
        escucha.alAsignarRevisores(mensaje("""
                {"eventId":"e-1","type":"review.reviewers-assigned","occurredAt":"2026-10-08T15:00:00Z",
                 "data":{"questionId":"Q-1","authorId":"a","reviewerIds":["r1","r2"],"assignedBy":"admin"}}
                """));

        verify(preguntas).aplicarRevisoresAsignados("e-1", "Q-1");
    }

    @Test
    @DisplayName("review.completed con APPROVED marca la pregunta como aprobada")
    void alCompletarseAprobada() {
        escucha.alCompletarseLaRevision(mensaje("""
                {"eventId":"e-2","type":"review.completed","occurredAt":"2026-10-08T15:00:00Z",
                 "data":{"questionId":"Q-1","authorId":"a","result":"APPROVED","observations":[]}}
                """));

        verify(preguntas).aplicarRevisionCompletada("e-2", "Q-1", true);
    }

    @Test
    @DisplayName("review.completed con REJECTED marca la pregunta como rechazada")
    void alCompletarseRechazada() {
        escucha.alCompletarseLaRevision(mensaje("""
                {"eventId":"e-3","type":"review.completed","occurredAt":"2026-10-08T15:00:00Z",
                 "data":{"questionId":"Q-1","authorId":"a","result":"REJECTED","observations":[]}}
                """));

        verify(preguntas).aplicarRevisionCompletada("e-3", "Q-1", false);
    }

    @Test
    @DisplayName("ignora los campos que no conoce (otros servicios pueden agregar datos)")
    void toleraCamposExtra() {
        escucha.alAsignarRevisores(mensaje("""
                {"eventId":"e-4","type":"review.reviewers-assigned","extra":123,
                 "data":{"questionId":"Q-9","nuevoCampo":{"a":1}}}
                """));

        verify(preguntas).aplicarRevisoresAsignados("e-4", "Q-9");
    }

    @Test
    @DisplayName("un resultado desconocido se descarta sin reencolar")
    void resultadoDesconocido() {
        assertThatThrownBy(() -> escucha.alCompletarseLaRevision(mensaje("""
                {"eventId":"e-5","data":{"questionId":"Q-1","result":"QUIZAS"}}
                """)))
                .isInstanceOf(AmqpRejectAndDontRequeueException.class);
        verifyNoInteractions(preguntas);
    }

    @Test
    @DisplayName("un mensaje que no es JSON se descarta sin reencolar")
    void noEsJson() {
        assertThatThrownBy(() -> escucha.alAsignarRevisores(mensaje("esto no es json")))
                .isInstanceOf(AmqpRejectAndDontRequeueException.class);
        verifyNoInteractions(preguntas);
    }

    @Test
    @DisplayName("un mensaje sin questionId o sin eventId se descarta sin reencolar")
    void faltanCampos() {
        assertThatThrownBy(() -> escucha.alAsignarRevisores(mensaje("""
                {"eventId":"e-6","data":{}}
                """)))
                .isInstanceOf(AmqpRejectAndDontRequeueException.class);
        assertThatThrownBy(() -> escucha.alAsignarRevisores(mensaje("""
                {"data":{"questionId":"Q-1"}}
                """)))
                .isInstanceOf(AmqpRejectAndDontRequeueException.class);
        verifyNoInteractions(preguntas);
    }
}
