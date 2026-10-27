package co.unicauca.bancopreguntas.assignment.infrastructure.messaging;

import co.unicauca.bancopreguntas.assignment.application.AssignmentApplicationService;
import co.unicauca.bancopreguntas.assignment.domain.Decision;
import co.unicauca.bancopreguntas.platform.events.IncomingEventParser;
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

    private final AssignmentApplicationService asignaciones = mock(AssignmentApplicationService.class);
    private final ReviewEventsListener escucha = new ReviewEventsListener(asignaciones, new IncomingEventParser(new ObjectMapper()));

    private static Message mensaje(String json) {
        return MessageBuilder.withBody(json.getBytes(StandardCharsets.UTF_8)).build();
    }

    @Test
    @DisplayName("review.evaluation-submitted pasa la pregunta, la ronda, el revisor y su decisión")
    void evaluada() {
        escucha.alEvaluarseUnaPregunta(mensaje("""
                {"eventId":"e-1","type":"review.evaluation-submitted","occurredAt":"2026-10-08T15:00:00Z",
                 "data":{"questionId":"Q-1","authorId":"a","round":2,"reviewerId":"r1","decision":"RECHAZAR",
                         "observation":"Mal","late":false}}
                """));

        verify(asignaciones).alEvaluarseUnaPregunta("e-1", "Q-1", 2, "r1", Decision.RECHAZAR);
    }

    @Test
    @DisplayName("review.completed con APPROVED o REJECTED pasa el resultado de la ronda")
    void completada() {
        escucha.alCompletarseLaRevision(mensaje("""
                {"eventId":"e-2","data":{"questionId":"Q-1","authorId":"a","round":1,"result":"APPROVED","observations":[]}}
                """));
        escucha.alCompletarseLaRevision(mensaje("""
                {"eventId":"e-3","data":{"questionId":"Q-2","round":3,"result":"REJECTED","observations":[]}}
                """));

        verify(asignaciones).alCompletarseLaRevision("e-2", "Q-1", 1, true);
        verify(asignaciones).alCompletarseLaRevision("e-3", "Q-2", 3, false);
    }

    @Test
    @DisplayName("una decisión o un resultado desconocidos se descartan sin reencolar")
    void desconocidos() {
        assertThatThrownBy(() -> escucha.alEvaluarseUnaPregunta(mensaje("""
                {"eventId":"e-4","data":{"questionId":"Q-1","round":1,"reviewerId":"r1","decision":"QUIZAS"}}
                """))).isInstanceOf(AmqpRejectAndDontRequeueException.class);
        assertThatThrownBy(() -> escucha.alCompletarseLaRevision(mensaje("""
                {"eventId":"e-5","data":{"questionId":"Q-1","round":1,"result":"QUIZAS"}}
                """))).isInstanceOf(AmqpRejectAndDontRequeueException.class);
        verifyNoInteractions(asignaciones);
    }

    @Test
    @DisplayName("un mensaje que no es JSON o sin questionId se descarta sin reencolar")
    void invalidos() {
        assertThatThrownBy(() -> escucha.alEvaluarseUnaPregunta(mensaje("esto no es json")))
                .isInstanceOf(AmqpRejectAndDontRequeueException.class);
        assertThatThrownBy(() -> escucha.alCompletarseLaRevision(mensaje("{\"eventId\":\"e-6\",\"data\":{\"result\":\"APPROVED\"}}")))
                .isInstanceOf(AmqpRejectAndDontRequeueException.class);
        verifyNoInteractions(asignaciones);
    }
}
