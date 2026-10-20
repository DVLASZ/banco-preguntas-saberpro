package co.unicauca.bancopreguntas.history.infrastructure.persistence;

import co.unicauca.bancopreguntas.history.EmbeddedPostgresConfig;
import co.unicauca.bancopreguntas.history.domain.EntradaDeHistorial;
import co.unicauca.bancopreguntas.history.domain.FiltroAuditoria;
import co.unicauca.bancopreguntas.history.domain.InfoPregunta;
import co.unicauca.bancopreguntas.history.domain.Observador;
import co.unicauca.bancopreguntas.history.domain.TipoEntrada;
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
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Prueba de los adaptadores de persistencia contra un PostgreSQL real embebido (sin Docker): aplica las
 * migraciones de Flyway, valida el mapeo JPA y ejecuta las consultas de visibilidad y de auditoría.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({JpaHistoryRepository.class, JpaQuestionRegistry.class, JpaProcessedEvents.class,
        EmbeddedPostgresConfig.class, JpaHistoryRepositoryTest.RelojConfig.class})
@DisplayName("Persistencia del historial (PostgreSQL)")
class JpaHistoryRepositoryTest {

    private static final Instant T0 = Instant.parse("2026-10-08T10:00:00Z");

    @TestConfiguration
    static class RelojConfig {

        @Bean
        Clock clock() {
            return Clock.systemUTC();
        }
    }

    @Autowired
    private JpaHistoryRepository repositorio;

    @Autowired
    private JpaQuestionRegistry preguntas;

    @Autowired
    private JpaProcessedEvents procesados;

    @Autowired
    private EntityManager em;

    private static String id() {
        return UUID.randomUUID().toString();
    }

    private EntradaDeHistorial guardar(String preguntaId, String actor, TipoEntrada tipo, int segundos) {
        EntradaDeHistorial e = new EntradaDeHistorial(id(), id(), preguntaId, actor, tipo, "Resumen de " + tipo,
                "{\"clave\":\"valor\"}", T0.plusSeconds(segundos), T0.plusSeconds(segundos + 1));
        repositorio.guardar(e);
        em.clear();
        return e;
    }

    @Test
    @DisplayName("guarda y rehidrata una entrada con todos sus campos")
    void guardaYRecupera() {
        String q = id();
        EntradaDeHistorial original = guardar(q, "autor-1", TipoEntrada.PREGUNTA_ENVIADA, 0);

        List<EntradaDeHistorial> linea = repositorio.lineaDeTiempo(q, null);

        assertThat(linea).containsExactly(original);
    }

    @Test
    @DisplayName("una entrada de seguridad se guarda sin pregunta ni actor")
    void sinPregunta() {
        EntradaDeHistorial login = guardar(null, null, TipoEntrada.INICIO_DE_SESION, 0);

        assertThat(repositorio.auditoria(new FiltroAuditoria(TipoEntrada.INICIO_DE_SESION, null, null, null, null), 1, 1000)
                .elementos()).contains(login);
    }

    @Test
    @DisplayName("no admite dos entradas del mismo evento")
    void eventoUnico() {
        String q = id();
        EntradaDeHistorial a = guardar(q, "a", TipoEntrada.PREGUNTA_ENVIADA, 0);
        EntradaDeHistorial repetida = new EntradaDeHistorial(id(), a.eventId(), q, "a", TipoEntrada.PREGUNTA_ENVIADA, "x",
                null, T0, T0);

        assertThatThrownBy(() -> repositorio.guardar(repetida)).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("la línea de tiempo va en orden cronológico y puede limitarse a ciertos tipos")
    void lineaDeTiempo() {
        String q = id();
        guardar(q, "autor-1", TipoEntrada.PREGUNTA_ENVIADA, 0);
        guardar(q, "rev-1", TipoEntrada.EVALUACION_ENVIADA, 20);
        guardar(q, "admin-1", TipoEntrada.REVISORES_ASIGNADOS, 10);
        guardar(id(), "otro", TipoEntrada.PREGUNTA_ENVIADA, 5);

        assertThat(repositorio.lineaDeTiempo(q, null)).extracting(EntradaDeHistorial::tipo).containsExactly(
                TipoEntrada.PREGUNTA_ENVIADA, TipoEntrada.REVISORES_ASIGNADOS, TipoEntrada.EVALUACION_ENVIADA);
        assertThat(repositorio.lineaDeTiempo(q, Set.of(TipoEntrada.PREGUNTA_ENVIADA, TipoEntrada.EVALUACION_ENVIADA)))
                .extracting(EntradaDeHistorial::tipo)
                .containsExactly(TipoEntrada.PREGUNTA_ENVIADA, TipoEntrada.EVALUACION_ENVIADA);
        assertThat(repositorio.lineaDeTiempo("no-existe", null)).isEmpty();
    }

    @Test
    @DisplayName("el autor ve de sus preguntas solo los tipos que le corresponden; el revisor ve todo lo de las suyas")
    void visibilidad() {
        String propia = id();
        String ajena = id();
        String autor = "autor-" + id();
        String revisor = "rev-" + id();
        preguntas.guardar(new InfoPregunta(propia, "Mía", autor));
        preguntas.guardar(new InfoPregunta(ajena, "Ajena", "otro-autor"));
        preguntas.agregarRevisores(ajena, List.of(revisor));
        guardar(propia, autor, TipoEntrada.PREGUNTA_ENVIADA, 0);
        guardar(propia, "rev-x", TipoEntrada.EVALUACION_ENVIADA, 1);
        guardar(propia, null, TipoEntrada.REVISION_COMPLETADA, 2);
        guardar(ajena, "otro-autor", TipoEntrada.PREGUNTA_ENVIADA, 3);
        guardar(ajena, revisor, TipoEntrada.EVALUACION_ENVIADA, 4);
        guardar(null, autor, TipoEntrada.INICIO_DE_SESION, 5);

        var delAutor = repositorio.visiblesPara(new Observador(autor, false, true, false), 1, 100);
        var delRevisor = repositorio.visiblesPara(new Observador(revisor, false, false, true), 1, 100);
        var sinRol = repositorio.visiblesPara(new Observador(autor, false, false, false), 1, 100);

        assertThat(delAutor.elementos()).extracting(EntradaDeHistorial::tipo)
                .containsExactly(TipoEntrada.REVISION_COMPLETADA, TipoEntrada.PREGUNTA_ENVIADA); // lo más reciente primero
        assertThat(delAutor.elementos()).allMatch(e -> propia.equals(e.preguntaId()));
        assertThat(delRevisor.elementos()).extracting(EntradaDeHistorial::preguntaId).containsOnly(ajena).hasSize(2);
        assertThat(sinRol.elementos()).isEmpty();
    }

    @Test
    @DisplayName("el administrador ve todo, también los inicios de sesión; el revisor y el autor nunca los ven")
    void administrador() {
        String q = id();
        String login = "u-" + id();
        EntradaDeHistorial sesion = guardar(null, login, TipoEntrada.INICIO_DE_SESION, 0);
        guardar(q, "a", TipoEntrada.PREGUNTA_ENVIADA, 1);

        var admin = repositorio.visiblesPara(new Observador("admin-1", true, false, false), 1, 1000);
        var todoslosRoles = repositorio.visiblesPara(new Observador(login, false, true, true), 1, 1000);

        assertThat(admin.elementos()).contains(sesion);
        assertThat(todoslosRoles.elementos()).doesNotContain(sesion);
    }

    @Test
    @DisplayName("la paginación de lo visible respeta página, tamaño y total")
    void paginacion() {
        String q = id();
        String autor = "autor-" + id();
        preguntas.guardar(new InfoPregunta(q, "P", autor));
        for (int i = 0; i < 5; i++) {
            guardar(q, autor, TipoEntrada.CAMBIO_DE_ESTADO, i);
        }

        var primera = repositorio.visiblesPara(new Observador(autor, false, true, false), 1, 2);
        var tercera = repositorio.visiblesPara(new Observador(autor, false, true, false), 3, 2);

        assertThat(primera.elementos()).hasSize(2);
        assertThat(primera.totalElementos()).isEqualTo(5);
        assertThat(primera.totalPaginas()).isEqualTo(3);
        assertThat(tercera.elementos()).hasSize(1);
    }

    @Test
    @DisplayName("la auditoría combina los filtros de tipo, actor, pregunta y fechas")
    void auditoria() {
        String q = id();
        String actor = "actor-" + id();
        guardar(q, actor, TipoEntrada.PREGUNTA_ENVIADA, 0);
        EntradaDeHistorial medio = guardar(q, actor, TipoEntrada.CAMBIO_DE_ESTADO, 100);
        guardar(q, "otro", TipoEntrada.CAMBIO_DE_ESTADO, 200);

        assertThat(repositorio.auditoria(new FiltroAuditoria(null, actor, null, null, null), 1, 100).elementos()).hasSize(2);
        assertThat(repositorio.auditoria(new FiltroAuditoria(TipoEntrada.CAMBIO_DE_ESTADO, null, q, null, null), 1, 100)
                .elementos()).hasSize(2);
        assertThat(repositorio.auditoria(new FiltroAuditoria(TipoEntrada.CAMBIO_DE_ESTADO, actor, q, null, null), 1, 100)
                .elementos()).containsExactly(medio);
        assertThat(repositorio.auditoria(new FiltroAuditoria(null, null, q, T0.plusSeconds(50), T0.plusSeconds(150)), 1, 100)
                .elementos()).containsExactly(medio);
        assertThat(repositorio.auditoria(new FiltroAuditoria(null, null, q, null, null), 1, 100).elementos())
                .extracting(EntradaDeHistorial::ocurridaEn).isSortedAccordingTo(java.util.Comparator.reverseOrder());
    }

    @Test
    @DisplayName("recuerda el autor de una pregunta y lo actualiza si se reenvía")
    void infoDeLaPregunta() {
        String q = id();

        preguntas.guardar(new InfoPregunta(q, "Observer", "autor-1"));
        em.clear();
        assertThat(preguntas.buscar(q)).contains(new InfoPregunta(q, "Observer", "autor-1"));

        preguntas.guardar(new InfoPregunta(q, "Observer corregido", "autor-1"));
        em.clear();
        assertThat(preguntas.buscar(q).orElseThrow().titulo()).isEqualTo("Observer corregido");
        assertThat(preguntas.buscar("no-existe")).isEmpty();
    }

    @Test
    @DisplayName("los revisores se registran una sola vez por pregunta")
    void revisores() {
        String q = id();

        preguntas.agregarRevisores(q, List.of("r1", "r2"));
        preguntas.agregarRevisores(q, List.of("r2", "r3"));

        assertThat(preguntas.esRevisor(q, "r1")).isTrue();
        assertThat(preguntas.esRevisor(q, "r3")).isTrue();
        assertThat(preguntas.esRevisor(q, "r9")).isFalse();
        assertThat(preguntas.esRevisor(id(), "r1")).isFalse();
    }

    @Test
    @DisplayName("registra un evento procesado una sola vez")
    void eventosProcesados() {
        String evento = "evt-" + id();

        assertThat(procesados.registrarSiEsNuevo(evento)).isTrue();
        assertThat(procesados.registrarSiEsNuevo(evento)).isFalse();
    }
}
