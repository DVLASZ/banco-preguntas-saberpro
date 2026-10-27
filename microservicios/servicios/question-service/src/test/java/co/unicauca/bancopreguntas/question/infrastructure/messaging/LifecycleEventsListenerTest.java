package co.unicauca.bancopreguntas.question.infrastructure.messaging;

import co.unicauca.bancopreguntas.platform.events.IncomingEventParser;
import co.unicauca.bancopreguntas.question.application.QuestionApplicationService;
import co.unicauca.bancopreguntas.question.domain.EstadoPregunta;
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

@DisplayName("LifecycleEventsListener")
class LifecycleEventsListenerTest {

    private final QuestionApplicationService preguntas = mock(QuestionApplicationService.class);
    private final LifecycleEventsListener escucha = new LifecycleEventsListener(preguntas, new IncomingEventParser(new ObjectMapper()));

    private static Message mensaje(String json) {
        return MessageBuilder.withBody(json.getBytes(StandardCharsets.UTF_8)).build();
    }

    @Test
    @DisplayName("question.state-changed pasa la pregunta, el estado destino, la versión y la secuencia")
    void alCambiarElEstado() {
        escucha.alCambiarElEstado(mensaje("""
                {"eventId":"e-1","type":"question.state-changed","occurredAt":"2026-10-08T15:00:00Z",
                 "data":{"questionId":"Q-1","from":"PENDIENTE_REVISION","to":"EN_REVISION","changedBy":null,"sequence":3,"version":2}}
                """));

        verify(preguntas).alCambiarElEstado("Q-1", EstadoPregunta.EN_REVISION, 2, 3);
    }

    @Test
    @DisplayName("un aviso sin versión ni secuencia (de un servicio anterior) usa la versión 1 y la secuencia 0")
    void sinVersionNiSecuencia() {
        escucha.alCambiarElEstado(mensaje("""
                {"eventId":"e-2","data":{"questionId":"Q-1","from":"BORRADOR","to":"PENDIENTE_REVISION"}}
                """));

        verify(preguntas).alCambiarElEstado("Q-1", EstadoPregunta.PENDIENTE_REVISION, 1, 0);
    }

    @Test
    @DisplayName("ignora los campos que no conoce (otros servicios pueden agregar datos)")
    void toleraCamposExtra() {
        escucha.alCambiarElEstado(mensaje("""
                {"eventId":"e-3","extra":123,"data":{"questionId":"Q-9","to":"APROBADA","nuevoCampo":{"a":1},"sequence":4}}
                """));

        verify(preguntas).alCambiarElEstado("Q-9", EstadoPregunta.APROBADA, 1, 4);
    }

    @Test
    @DisplayName("un estado desconocido se descarta sin reencolar")
    void estadoDesconocido() {
        assertThatThrownBy(() -> escucha.alCambiarElEstado(mensaje("""
                {"eventId":"e-4","data":{"questionId":"Q-1","to":"QUIZAS"}}
                """))).isInstanceOf(AmqpRejectAndDontRequeueException.class);
        verifyNoInteractions(preguntas);
    }

    @Test
    @DisplayName("un mensaje que no es JSON o sin questionId se descarta sin reencolar")
    void mensajeInvalido() {
        assertThatThrownBy(() -> escucha.alCambiarElEstado(mensaje("esto no es json")))
                .isInstanceOf(AmqpRejectAndDontRequeueException.class);
        assertThatThrownBy(() -> escucha.alCambiarElEstado(mensaje("{\"eventId\":\"e-5\",\"data\":{\"to\":\"APROBADA\"}}")))
                .isInstanceOf(AmqpRejectAndDontRequeueException.class);
        verifyNoInteractions(preguntas);
    }
}
