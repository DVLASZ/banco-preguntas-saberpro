package co.unicauca.bancopreguntas.email.application;

import co.unicauca.bancopreguntas.email.domain.CorreoEnviado;
import co.unicauca.bancopreguntas.email.domain.EstadoCorreo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("EmailApplicationService (casos de uso)")
class EmailApplicationServiceTest {

    private static final Instant T0 = Instant.parse("2026-10-08T10:00:00Z");

    private RecordingEmailSender sender;
    private InMemorySentEmailRepository enviados;
    private RelojManual reloj;
    private EmailApplicationService servicio;

    @BeforeEach
    void preparar() {
        sender = new RecordingEmailSender();
        enviados = new InMemorySentEmailRepository();
        reloj = new RelojManual(T0);
        AtomicInteger ids = new AtomicInteger();
        servicio = new EmailApplicationService(sender, enviados, new InMemoryProcessedEvents(), reloj,
                () -> "c-" + ids.incrementAndGet(), usuario -> usuario + "@correo.test");
    }

    @Test
    @DisplayName("una notificación creada se envía a la dirección derivada del usuario y queda registrada")
    void envia() {
        servicio.alCrearseUnaNotificacion("e-1", "n-1", "autor-1", "Pregunta aprobada", "Cuerpo del aviso");

        assertThat(sender.enviados()).singleElement().satisfies(c -> {
            assertThat(c.para()).isEqualTo("autor-1@correo.test");
            assertThat(c.asunto()).isEqualTo("Pregunta aprobada");
            assertThat(c.cuerpo()).isEqualTo("Cuerpo del aviso");
        });
        assertThat(enviados.todos()).singleElement().satisfies(c -> {
            assertThat(c.id()).isEqualTo("c-1");
            assertThat(c.notificacionId()).isEqualTo("n-1");
            assertThat(c.destinatarioId()).isEqualTo("autor-1");
            assertThat(c.estado()).isEqualTo(EstadoCorreo.ENVIADO);
            assertThat(c.error()).isNull();
            assertThat(c.intentadoEn()).isEqualTo(T0);
        });
    }

    @Test
    @DisplayName("si el servidor de correo falla, el envío queda registrado como fallido con el motivo y no se lanza")
    void fallido() {
        sender.fallar = true;

        servicio.alCrearseUnaNotificacion("e-1", "n-1", "autor-1", "Asunto", "Cuerpo");

        assertThat(enviados.todos()).singleElement().satisfies(c -> {
            assertThat(c.estado()).isEqualTo(EstadoCorreo.FALLIDO);
            assertThat(c.error()).contains("servidor de correo caído");
            assertThat(c.para()).isEqualTo("autor-1@correo.test");
        });
    }

    @Test
    @DisplayName("un evento repetido no envía dos veces el mismo correo")
    void idempotente() {
        servicio.alCrearseUnaNotificacion("e-1", "n-1", "autor-1", "Asunto", "Cuerpo");
        servicio.alCrearseUnaNotificacion("e-1", "n-1", "autor-1", "Asunto", "Cuerpo");

        assertThat(sender.enviados()).hasSize(1);
        assertThat(enviados.todos()).hasSize(1);
    }

    @Test
    @DisplayName("los correos se listan del más reciente al más antiguo y se pueden filtrar por estado")
    void listado() {
        servicio.alCrearseUnaNotificacion("e-1", "n-1", "a", "Primero", "c");
        reloj.avanzar(Duration.ofMinutes(1));
        sender.fallar = true;
        servicio.alCrearseUnaNotificacion("e-2", "n-2", "b", "Segundo", "c");
        reloj.avanzar(Duration.ofMinutes(1));
        sender.fallar = false;
        servicio.alCrearseUnaNotificacion("e-3", "n-3", "c", "Tercero", "c");

        assertThat(servicio.correos(null, 1, 10).elementos()).extracting(CorreoEnviado::asunto)
                .containsExactly("Tercero", "Segundo", "Primero");
        assertThat(servicio.correos(EstadoCorreo.FALLIDO, 1, 10).elementos()).extracting(CorreoEnviado::asunto)
                .containsExactly("Segundo");
        assertThat(servicio.correos(EstadoCorreo.ENVIADO, 1, 10).totalElementos()).isEqualTo(2);
    }

    @Test
    @DisplayName("si se pide una página que no existe, devuelve la última")
    void paginaFueraDeRango() {
        for (int i = 1; i <= 5; i++) {
            servicio.alCrearseUnaNotificacion("e-" + i, "n-" + i, "u" + i, "Asunto " + i, "c");
            reloj.avanzar(Duration.ofSeconds(1));
        }

        var pagina = servicio.correos(null, 9, 2);

        assertThat(pagina.numero()).isEqualTo(3);
        assertThat(pagina.elementos()).hasSize(1);
    }

    @Test
    @DisplayName("valida los parámetros de paginación")
    void validaPaginacion() {
        assertThatThrownBy(() -> servicio.correos(null, 0, 10)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> servicio.correos(null, 1, 0)).isInstanceOf(IllegalArgumentException.class);
    }
}
