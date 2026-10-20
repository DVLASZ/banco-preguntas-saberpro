package co.unicauca.bancopreguntas.history.infrastructure.messaging;

import co.unicauca.bancopreguntas.history.application.HistoryApplicationService;
import co.unicauca.bancopreguntas.history.domain.EventoRecibido;
import co.unicauca.bancopreguntas.platform.events.IncomingEventParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@DisplayName("EventsListener")
class EventsListenerTest {

    private final HistoryApplicationService servicio = mock(HistoryApplicationService.class);
    private final EventsListener escucha = new EventsListener(servicio, new IncomingEventParser(new ObjectMapper()));

    private static Message mensaje(String json) {
        return MessageBuilder.withBody(json.getBytes(StandardCharsets.UTF_8)).build();
    }

    @Test
    @DisplayName("question.submitted conserva el sobre: id, fecha y cuerpo")
    void enviada() {
        escucha.alEnviarseUnaPregunta(mensaje("""
                {"eventId":"e-1","type":"question.submitted","occurredAt":"2026-10-08T15:00:00Z",
                 "data":{"questionId":"Q-1","authorId":"a-1","title":"Observer","version":2}}
                """));

        ArgumentCaptor<EventoRecibido> sobre = ArgumentCaptor.forClass(EventoRecibido.class);
        verify(servicio).alEnviarseUnaPregunta(sobre.capture(), eq("Q-1"), eq("a-1"), eq("Observer"), eq(2));
        assertThat(sobre.getValue().eventId()).isEqualTo("e-1");
        assertThat(sobre.getValue().ocurridaEn()).isEqualTo(Instant.parse("2026-10-08T15:00:00Z"));
        assertThat(sobre.getValue().detalles()).contains("\"questionId\":\"Q-1\"");
    }

    @Test
    @DisplayName("una fecha ausente o inválida se reemplaza por el momento de la recepción")
    void fechaInvalida() {
        Instant antes = Instant.now();

        escucha.alPublicarseUnaPregunta(mensaje("""
                {"eventId":"e-2","occurredAt":"ayer","data":{"questionId":"Q-1","publishedBy":"admin-1"}}
                """));

        ArgumentCaptor<EventoRecibido> sobre = ArgumentCaptor.forClass(EventoRecibido.class);
        verify(servicio).alPublicarseUnaPregunta(sobre.capture(), eq("Q-1"), eq("admin-1"));
        assertThat(sobre.getValue().ocurridaEn()).isAfterOrEqualTo(antes);
    }

    @Test
    @DisplayName("question.reopened y question.state-changed")
    void reabiertaYEstado() {
        escucha.alReabrirseUnaPregunta(mensaje("""
                {"eventId":"e-3","data":{"questionId":"Q-1","authorId":"a-1","version":3}}
                """));
        escucha.alCambiarDeEstado(mensaje("""
                {"eventId":"e-4","data":{"questionId":"Q-1","from":"BORRADOR","to":"PENDIENTE_REVISION","changedBy":null}}
                """));

        verify(servicio).alReabrirseUnaPregunta(org.mockito.ArgumentMatchers.any(), eq("Q-1"), eq("a-1"), eq(3));
        verify(servicio).alCambiarDeEstado(org.mockito.ArgumentMatchers.any(), eq("Q-1"), eq("BORRADOR"),
                eq("PENDIENTE_REVISION"), eq((String) null));
    }

    @Test
    @DisplayName("review.reviewers-assigned trae los revisores, quién los asignó y si fue automático")
    void asignados() {
        escucha.alAsignarseRevisores(mensaje("""
                {"eventId":"e-5","data":{"questionId":"Q-1","round":2,"reviewerIds":["r1","r2"],
                 "assignedBy":"system","automatic":true}}
                """));

        verify(servicio).alAsignarseRevisores(org.mockito.ArgumentMatchers.any(), eq("Q-1"), eq(2),
                eq(List.of("r1", "r2")), eq("system"), eq(true));
    }

    @Test
    @DisplayName("review.evaluation-submitted y review.completed")
    void revision() {
        escucha.alEnviarseUnaEvaluacion(mensaje("""
                {"eventId":"e-6","data":{"questionId":"Q-1","round":1,"reviewerId":"r2","decision":"APROBAR","late":true}}
                """));
        escucha.alCompletarseUnaRevision(mensaje("""
                {"eventId":"e-7","data":{"questionId":"Q-1","round":1,"result":"APPROVED"}}
                """));

        verify(servicio).alEnviarseUnaEvaluacion(org.mockito.ArgumentMatchers.any(), eq("Q-1"), eq(1), eq("r2"),
                eq("APROBAR"), eq(true));
        verify(servicio).alCompletarseUnaRevision(org.mockito.ArgumentMatchers.any(), eq("Q-1"), eq(1), eq(true));
    }

    @Test
    @DisplayName("los inicios de sesión exitosos y fallidos llegan por sus propias colas")
    void sesiones() {
        escucha.alIniciarSesion(mensaje("""
                {"eventId":"e-8","data":{"userId":"autor-1"}}
                """));
        escucha.alFallarElInicioDeSesion(mensaje("""
                {"eventId":"e-9","data":{"userId":"intruso"}}
                """));

        verify(servicio).alIniciarSesion(org.mockito.ArgumentMatchers.any(), eq("autor-1"), eq(true));
        verify(servicio).alIniciarSesion(org.mockito.ArgumentMatchers.any(), eq("intruso"), eq(false));
    }

    @Test
    @DisplayName("un resultado de revisión desconocido se descarta sin reencolar")
    void resultadoDesconocido() {
        assertThatThrownBy(() -> escucha.alCompletarseUnaRevision(mensaje("""
                {"eventId":"e-10","data":{"questionId":"Q-1","result":"QUIZAS"}}
                """))).isInstanceOf(AmqpRejectAndDontRequeueException.class);
        verifyNoInteractions(servicio);
    }

    @Test
    @DisplayName("los mensajes mal formados o incompletos se descartan sin reencolar")
    void malFormados() {
        assertThatThrownBy(() -> escucha.alEnviarseUnaPregunta(mensaje("no es json")))
                .isInstanceOf(AmqpRejectAndDontRequeueException.class);
        assertThatThrownBy(() -> escucha.alEnviarseUnaPregunta(mensaje("""
                {"eventId":"e-11","data":{"questionId":"Q-1"}}
                """))).isInstanceOf(AmqpRejectAndDontRequeueException.class);
        assertThatThrownBy(() -> escucha.alIniciarSesion(mensaje("""
                {"eventId":"e-12","data":{}}
                """))).isInstanceOf(AmqpRejectAndDontRequeueException.class);
        verifyNoInteractions(servicio);
    }
}
