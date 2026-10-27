package co.unicauca.bancopreguntas.lifecycle.infrastructure.persistence;

import co.unicauca.bancopreguntas.lifecycle.EmbeddedPostgresConfig;
import co.unicauca.bancopreguntas.lifecycle.domain.CambioEstado;
import co.unicauca.bancopreguntas.lifecycle.domain.CicloDeVida;
import co.unicauca.bancopreguntas.lifecycle.domain.EstadoPregunta;
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
@Import({JpaLifecycleRepository.class, JpaProcessedEvents.class, EmbeddedPostgresConfig.class,
        JpaLifecycleRepositoryTest.Config.class})
@DisplayName("Persistencia del ciclo de vida (PostgreSQL)")
class JpaLifecycleRepositoryTest {

    private static final Instant T0 = Instant.parse("2026-10-08T10:00:00Z");

    @TestConfiguration
    static class Config {

        @Bean
        Clock clock() {
            return Clock.systemUTC();
        }
    }

    @Autowired
    private JpaLifecycleRepository repositorio;

    @Autowired
    private JpaProcessedEvents procesados;

    @Autowired
    private EntityManager em;

    private static String id() {
        return UUID.randomUUID().toString();
    }

    @Test
    @DisplayName("guarda un ciclo de vida, lo lee igual y lo actualiza")
    void guardaYActualiza() {
        String id = id();
        CicloDeVida inicial = CicloDeVida.registrar(id, "autor-1", T0);
        repositorio.guardar(inicial);
        em.flush();
        em.clear();

        CicloDeVida leido = repositorio.buscar(id).orElseThrow();
        assertThat(leido.getEstado()).isEqualTo(EstadoPregunta.BORRADOR);
        assertThat(leido.getAutorId()).isEqualTo("autor-1");
        assertThat(leido.getVersion()).isEqualTo(1);
        assertThat(leido.getSecuencia()).isEqualTo(1);
        assertThat(leido.getActualizadoEn()).isEqualTo(T0);

        repositorio.guardar(leido.enviarARevision(T0.plusSeconds(60)));
        em.clear();

        CicloDeVida actualizado = repositorio.buscar(id).orElseThrow();
        assertThat(actualizado.getEstado()).isEqualTo(EstadoPregunta.PENDIENTE_REVISION);
        assertThat(actualizado.getSecuencia()).isEqualTo(2);
        assertThat(repositorio.buscar("no-existe")).isEmpty();
    }

    @Test
    @DisplayName("el historial sale en orden cronológico, con la creación sin estado anterior")
    void historial() {
        String id = id();
        repositorio.guardar(CicloDeVida.registrar(id, "autor-1", T0));
        repositorio.registrarCambioEstado(id, new CambioEstado(null, EstadoPregunta.BORRADOR, "autor-1", T0));
        repositorio.registrarCambioEstado(id, new CambioEstado(EstadoPregunta.BORRADOR, EstadoPregunta.PENDIENTE_REVISION,
                "autor-1", T0.plusSeconds(60)));
        repositorio.registrarCambioEstado(id, new CambioEstado(EstadoPregunta.PENDIENTE_REVISION, EstadoPregunta.EN_REVISION,
                null, T0.plusSeconds(120)));
        em.flush();
        em.clear();

        var historial = repositorio.historial(id);

        assertThat(historial).extracting(CambioEstado::hacia)
                .containsExactly(EstadoPregunta.BORRADOR, EstadoPregunta.PENDIENTE_REVISION, EstadoPregunta.EN_REVISION);
        assertThat(historial.get(0).desde()).isNull();
        assertThat(historial.get(2).cambiadoPor()).isNull();
        assertThat(repositorio.historial("no-existe")).isEmpty();
    }

    @Test
    @DisplayName("registra cada evento una sola vez")
    void idempotencia() {
        String evento = id();

        assertThat(procesados.registrarSiEsNuevo(evento)).isTrue();
        assertThat(procesados.registrarSiEsNuevo(evento)).isFalse();
    }
}
