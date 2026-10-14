package co.unicauca.bancopreguntas.notification.infrastructure.persistence;

import co.unicauca.bancopreguntas.notification.EmbeddedPostgresConfig;
import co.unicauca.bancopreguntas.notification.domain.InfoPregunta;
import co.unicauca.bancopreguntas.notification.domain.Mensaje;
import co.unicauca.bancopreguntas.notification.domain.Notificacion;
import co.unicauca.bancopreguntas.notification.domain.TipoNotificacion;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Prueba de los adaptadores de persistencia contra un PostgreSQL real embebido (sin Docker): aplica las
 * migraciones de Flyway y valida el mapeo JPA contra el esquema.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({JpaNotificationRepository.class, JpaQuestionInfoRepository.class, JpaProcessedEvents.class,
        EmbeddedPostgresConfig.class, JpaNotificationRepositoryTest.RelojConfig.class})
@DisplayName("Persistencia de notificaciones (PostgreSQL)")
class JpaNotificationRepositoryTest {

    private static final Instant T0 = Instant.parse("2026-10-08T10:00:00Z");

    @TestConfiguration
    static class RelojConfig {

        @Bean
        Clock clock() {
            return Clock.systemUTC();
        }
    }

    @Autowired
    private JpaNotificationRepository repositorio;

    @Autowired
    private JpaQuestionInfoRepository informacion;

    @Autowired
    private JpaProcessedEvents procesados;

    @Autowired
    private EntityManager em;

    private static String id() {
        return UUID.randomUUID().toString();
    }

    private Notificacion guardar(String usuario, Instant creada, boolean conCorreo) {
        Notificacion n = Notificacion.nueva(id(), usuario, usuario + "@correo.test", TipoNotificacion.REVISION_APROBADA,
                id(), new Mensaje("Asunto de prueba", "Cuerpo de prueba con tildes: revisión"), creada);
        if (conCorreo) {
            n = n.conCorreoEnviado(creada.plusSeconds(1));
        }
        repositorio.guardar(n);
        em.clear();
        return n;
    }

    @Test
    @DisplayName("guarda y recupera una notificación con todos sus campos")
    void guardaYRecupera() {
        Notificacion original = guardar("autor-" + id(), T0, true);

        Notificacion leida = repositorio.buscarPorId(original.id()).orElseThrow();

        assertThat(leida).isEqualTo(original);
        assertThat(leida.mensaje()).contains("revisión");
    }

    @Test
    @DisplayName("una notificación inexistente no se encuentra")
    void noExiste() {
        assertThat(repositorio.buscarPorId("no-existe")).isEmpty();
    }

    @Test
    @DisplayName("guardar de nuevo actualiza la misma fila (marcar como leída)")
    void actualiza() {
        Notificacion original = guardar("autor-" + id(), T0, false);

        repositorio.guardar(original.marcadaComoLeida(T0.plusSeconds(30)));
        em.clear();

        Notificacion leida = repositorio.buscarPorId(original.id()).orElseThrow();
        assertThat(leida.leidaEn()).isEqualTo(T0.plusSeconds(30));
    }

    @Test
    @DisplayName("la bandeja es del usuario, la más reciente primero, con total y paginación")
    void bandeja() {
        String usuario = "autor-" + id();
        Notificacion vieja = guardar(usuario, T0, false);
        Notificacion media = guardar(usuario, T0.plusSeconds(10), false);
        Notificacion nueva = guardar(usuario, T0.plusSeconds(20), false);
        guardar("otro-" + id(), T0.plusSeconds(30), false);

        var primera = repositorio.delUsuario(usuario, false, 1, 2);
        var segunda = repositorio.delUsuario(usuario, false, 2, 2);

        assertThat(primera.elementos()).extracting(Notificacion::id).containsExactly(nueva.id(), media.id());
        assertThat(segunda.elementos()).extracting(Notificacion::id).containsExactly(vieja.id());
        assertThat(primera.totalElementos()).isEqualTo(3);
        assertThat(primera.totalPaginas()).isEqualTo(2);
    }

    @Test
    @DisplayName("se filtran y cuentan las no leídas")
    void noLeidas() {
        String usuario = "autor-" + id();
        Notificacion a = guardar(usuario, T0, false);
        guardar(usuario, T0.plusSeconds(1), false);
        repositorio.guardar(a.marcadaComoLeida(T0.plusSeconds(5)));
        em.clear();

        assertThat(repositorio.contarNoLeidas(usuario)).isEqualTo(1);
        assertThat(repositorio.delUsuario(usuario, true, 1, 10).elementos()).hasSize(1)
                .noneMatch(n -> n.id().equals(a.id()));
        assertThat(repositorio.delUsuario(usuario, false, 1, 10).elementos()).hasSize(2);
        assertThat(repositorio.contarNoLeidas("nadie-" + id())).isZero();
    }

    @Test
    @DisplayName("marcar todas como leídas afecta solo al usuario y devuelve cuántas cambiaron")
    void marcarTodas() {
        String usuario = "autor-" + id();
        String otro = "otro-" + id();
        guardar(usuario, T0, false);
        guardar(usuario, T0.plusSeconds(1), false);
        guardar(otro, T0, false);

        int cambiadas = repositorio.marcarTodasComoLeidas(usuario, T0.plusSeconds(60));

        assertThat(cambiadas).isEqualTo(2);
        assertThat(repositorio.contarNoLeidas(usuario)).isZero();
        assertThat(repositorio.contarNoLeidas(otro)).isEqualTo(1);
        assertThat(repositorio.delUsuario(usuario, false, 1, 10).elementos())
                .allSatisfy(n -> assertThat(n.leidaEn()).isEqualTo(T0.plusSeconds(60)));
        assertThat(repositorio.marcarTodasComoLeidas(usuario, T0.plusSeconds(90))).isZero();
    }

    @Test
    @DisplayName("lista solo las notificaciones con correo enviado, la más reciente primero")
    void conCorreo() {
        String usuario = "autor-" + id();
        Notificacion sinCorreo = guardar(usuario, T0, false);
        Notificacion vieja = guardar(usuario, T0.plusSeconds(100), true);
        Notificacion nueva = guardar(usuario, T0.plusSeconds(200), true);

        var correos = repositorio.conCorreoEnviado(1, 1000);

        var ids = correos.elementos().stream().map(Notificacion::id).toList();
        assertThat(ids).contains(vieja.id(), nueva.id()).doesNotContain(sinCorreo.id());
        assertThat(ids.indexOf(nueva.id())).isLessThan(ids.indexOf(vieja.id()));
    }

    @Test
    @DisplayName("recuerda el título y el autor de una pregunta y los actualiza al reenviarse")
    void informacionDeLaPregunta() {
        String pregunta = id();

        informacion.guardar(new InfoPregunta(pregunta, "Observer", "autor-1"));
        em.clear();
        assertThat(informacion.buscar(pregunta)).contains(new InfoPregunta(pregunta, "Observer", "autor-1"));

        informacion.guardar(new InfoPregunta(pregunta, "Observer corregido", "autor-1"));
        em.clear();
        assertThat(informacion.buscar(pregunta).orElseThrow().titulo()).isEqualTo("Observer corregido");
        assertThat(informacion.buscar("no-existe")).isEmpty();
    }

    @Test
    @DisplayName("la pregunta puede no tener título")
    void sinTitulo() {
        String pregunta = id();

        informacion.guardar(new InfoPregunta(pregunta, null, "autor-1"));
        em.clear();

        assertThat(informacion.buscar(pregunta).orElseThrow().titulo()).isNull();
    }

    @Test
    @DisplayName("registra un evento procesado una sola vez")
    void eventosProcesados() {
        String evento = "evt-" + id();

        assertThat(procesados.registrarSiEsNuevo(evento)).isTrue();
        assertThat(procesados.registrarSiEsNuevo(evento)).isFalse();
    }
}
