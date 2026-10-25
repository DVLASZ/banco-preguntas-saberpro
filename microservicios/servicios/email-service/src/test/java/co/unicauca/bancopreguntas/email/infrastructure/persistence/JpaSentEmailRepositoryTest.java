package co.unicauca.bancopreguntas.email.infrastructure.persistence;

import co.unicauca.bancopreguntas.email.EmbeddedPostgresConfig;
import co.unicauca.bancopreguntas.email.domain.Correo;
import co.unicauca.bancopreguntas.email.domain.CorreoEnviado;
import co.unicauca.bancopreguntas.email.domain.EstadoCorreo;
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
@Import({JpaSentEmailRepository.class, JpaProcessedEvents.class, EmbeddedPostgresConfig.class,
        JpaSentEmailRepositoryTest.RelojConfig.class})
@DisplayName("Persistencia de correos (PostgreSQL)")
class JpaSentEmailRepositoryTest {

    private static final Instant T0 = Instant.parse("2026-10-08T10:00:00Z");

    @TestConfiguration
    static class RelojConfig {

        @Bean
        Clock clock() {
            return Clock.systemUTC();
        }
    }

    @Autowired
    private JpaSentEmailRepository repositorio;

    @Autowired
    private JpaProcessedEvents procesados;

    @Autowired
    private EntityManager em;

    private static String id() {
        return UUID.randomUUID().toString();
    }

    private CorreoEnviado guardar(boolean fallido, Instant cuando) {
        Correo c = new Correo("autor@correo.test", "Asunto con tildes: revisión", "Cuerpo de prueba");
        CorreoEnviado correo = fallido ? CorreoEnviado.fallido(id(), id(), "autor", c, "servidor caído", cuando)
                : CorreoEnviado.enviado(id(), id(), "autor", c, cuando);
        repositorio.guardar(correo);
        em.clear();
        return correo;
    }

    @Test
    @DisplayName("guarda un correo y lo recupera igual, con el motivo cuando falló")
    void guardaYRecupera() {
        CorreoEnviado ok = guardar(false, T0);
        CorreoEnviado mal = guardar(true, T0.plusSeconds(10));

        var todos = repositorio.listar(null, 1, 1000).elementos();

        assertThat(todos).contains(ok, mal);
        assertThat(todos.stream().filter(c -> c.id().equals(mal.id())).findFirst().orElseThrow().error()).isEqualTo("servidor caído");
    }

    @Test
    @DisplayName("lista del más reciente al más antiguo y filtra por estado")
    void ordenYFiltro() {
        CorreoEnviado vieja = guardar(false, T0.plusSeconds(100));
        CorreoEnviado fallida = guardar(true, T0.plusSeconds(150));
        CorreoEnviado nueva = guardar(false, T0.plusSeconds(200));

        var ids = repositorio.listar(null, 1, 1000).elementos().stream().map(CorreoEnviado::id).toList();
        assertThat(ids.indexOf(nueva.id())).isLessThan(ids.indexOf(fallida.id()));
        assertThat(ids.indexOf(fallida.id())).isLessThan(ids.indexOf(vieja.id()));

        var fallidos = repositorio.listar(EstadoCorreo.FALLIDO, 1, 1000).elementos();
        assertThat(fallidos).extracting(CorreoEnviado::estado).containsOnly(EstadoCorreo.FALLIDO);
        assertThat(fallidos).extracting(CorreoEnviado::id).contains(fallida.id()).doesNotContain(vieja.id(), nueva.id());
    }

    @Test
    @DisplayName("pagina el listado")
    void paginacion() {
        for (int i = 0; i < 5; i++) {
            guardar(false, T0.plusSeconds(i));
        }

        var pagina = repositorio.listar(null, 2, 2);

        assertThat(pagina.elementos()).hasSize(2);
        assertThat(pagina.numero()).isEqualTo(2);
        assertThat(pagina.totalElementos()).isGreaterThanOrEqualTo(5);
    }

    @Test
    @DisplayName("un evento solo se registra como procesado la primera vez")
    void idempotencia() {
        String evento = id();

        assertThat(procesados.registrarSiEsNuevo(evento)).isTrue();
        assertThat(procesados.registrarSiEsNuevo(evento)).isFalse();
    }
}
