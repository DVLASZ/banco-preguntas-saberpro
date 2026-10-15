package co.unicauca.bancopreguntas.review.infrastructure.messaging;

import co.unicauca.bancopreguntas.platform.events.IncomingEventParser;
import co.unicauca.bancopreguntas.review.application.ReviewApplicationService;
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

@DisplayName("QuestionEventsListener")
class QuestionEventsListenerTest {

    private final ReviewApplicationService revisiones = mock(ReviewApplicationService.class);
    private final QuestionEventsListener escucha =
            new QuestionEventsListener(revisiones, new IncomingEventParser(new ObjectMapper()));

    private static Message mensaje(String json) {
        return MessageBuilder.withBody(json.getBytes(StandardCharsets.UTF_8)).build();
    }

    @Test
    @DisplayName("question.submitted abre la revisión con los datos de la pregunta y su versión")
    void enviada() {
        escucha.alEnviarseUnaPregunta(mensaje("""
                {"eventId":"e-1","type":"question.submitted","occurredAt":"2026-10-08T15:00:00Z",
                 "data":{"questionId":"Q-1","authorId":"autor-1","title":"Observer","competency":"INGLES","version":2}}
                """));

        verify(revisiones).alEnviarseUnaPregunta("e-1", "Q-1", "autor-1", "Observer", "INGLES", 2);
    }

    @Test
    @DisplayName("sin versión se asume la primera; título y competencia son opcionales")
    void versionPorDefecto() {
        escucha.alEnviarseUnaPregunta(mensaje("""
                {"eventId":"e-2","data":{"questionId":"Q-2","authorId":"autor-1"}}
                """));

        verify(revisiones).alEnviarseUnaPregunta("e-2", "Q-2", "autor-1", null, null, 1);
    }

    @Test
    @DisplayName("question.reopened cierra la ronda rechazada de la pregunta")
    void reabierta() {
        escucha.alReabrirseUnaPregunta(mensaje("""
                {"eventId":"e-3","type":"question.reopened",
                 "data":{"questionId":"Q-1","authorId":"autor-1","version":2}}
                """));

        verify(revisiones).alReabrirseUnaPregunta("e-3", "Q-1");
    }

    @Test
    @DisplayName("ignora los campos que no conoce (otros servicios pueden agregar datos)")
    void toleraCamposExtra() {
        escucha.alReabrirseUnaPregunta(mensaje("""
                {"eventId":"e-4","extra":1,"data":{"questionId":"Q-9","nuevo":{"a":1}}}
                """));

        verify(revisiones).alReabrirseUnaPregunta("e-4", "Q-9");
    }

    @Test
    @DisplayName("un mensaje que no es JSON se descarta sin reencolar")
    void noEsJson() {
        assertThatThrownBy(() -> escucha.alEnviarseUnaPregunta(mensaje("esto no es json")))
                .isInstanceOf(AmqpRejectAndDontRequeueException.class);
        assertThatThrownBy(() -> escucha.alReabrirseUnaPregunta(mensaje("[")))
                .isInstanceOf(AmqpRejectAndDontRequeueException.class);
        verifyNoInteractions(revisiones);
    }

    @Test
    @DisplayName("un mensaje sin eventId, questionId o authorId se descarta sin reencolar")
    void faltanCampos() {
        assertThatThrownBy(() -> escucha.alEnviarseUnaPregunta(mensaje("""
                {"eventId":"e-5","data":{"authorId":"a"}}
                """))).isInstanceOf(AmqpRejectAndDontRequeueException.class);
        assertThatThrownBy(() -> escucha.alEnviarseUnaPregunta(mensaje("""
                {"eventId":"e-6","data":{"questionId":"Q-1"}}
                """))).isInstanceOf(AmqpRejectAndDontRequeueException.class);
        assertThatThrownBy(() -> escucha.alReabrirseUnaPregunta(mensaje("""
                {"data":{"questionId":"Q-1"}}
                """))).isInstanceOf(AmqpRejectAndDontRequeueException.class);
        verifyNoInteractions(revisiones);
    }
}
