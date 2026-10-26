package co.unicauca.bancopreguntas.audit.infrastructure.persistence;

import co.unicauca.bancopreguntas.audit.EmbeddedPostgresConfig;
import co.unicauca.bancopreguntas.audit.domain.EntradaDeAuditoria;
import co.unicauca.bancopreguntas.audit.domain.FiltroAuditoria;
import co.unicauca.bancopreguntas.audit.domain.TipoEntrada;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Prueba de los adaptadores de persistencia contra un PostgreSQL real embebido (sin Docker): aplica las
 * migraciones de Flyway, valida el mapeo JPA y ejecuta la consulta de auditoría con sus filtros.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({JpaAuditRepository.class, JpaProcessedEvents.class, EmbeddedPostgresConfig.class, JpaAuditRepositoryTest.RelojConfig.class})
@DisplayName("Persistencia de la auditoría (PostgreSQL)")
class JpaAuditRepositoryTest {

    private static final Instant T0 = Instant.parse("2026-10-08T10:00:00Z");

    @TestConfiguration
    static class RelojConfig {

        @Bean
        Clock clock() {
            return Clock.systemUTC();
        }
    }

    @Autowired
    private JpaAuditRepository repositorio;

    @Autowired
    private JpaProcessedEvents procesados;

    @Autowired
    private EntityManager em;

    private static String id() {
        return UUID.randomUUID().toString();
    }

    private EntradaDeAuditoria guardar(String preguntaId, String actor, TipoEntrada tipo, int segundos) {
        EntradaDeAuditoria e = new EntradaDeAuditoria(id(), id(), preguntaId, actor, tipo, "Resumen con tildes: revisión",
                "{\"detalle\":1}", T0.plusSeconds(segundos), T0.plusSeconds(segundos + 1));
        repositorio.guardar(e);
        em.clear();
        return e;
    }

    @Test
    @DisplayName("guarda y rehidrata una entrada con todos sus campos")
    void guardaYRecupera() {
        EntradaDeAuditoria e = guardar(id(), "autor-1", TipoEntrada.CAMBIO_DE_ESTADO, 0);

        assertThat(repositorio.consultar(new FiltroAuditoria(null, "autor-1", e.preguntaId(), null, null), 1, 10).elementos())
                .containsExactly(e);
    }

    @Test
    @DisplayName("una entrada de seguridad se guarda sin pregunta")
    void sinPregunta() {
        EntradaDeAuditoria login = guardar(null, "u-" + id(), TipoEntrada.INICIO_DE_SESION, 0);

        assertThat(repositorio.consultar(new FiltroAuditoria(TipoEntrada.INICIO_DE_SESION, login.actorId(), null, null, null), 1, 10)
                .elementos()).containsExactly(login);
    }

    @Test
    @DisplayName("no admite dos entradas del mismo evento")
    void eventoUnico() {
        EntradaDeAuditoria primera = guardar("q-1", "a", TipoEntrada.CAMBIO_DE_ESTADO, 0);
        EntradaDeAuditoria repetida = new EntradaDeAuditoria(id(), primera.eventId(), "q-1", "a", TipoEntrada.CAMBIO_DE_ESTADO,
                "otro", null, T0, T0);

        assertThatThrownBy(() -> repositorio.guardar(repetida)).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("combina los filtros de tipo, actor, pregunta y fechas, lo más reciente primero")
    void filtros() {
        String actor = "u-" + id();
        String q = id();
        EntradaDeAuditoria a = guardar(q, actor, TipoEntrada.CAMBIO_DE_ESTADO, 0);
        EntradaDeAuditoria b = guardar(q, actor, TipoEntrada.PREGUNTA_ENVIADA, 100);
        EntradaDeAuditoria c = guardar(q, "otro-" + id(), TipoEntrada.CAMBIO_DE_ESTADO, 200);

        assertThat(repositorio.consultar(new FiltroAuditoria(null, actor, null, null, null), 1, 100).elementos())
                .containsExactly(b, a);
        assertThat(repositorio.consultar(new FiltroAuditoria(TipoEntrada.CAMBIO_DE_ESTADO, null, q, null, null), 1, 100).elementos())
                .containsExactly(c, a);
        assertThat(repositorio.consultar(new FiltroAuditoria(TipoEntrada.CAMBIO_DE_ESTADO, actor, q, null, null), 1, 100).elementos())
                .containsExactly(a);
        assertThat(repositorio.consultar(new FiltroAuditoria(null, null, q, T0.plusSeconds(50), T0.plusSeconds(150)), 1, 100)
                .elementos()).containsExactly(b);
        assertThat(repositorio.consultar(new FiltroAuditoria(null, null, q, null, null), 1, 100).elementos()).containsExactly(c, b, a);
    }

    @Test
    @DisplayName("la paginación respeta página, tamaño y total")
    void paginacion() {
        String q = id();
        for (int i = 0; i < 5; i++) {
            guardar(q, "a", TipoEntrada.CAMBIO_DE_ESTADO, i);
        }

        var pagina = repositorio.consultar(new FiltroAuditoria(null, null, q, null, null), 2, 2);

        assertThat(pagina.elementos()).hasSize(2);
        assertThat(pagina.totalElementos()).isEqualTo(5);
        assertThat(pagina.totalPaginas()).isEqualTo(3);
    }

    @Test
    @DisplayName("registra un evento procesado una sola vez")
    void eventosProcesados() {
        String evento = id();

        assertThat(procesados.registrarSiEsNuevo(evento)).isTrue();
        assertThat(procesados.registrarSiEsNuevo(evento)).isFalse();
    }
}
