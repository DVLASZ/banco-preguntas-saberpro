package co.unicauca.bancopreguntas.email.infrastructure.messaging;

import co.unicauca.bancopreguntas.email.application.EmailApplicationService;
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

@DisplayName("EventsListener del servicio de correo")
class EventsListenerTest {

    private final EmailApplicationService servicio = mock(EmailApplicationService.class);
    private final EventsListener escucha = new EventsListener(servicio, new IncomingEventParser(new ObjectMapper()));

    private static Message mensaje(String json) {
        return MessageBuilder.withBody(json.getBytes(StandardCharsets.UTF_8)).build();
    }

    @Test
    @DisplayName("notification.created se traduce al caso de uso con la notificación, el destinatario y el texto")
    void creada() {
        escucha.alCrearseUnaNotificacion(mensaje("""
                {"eventId":"e-1","data":{"notificationId":"n-1","recipientId":"autor-1","kind":"REVISION_APROBADA",
                 "questionId":"q-1","subject":"Pregunta aprobada","message":"Cuerpo"}}
                """));

        verify(servicio).alCrearseUnaNotificacion("e-1", "n-1", "autor-1", "Pregunta aprobada", "Cuerpo");
    }

    @Test
    @DisplayName("un mensaje al que le falta el destinatario o el texto se descarta sin reencolar")
    void incompleto() {
        assertThatThrownBy(() -> escucha.alCrearseUnaNotificacion(mensaje("""
                {"eventId":"e-1","data":{"notificationId":"n-1","subject":"A","message":"B"}}
                """))).isInstanceOf(AmqpRejectAndDontRequeueException.class);
        assertThatThrownBy(() -> escucha.alCrearseUnaNotificacion(mensaje("""
                {"eventId":"e-1","data":{"notificationId":"n-1","recipientId":"u","message":"B"}}
                """))).isInstanceOf(AmqpRejectAndDontRequeueException.class);

        verifyNoInteractions(servicio);
    }

    @Test
    @DisplayName("un mensaje que no es JSON se descarta sin reencolar")
    void malformado() {
        assertThatThrownBy(() -> escucha.alCrearseUnaNotificacion(mensaje("no es json")))
                .isInstanceOf(AmqpRejectAndDontRequeueException.class);
    }
}
