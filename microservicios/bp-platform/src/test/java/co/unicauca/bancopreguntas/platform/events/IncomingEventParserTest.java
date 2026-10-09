package co.unicauca.bancopreguntas.platform.events;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("IncomingEventParser e IncomingEvent")
class IncomingEventParserTest {

    private final IncomingEventParser lector = new IncomingEventParser(new ObjectMapper());

    private static Message mensaje(String json) {
        return MessageBuilder.withBody(json.getBytes(StandardCharsets.UTF_8)).build();
    }

    private IncomingEvent evento(String json) {
        return lector.leer(mensaje(json));
    }

    @Test
    @DisplayName("lee el sobre completo")
    void leeElSobre() {
        IncomingEvent e = evento("""
                {"eventId":"e-1","type":"review.completed","occurredAt":"2026-10-08T15:00:00Z",
                 "data":{"questionId":"Q-1"}}
                """);

        assertThat(e.eventId()).isEqualTo("e-1");
        assertThat(e.type()).isEqualTo("review.completed");
        assertThat(e.occurredAt()).isEqualTo("2026-10-08T15:00:00Z");
        assertThat(e.requireText("questionId")).isEqualTo("Q-1");
    }

    @Test
    @DisplayName("tolera campos que no conoce")
    void toleraCamposExtra() {
        IncomingEvent e = evento("""
                {"eventId":"e-1","extra":123,"data":{"questionId":"Q-9","nuevo":{"a":1}}}
                """);

        assertThat(e.requireText("questionId")).isEqualTo("Q-9");
        assertThat(e.type()).isNull();
    }

    @Test
    @DisplayName("un mensaje que no es JSON se descarta sin reencolar")
    void noEsJson() {
        assertThatThrownBy(() -> evento("esto no es json")).isInstanceOf(AmqpRejectAndDontRequeueException.class);
    }

    @Test
    @DisplayName("un JSON que no es un objeto se descarta")
    void noEsObjeto() {
        assertThatThrownBy(() -> evento("[1,2,3]")).isInstanceOf(AmqpRejectAndDontRequeueException.class);
        assertThatThrownBy(() -> evento("")).isInstanceOf(AmqpRejectAndDontRequeueException.class);
    }

    @Test
    @DisplayName("sin eventId se descarta")
    void sinEventId() {
        assertThatThrownBy(() -> evento("{\"data\":{}}")).isInstanceOf(AmqpRejectAndDontRequeueException.class);
        assertThatThrownBy(() -> evento("{\"eventId\":\" \",\"data\":{}}"))
                .isInstanceOf(AmqpRejectAndDontRequeueException.class);
        assertThatThrownBy(() -> evento("{\"eventId\":5,\"data\":{}}"))
                .isInstanceOf(AmqpRejectAndDontRequeueException.class);
    }

    @Test
    @DisplayName("requireText descarta el mensaje si falta el campo")
    void requireTextFalta() {
        IncomingEvent e = evento("{\"eventId\":\"e-1\",\"type\":\"x\",\"data\":{\"a\":\"\",\"b\":3}}");

        assertThatThrownBy(() -> e.requireText("a")).isInstanceOf(AmqpRejectAndDontRequeueException.class);
        assertThatThrownBy(() -> e.requireText("b")).isInstanceOf(AmqpRejectAndDontRequeueException.class);
        assertThatThrownBy(() -> e.requireText("c")).isInstanceOf(AmqpRejectAndDontRequeueException.class);
    }

    @Test
    @DisplayName("optionalText devuelve nulo si falta o está vacío")
    void optionalText() {
        IncomingEvent e = evento("{\"eventId\":\"e-1\",\"data\":{\"a\":\"hola\",\"b\":null,\"c\":\" \"}}");

        assertThat(e.optionalText("a")).isEqualTo("hola");
        assertThat(e.optionalText("b")).isNull();
        assertThat(e.optionalText("c")).isNull();
        assertThat(e.optionalText("d")).isNull();
    }

    @Test
    @DisplayName("lee enteros obligatorios y opcionales")
    void enteros() {
        IncomingEvent e = evento("{\"eventId\":\"e-1\",\"data\":{\"n\":4,\"t\":\"4\"}}");

        assertThat(e.requireInt("n")).isEqualTo(4);
        assertThat(e.optionalInt("n", 1)).isEqualTo(4);
        assertThat(e.optionalInt("falta", 1)).isEqualTo(1);
        assertThatThrownBy(() -> e.requireInt("t")).isInstanceOf(AmqpRejectAndDontRequeueException.class);
        assertThatThrownBy(() -> e.requireInt("falta")).isInstanceOf(AmqpRejectAndDontRequeueException.class);
    }

    @Test
    @DisplayName("lee listas de textos, vacías incluidas, y descarta elementos inválidos")
    void listas() {
        IncomingEvent e = evento("""
                {"eventId":"e-1","data":{"ok":["a","b"],"vacia":[],"mala":["a",3],"noLista":"a"}}
                """);

        assertThat(e.requireTextList("ok")).containsExactly("a", "b");
        assertThat(e.requireTextList("vacia")).isEmpty();
        assertThatThrownBy(() -> e.requireTextList("mala")).isInstanceOf(AmqpRejectAndDontRequeueException.class);
        assertThatThrownBy(() -> e.requireTextList("noLista")).isInstanceOf(AmqpRejectAndDontRequeueException.class);
        assertThatThrownBy(() -> e.requireTextList("falta")).isInstanceOf(AmqpRejectAndDontRequeueException.class);
    }
}
