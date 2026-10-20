package co.unicauca.bancopreguntas.tracking.infrastructure.messaging;

import co.unicauca.bancopreguntas.platform.events.IncomingEvent;
import co.unicauca.bancopreguntas.platform.events.IncomingEventParser;
import co.unicauca.bancopreguntas.tracking.application.TrackingApplicationService;
import co.unicauca.bancopreguntas.tracking.domain.ResultadoPorCompetencia;
import co.unicauca.bancopreguntas.tracking.domain.ResultadoRegistrado;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

/**
 * Adaptador de entrada de mensajería: escucha {@code simulation.completed} y lo traduce a un caso de uso. Lee el
 * sobre como JSON genérico, sin depender de las clases del servicio de simulacros.
 */
@Component
public class SimulationEventsListener {

    private final TrackingApplicationService seguimiento;
    private final IncomingEventParser lector;

    public SimulationEventsListener(TrackingApplicationService seguimiento, IncomingEventParser lector) {
        this.seguimiento = seguimiento;
        this.lector = lector;
    }

    @RabbitListener(queues = RabbitTopology.QUEUE_SIMULATION_COMPLETED)
    public void alCompletarseUnSimulacro(Message mensaje) {
        IncomingEvent e = lector.leer(mensaje);
        JsonNode datos = e.data();
        List<ResultadoPorCompetencia> porCompetencia = new ArrayList<>();
        try {
            datos.path("byCompetency").forEach(c -> porCompetencia.add(new ResultadoPorCompetencia(
                    c.path("competency").asText(""), c.path("correct").asInt(), c.path("total").asInt())));
            ResultadoRegistrado resultado = new ResultadoRegistrado(e.requireText("simulationId"),
                    e.requireText("studentId"), instante(e.optionalText("startedAt")),
                    instante(e.requireText("finishedAt")), datos.path("durationSeconds").asLong(),
                    datos.path("expired").asBoolean(false), e.requireInt("total"), e.requireInt("correct"),
                    e.optionalInt("incorrect", 0), e.optionalInt("unanswered", 0), datos.path("score").asDouble(),
                    porCompetencia);
            seguimiento.alCompletarseUnSimulacro(e.eventId(), resultado);
        } catch (IllegalArgumentException | DateTimeParseException ex) {
            // un mensaje malformado no se arregla reintentándolo
            throw new AmqpRejectAndDontRequeueException("Resultado de simulacro inválido: " + ex.getMessage(), ex);
        }
    }

    private static Instant instante(String texto) {
        return texto == null ? null : Instant.parse(texto);
    }
}
