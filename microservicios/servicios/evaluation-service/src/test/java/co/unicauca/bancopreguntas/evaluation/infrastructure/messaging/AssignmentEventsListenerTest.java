package co.unicauca.bancopreguntas.evaluation.infrastructure.messaging;

import co.unicauca.bancopreguntas.evaluation.application.EvaluationApplicationService;
import co.unicauca.bancopreguntas.platform.events.IncomingEventParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@DisplayName("AssignmentEventsListener")
class AssignmentEventsListenerTest {

    private final EvaluationApplicationService evaluaciones = mock(EvaluationApplicationService.class);
    private final AssignmentEventsListener escucha = new AssignmentEventsListener(evaluaciones, new IncomingEventParser(new ObjectMapper()));

    private static Message mensaje(String json) {
        return MessageBuilder.withBody(json.getBytes(StandardCharsets.UTF_8)).build();
    }

    @Test
    @DisplayName("review.reviewers-assigned pasa los revisores, la ronda, los datos de la pregunta y la hora del evento")
    void asignados() {
        escucha.alAsignarseRevisores(mensaje("""
                {"eventId":"e-1","type":"review.reviewers-assigned","occurredAt":"2026-10-08T15:00:00Z",
                 "data":{"questionId":"Q-1","authorId":"autor-1","round":2,"reviewerIds":["r1","r2"],"assignedBy":"system",
                         "automatic":true,"questionVersion":3,"title":"Observer","competency":"INGLES"}}
                """));

        verify(evaluaciones).alAsignarseRevisores("e-1", "Q-1", "autor-1", "Observer", "INGLES", 3, 2,
                List.of("r1", "r2"), "system", true, Instant.parse("2026-10-08T15:00:00Z"));
    }

    @Test
    @DisplayName("un aviso de un servicio anterior, sin versión ni título, usa la versión 1 y la ronda 1")
    void valoresPorDefecto() {
        escucha.alAsignarseRevisores(mensaje("""
                {"eventId":"e-2","data":{"questionId":"Q-1","authorId":"a","reviewerIds":["r1"]}}
                """));

        verify(evaluaciones).alAsignarseRevisores("e-2", "Q-1", "a", null, null, 1, 1, List.of("r1"), "system", false, null);
    }

    @Test
    @DisplayName("un mensaje sin revisores, sin questionId o que no es JSON se descarta sin reencolar")
    void invalidos() {
        assertThatThrownBy(() -> escucha.alAsignarseRevisores(mensaje("{\"eventId\":\"e-3\",\"data\":{\"questionId\":\"Q-1\",\"authorId\":\"a\"}}")))
                .isInstanceOf(AmqpRejectAndDontRequeueException.class);
        assertThatThrownBy(() -> escucha.alAsignarseRevisores(mensaje("{\"eventId\":\"e-4\",\"data\":{\"reviewerIds\":[\"r1\"]}}")))
                .isInstanceOf(AmqpRejectAndDontRequeueException.class);
        assertThatThrownBy(() -> escucha.alAsignarseRevisores(mensaje("no es json"))).isInstanceOf(AmqpRejectAndDontRequeueException.class);
        verifyNoInteractions(evaluaciones);
    }
}
