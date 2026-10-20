package co.unicauca.bancopreguntas.notification.infrastructure.messaging;

import co.unicauca.bancopreguntas.notification.application.NotificationApplicationService;
import co.unicauca.bancopreguntas.notification.domain.ObservacionDeRevisor;
import co.unicauca.bancopreguntas.platform.events.IncomingEventParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@DisplayName("EventsListener")
class EventsListenerTest {

    private final NotificationApplicationService servicio = mock(NotificationApplicationService.class);
    private final EventsListener escucha = new EventsListener(servicio, new IncomingEventParser(new ObjectMapper()));

    private static Message mensaje(String json) {
        return MessageBuilder.withBody(json.getBytes(StandardCharsets.UTF_8)).build();
    }

    @Test
    @DisplayName("question.submitted recuerda el título y el autor")
    void enviada() {
        escucha.alEnviarseUnaPregunta(mensaje("""
                {"eventId":"e-1","data":{"questionId":"Q-1","authorId":"a-1","title":"Observer","competency":"INGLES","version":1}}
                """));

        verify(servicio).alEnviarseUnaPregunta("e-1", "Q-1", "a-1", "Observer");
    }

    @Test
    @DisplayName("question.published solo necesita la pregunta")
    void publicada() {
        escucha.alPublicarseUnaPregunta(mensaje("""
                {"eventId":"e-2","data":{"questionId":"Q-1","publishedBy":"admin-1"}}
                """));

        verify(servicio).alPublicarseUnaPregunta("e-2", "Q-1");
    }

    @Test
    @DisplayName("review.reviewers-assigned trae los revisores, la ronda y si fue automática")
    void asignados() {
        escucha.alAsignarseRevisores(mensaje("""
                {"eventId":"e-3","data":{"questionId":"Q-1","authorId":"a-1","round":2,"reviewerIds":["r1","r2"],
                 "assignedBy":"system","automatic":true}}
                """));

        verify(servicio).alAsignarseRevisores("e-3", "Q-1", "a-1", 2, List.of("r1", "r2"), true, "system");
    }

    @Test
    @DisplayName("sin ronda ni bandera de automática se asumen los valores por defecto")
    void asignadosPorDefecto() {
        escucha.alAsignarseRevisores(mensaje("""
                {"eventId":"e-4","data":{"questionId":"Q-1","reviewerIds":["r1"]}}
                """));

        verify(servicio).alAsignarseRevisores("e-4", "Q-1", null, 1, List.of("r1"), false, null);
    }

    @Test
    @DisplayName("review.completed convierte el resultado y las observaciones")
    void completada() {
        escucha.alCompletarseUnaRevision(mensaje("""
                {"eventId":"e-5","data":{"questionId":"Q-1","authorId":"a-1","round":1,"result":"REJECTED",
                 "observations":[{"reviewerId":"r1","decision":"RECHAZAR","text":"Mal"},
                                 {"reviewerId":"r2","decision":"APROBAR","text":null}]}}
                """));

        verify(servicio).alCompletarseUnaRevision("e-5", "Q-1", "a-1", 1, false, List.of(
                new ObservacionDeRevisor("r1", "RECHAZAR", "Mal"), new ObservacionDeRevisor("r2", "APROBAR", null)));
    }

    @Test
    @DisplayName("review.completed APPROVED sin observaciones")
    void completadaAprobada() {
        escucha.alCompletarseUnaRevision(mensaje("""
                {"eventId":"e-6","data":{"questionId":"Q-1","authorId":"a-1","result":"APPROVED"}}
                """));

        verify(servicio).alCompletarseUnaRevision("e-6", "Q-1", "a-1", 1, true, List.of());
    }

    @Test
    @DisplayName("un resultado desconocido se descarta sin reencolar")
    void resultadoDesconocido() {
        assertThatThrownBy(() -> escucha.alCompletarseUnaRevision(mensaje("""
                {"eventId":"e-7","data":{"questionId":"Q-1","authorId":"a-1","result":"QUIZAS"}}
                """))).isInstanceOf(AmqpRejectAndDontRequeueException.class);
        verifyNoInteractions(servicio);
    }

    @Test
    @DisplayName("review.evaluation-submitted trae la bandera de tardía")
    void evaluacion() {
        escucha.alEnviarseUnaEvaluacion(mensaje("""
                {"eventId":"e-8","data":{"questionId":"Q-1","authorId":"a-1","round":1,"reviewerId":"r2",
                 "decision":"APROBAR","observation":"Bien","late":true}}
                """));

        verify(servicio).alEnviarseUnaEvaluacion("e-8", "Q-1", "a-1", 1, "r2", "APROBAR", "Bien", true);
    }

    @Test
    @DisplayName("los mensajes mal formados o incompletos se descartan sin reencolar")
    void malFormados() {
        assertThatThrownBy(() -> escucha.alEnviarseUnaPregunta(mensaje("no es json")))
                .isInstanceOf(AmqpRejectAndDontRequeueException.class);
        assertThatThrownBy(() -> escucha.alEnviarseUnaPregunta(mensaje("""
                {"eventId":"e-9","data":{"questionId":"Q-1"}}
                """))).isInstanceOf(AmqpRejectAndDontRequeueException.class);
        assertThatThrownBy(() -> escucha.alEnviarseUnaEvaluacion(mensaje("""
                {"eventId":"e-10","data":{"questionId":"Q-1","authorId":"a-1"}}
                """))).isInstanceOf(AmqpRejectAndDontRequeueException.class);
        assertThatThrownBy(() -> escucha.alPublicarseUnaPregunta(mensaje("""
                {"data":{"questionId":"Q-1"}}
                """))).isInstanceOf(AmqpRejectAndDontRequeueException.class);
        verifyNoInteractions(servicio);
    }
}
