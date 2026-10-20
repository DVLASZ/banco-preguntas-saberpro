package co.unicauca.bancopreguntas.simulation.infrastructure.messaging;

import co.unicauca.bancopreguntas.platform.events.IncomingEventParser;
import co.unicauca.bancopreguntas.simulation.application.SimulationApplicationService;
import co.unicauca.bancopreguntas.simulation.domain.PreguntaDeCatalogo;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@DisplayName("QuestionEventsListener")
class QuestionEventsListenerTest {

    private final SimulationApplicationService servicio = mock(SimulationApplicationService.class);
    private final QuestionEventsListener escucha = new QuestionEventsListener(servicio, new IncomingEventParser(new ObjectMapper()));

    private static Message mensaje(String json) {
        return MessageBuilder.withBody(json.getBytes(StandardCharsets.UTF_8)).build();
    }

    private static final String PUBLICADA = """
            {"eventId":"e-1","occurredAt":"2026-10-16T10:00:00Z","data":{"questionId":"Q-1","publishedBy":"admin","version":2,
             "title":"Observer","competency":"lectura_critica","topic":"Patrones","subtopic":"Observer","difficulty":"INTERMEDIO",
             "context":"Contexto","statement":"¿Qué patrón?","optionA":"A1","optionB":"B1","optionC":"C1","optionD":"D1","correctAnswer":"b"}}
            """;

    @Test
    @DisplayName("question.published arma la pregunta del catálogo con todo su contenido")
    void publicada() {
        escucha.alPublicarseUnaPregunta(mensaje(PUBLICADA));

        ArgumentCaptor<PreguntaDeCatalogo> captura = ArgumentCaptor.forClass(PreguntaDeCatalogo.class);
        verify(servicio).alPublicarseUnaPregunta(eq("e-1"), captura.capture());
        PreguntaDeCatalogo p = captura.getValue();
        assertThat(p.preguntaId()).isEqualTo("Q-1");
        assertThat(p.version()).isEqualTo(2);
        assertThat(p.competencia()).isEqualTo("LECTURA_CRITICA");
        assertThat(p.dificultad()).isEqualTo("INTERMEDIO");
        assertThat(p.enunciado()).isEqualTo("¿Qué patrón?");
        assertThat(p.opcionB()).isEqualTo("B1");
        assertThat(p.respuestaCorrecta()).isEqualTo('B');
        assertThat(p.publicadaEn()).isEqualTo(java.time.Instant.parse("2026-10-16T10:00:00Z"));
        assertThat(p.disponible()).isTrue();
    }

    @Test
    @DisplayName("un evento sin contenido de la pregunta se descarta sin reencolar")
    void publicadaSinContenido() {
        assertThatThrownBy(() -> escucha.alPublicarseUnaPregunta(mensaje("""
                {"eventId":"e-2","data":{"questionId":"Q-1","publishedBy":"admin"}}
                """))).isInstanceOf(AmqpRejectAndDontRequeueException.class);
        verifyNoInteractions(servicio);
    }

    @Test
    @DisplayName("una respuesta correcta inválida se descarta sin reencolar")
    void respuestaInvalida() {
        assertThatThrownBy(() -> escucha.alPublicarseUnaPregunta(mensaje(PUBLICADA.replace("\"b\"", "\"Z\""))))
                .isInstanceOf(AmqpRejectAndDontRequeueException.class);
        verifyNoInteractions(servicio);
    }

    @Test
    @DisplayName("question.state-changed pasa la pregunta y el estado destino")
    void cambioDeEstado() {
        escucha.alCambiarElEstadoDeUnaPregunta(mensaje("""
                {"eventId":"e-3","data":{"questionId":"Q-1","from":"PUBLICADA","to":"ARCHIVADA","changedBy":"admin"}}
                """));

        verify(servicio).alCambiarElEstadoDeUnaPregunta("e-3", "Q-1", "ARCHIVADA");
    }
}
