package co.unicauca.bancopreguntas.tracking.infrastructure.messaging;

import co.unicauca.bancopreguntas.platform.events.IncomingEventParser;
import co.unicauca.bancopreguntas.tracking.application.TrackingApplicationService;
import co.unicauca.bancopreguntas.tracking.domain.ResultadoPorCompetencia;
import co.unicauca.bancopreguntas.tracking.domain.ResultadoRegistrado;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;

import java.nio.charset.StandardCharsets;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@DisplayName("SimulationEventsListener")
class SimulationEventsListenerTest {

    private final TrackingApplicationService servicio = mock(TrackingApplicationService.class);
    private final SimulationEventsListener escucha = new SimulationEventsListener(servicio, new IncomingEventParser(new ObjectMapper()));

    private static Message mensaje(String json) {
        return MessageBuilder.withBody(json.getBytes(StandardCharsets.UTF_8)).build();
    }

    private static final String COMPLETADO = """
            {"eventId":"e-1","data":{"simulationId":"s-1","studentId":"ana","startedAt":"2026-10-16T10:00:00Z",
             "finishedAt":"2026-10-16T10:25:00Z","durationSeconds":1500,"expired":true,"total":10,"correct":6,"incorrect":3,
             "unanswered":1,"score":60.0,"byCompetency":[{"competency":"INGLES","correct":2,"total":4},
             {"competency":"LECTURA_CRITICA","correct":4,"total":6}]}}
            """;

    @Test
    @DisplayName("simulation.completed se convierte en un resultado con su detalle por competencia")
    void completado() {
        escucha.alCompletarseUnSimulacro(mensaje(COMPLETADO));

        ArgumentCaptor<ResultadoRegistrado> captura = ArgumentCaptor.forClass(ResultadoRegistrado.class);
        verify(servicio).alCompletarseUnSimulacro(eq("e-1"), captura.capture());
        ResultadoRegistrado r = captura.getValue();
        assertThat(r.simulacroId()).isEqualTo("s-1");
        assertThat(r.estudianteId()).isEqualTo("ana");
        assertThat(r.iniciadoEn()).isEqualTo(Instant.parse("2026-10-16T10:00:00Z"));
        assertThat(r.finalizadoEn()).isEqualTo(Instant.parse("2026-10-16T10:25:00Z"));
        assertThat(r.duracionSegundos()).isEqualTo(1500);
        assertThat(r.expirado()).isTrue();
        assertThat(r.total()).isEqualTo(10);
        assertThat(r.correctas()).isEqualTo(6);
        assertThat(r.incorrectas()).isEqualTo(3);
        assertThat(r.sinResponder()).isEqualTo(1);
        assertThat(r.puntaje()).isEqualTo(60.0);
        assertThat(r.porCompetencia()).containsExactly(new ResultadoPorCompetencia("INGLES", 2, 4),
                new ResultadoPorCompetencia("LECTURA_CRITICA", 4, 6));
    }

    @Test
    @DisplayName("sin la fecha de fin o con datos incoherentes, el mensaje se descarta sin reencolar")
    void malformado() {
        assertThatThrownBy(() -> escucha.alCompletarseUnSimulacro(mensaje("""
                {"eventId":"e-2","data":{"simulationId":"s-1","studentId":"ana","total":10,"correct":6}}
                """))).isInstanceOf(AmqpRejectAndDontRequeueException.class);
        assertThatThrownBy(() -> escucha.alCompletarseUnSimulacro(mensaje("""
                {"eventId":"e-3","data":{"simulationId":"s-1","studentId":"ana","finishedAt":"2026-10-16T10:25:00Z","total":10,"correct":11}}
                """))).isInstanceOf(AmqpRejectAndDontRequeueException.class);
        assertThatThrownBy(() -> escucha.alCompletarseUnSimulacro(mensaje("""
                {"eventId":"e-4","data":{"simulationId":"s-1","studentId":"ana","finishedAt":"ayer","total":10,"correct":5}}
                """))).isInstanceOf(AmqpRejectAndDontRequeueException.class);
        verifyNoInteractions(servicio);
    }
}
