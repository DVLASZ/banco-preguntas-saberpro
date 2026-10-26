package co.unicauca.bancopreguntas.audit.application;

import co.unicauca.bancopreguntas.audit.domain.EntradaDeAuditoria;
import co.unicauca.bancopreguntas.audit.domain.EventoRecibido;
import co.unicauca.bancopreguntas.audit.domain.FiltroAuditoria;
import co.unicauca.bancopreguntas.audit.domain.TipoEntrada;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("AuditApplicationService (casos de uso)")
class AuditApplicationServiceTest {

    private static final Instant T0 = Instant.parse("2026-10-08T10:00:00Z");

    private InMemoryAuditRepository auditoria;
    private RelojManual reloj;
    private AuditApplicationService servicio;

    @BeforeEach
    void preparar() {
        auditoria = new InMemoryAuditRepository();
        reloj = new RelojManual(T0);
        AtomicInteger ids = new AtomicInteger();
        servicio = new AuditApplicationService(auditoria, new InMemoryProcessedEvents(), reloj, () -> "a-" + ids.incrementAndGet());
    }

    private EventoRecibido evento(String id, int segundos) {
        return new EventoRecibido(id, T0.plusSeconds(segundos), "{\"k\":\"v\"}");
    }

    @Test
    @DisplayName("cada tipo de evento deja una entrada con su tipo, su actor y su resumen")
    void registraCadaTipo() {
        servicio.alEnviarseUnaPregunta(evento("e1", 1), "q-1", "autor-1", "Observer", 2);
        servicio.alReabrirseUnaPregunta(evento("e2", 2), "q-1", "autor-1", 3);
        servicio.alPublicarseUnaPregunta(evento("e3", 3), "q-1", "admin-1");
        servicio.alCambiarDeEstado(evento("e4", 4), "q-1", "BORRADOR", "PENDIENTE_REVISION", "autor-1");
        servicio.alAsignarseRevisores(evento("e5", 5), "q-1", 1, List.of("rev-1", "rev-2"), "admin-1", false);
        servicio.alEnviarseUnaEvaluacion(evento("e6", 6), "q-1", 1, "rev-1", "APROBAR", false);
        servicio.alEnviarseUnaEvaluacion(evento("e7", 7), "q-1", 1, "rev-2", "RECHAZAR", true);
        servicio.alCompletarseUnaRevision(evento("e8", 8), "q-1", 1, true);
        servicio.alIniciarSesion(evento("e9", 9), "autor-1", true);
        servicio.alIniciarSesion(evento("e10", 10), "intruso", false);

        assertThat(auditoria.todas()).extracting(EntradaDeAuditoria::tipo).containsExactly(
                TipoEntrada.PREGUNTA_ENVIADA, TipoEntrada.PREGUNTA_REABIERTA, TipoEntrada.PREGUNTA_PUBLICADA,
                TipoEntrada.CAMBIO_DE_ESTADO, TipoEntrada.REVISORES_ASIGNADOS, TipoEntrada.EVALUACION_ENVIADA,
                TipoEntrada.EVALUACION_TARDIA, TipoEntrada.REVISION_COMPLETADA, TipoEntrada.INICIO_DE_SESION,
                TipoEntrada.INICIO_DE_SESION_FALLIDO);
        assertThat(auditoria.todas().get(0)).satisfies(e -> {
            assertThat(e.id()).isEqualTo("a-1");
            assertThat(e.eventId()).isEqualTo("e1");
            assertThat(e.preguntaId()).isEqualTo("q-1");
            assertThat(e.actorId()).isEqualTo("autor-1");
            assertThat(e.resumen()).contains("autor-1").contains("Observer").contains("versión 2");
            assertThat(e.detalles()).contains("\"k\"");
            assertThat(e.ocurridaEn()).isEqualTo(T0.plusSeconds(1));
            assertThat(e.registradaEn()).isEqualTo(T0);
        });
        assertThat(auditoria.todas().get(8).preguntaId()).isNull();
        assertThat(auditoria.todas().get(9).resumen()).contains("Intento fallido").contains("intruso");
    }

    @Test
    @DisplayName("un evento repetido no deja una segunda entrada")
    void idempotente() {
        servicio.alEnviarseUnaPregunta(evento("e1", 1), "q-1", "autor-1", "T", 1);
        servicio.alEnviarseUnaPregunta(evento("e1", 1), "q-1", "autor-1", "T", 1);
        servicio.alIniciarSesion(evento("e2", 2), "autor-1", true);
        servicio.alIniciarSesion(evento("e2", 2), "autor-1", true);

        assertThat(auditoria.todas()).hasSize(2);
    }

    @Test
    @DisplayName("la consulta devuelve lo más reciente primero y aplica los filtros")
    void consulta() {
        servicio.alEnviarseUnaPregunta(evento("e1", 1), "q-1", "autor-1", "T", 1);
        servicio.alCambiarDeEstado(evento("e2", 2), "q-1", "BORRADOR", "PENDIENTE_REVISION", "autor-1");
        servicio.alIniciarSesion(evento("e3", 3), "autor-1", true);
        servicio.alIniciarSesion(evento("e4", 4), "intruso", false);
        reloj.avanzar(Duration.ofMinutes(1));

        assertThat(servicio.consultar(FiltroAuditoria.sinFiltros(), 1, 10).elementos()).extracting(EntradaDeAuditoria::eventId)
                .containsExactly("e4", "e3", "e2", "e1");
        assertThat(servicio.consultar(new FiltroAuditoria(TipoEntrada.INICIO_DE_SESION_FALLIDO, null, null, null, null), 1, 10)
                .elementos()).extracting(EntradaDeAuditoria::actorId).containsExactly("intruso");
        assertThat(servicio.consultar(new FiltroAuditoria(null, "autor-1", null, null, null), 1, 10).totalElementos()).isEqualTo(3);
        assertThat(servicio.consultar(new FiltroAuditoria(null, null, "q-1", null, null), 1, 10).totalElementos()).isEqualTo(2);
        assertThat(servicio.consultar(new FiltroAuditoria(null, null, null, T0.plusSeconds(2), T0.plusSeconds(3)), 1, 10).elementos())
                .extracting(EntradaDeAuditoria::eventId).containsExactly("e3", "e2");
    }

    @Test
    @DisplayName("si se pide una página que no existe, devuelve la última; y valida la paginación")
    void paginacion() {
        for (int i = 1; i <= 5; i++) {
            servicio.alIniciarSesion(evento("e" + i, i), "u" + i, true);
        }

        var pagina = servicio.consultar(FiltroAuditoria.sinFiltros(), 9, 2);

        assertThat(pagina.numero()).isEqualTo(3);
        assertThat(pagina.elementos()).hasSize(1);
        assertThatThrownBy(() -> servicio.consultar(FiltroAuditoria.sinFiltros(), 0, 10)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> servicio.consultar(FiltroAuditoria.sinFiltros(), 1, 0)).isInstanceOf(IllegalArgumentException.class);
    }
}
