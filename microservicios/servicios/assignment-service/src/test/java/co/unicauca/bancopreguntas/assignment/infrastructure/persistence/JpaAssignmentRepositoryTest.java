package co.unicauca.bancopreguntas.assignment.infrastructure.persistence;

import co.unicauca.bancopreguntas.assignment.EmbeddedPostgresConfig;
import co.unicauca.bancopreguntas.assignment.domain.CasoDeAsignacion;
import co.unicauca.bancopreguntas.assignment.domain.Decision;
import co.unicauca.bancopreguntas.assignment.domain.EstadoRonda;
import co.unicauca.bancopreguntas.assignment.domain.FiltroAsignacion;
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
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Prueba de los adaptadores de persistencia contra un PostgreSQL real embebido (sin Docker): aplica las migraciones
 * de Flyway y valida el mapeo JPA y las consultas contra el esquema.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({JpaAssignmentRepository.class, JpaProcessedEvents.class, EmbeddedPostgresConfig.class,
        JpaAssignmentRepositoryTest.RelojConfig.class})
@DisplayName("Persistencia de asignaciones (PostgreSQL)")
class JpaAssignmentRepositoryTest {

    private static final Instant T0 = Instant.parse("2026-10-08T10:00:00Z");
    private static final Instant T1 = T0.plusSeconds(60);
    private static final Instant T2 = T0.plusSeconds(120);

    @TestConfiguration
    static class RelojConfig {

        @Bean
        Clock clock() {
            return Clock.systemUTC();
        }
    }

    @Autowired
    private JpaAssignmentRepository repositorio;

    @Autowired
    private JpaProcessedEvents procesados;

    @Autowired
    private CaseJpaRepository jpa;

    @Autowired
    private EntityManager em;

    private static String id() {
        return UUID.randomUUID().toString();
    }

    private CasoDeAsignacion nuevoCaso(String preguntaId, Instant ahora) {
        CasoDeAsignacion caso = CasoDeAsignacion.nuevo(id(), preguntaId, "autor-1", "Patrón Observer", "LECTURA_CRITICA", ahora);
        caso.iniciarRonda(1, ahora);
        return caso;
    }

    private void guardarYLimpiar(CasoDeAsignacion caso) {
        repositorio.guardar(caso);
        jpa.flush();
        em.clear(); // las lecturas siguientes van a la base, no a la caché de la sesión
    }

    @Test
    @DisplayName("guarda y recupera un caso con su ronda pendiente")
    void guardaYRecupera() {
        String q = id();
        guardarYLimpiar(nuevoCaso(q, T0));

        CasoDeAsignacion leido = repositorio.buscarPorPreguntaId(q).orElseThrow();

        assertThat(leido.preguntaId()).isEqualTo(q);
        assertThat(leido.autorId()).isEqualTo("autor-1");
        assertThat(leido.titulo()).isEqualTo("Patrón Observer");
        assertThat(leido.competencia()).isEqualTo("LECTURA_CRITICA");
        assertThat(leido.creadoEn()).isEqualTo(T0);
        assertThat(leido.rondas()).hasSize(1);
        assertThat(leido.rondaActual().estado()).isEqualTo(EstadoRonda.PENDIENTE_ASIGNACION);
        assertThat(leido.rondaActual().versionDePregunta()).isEqualTo(1);
        assertThat(leido.rondaActual().iniciadaEn()).isEqualTo(T0);
    }

    @Test
    @DisplayName("una pregunta sin caso no se encuentra")
    void noExiste() {
        assertThat(repositorio.buscarPorPreguntaId("no-existe")).isEmpty();
        assertThat(repositorio.buscarPorPreguntaIdParaActualizar("no-existe")).isEmpty();
    }

    @Test
    @DisplayName("guarda asignaciones y decisiones, y las rehidrata en orden")
    void asignacionesYDecisiones() {
        String q = id();
        CasoDeAsignacion caso = nuevoCaso(q, T0);
        caso.asignarRevisores(List.of("rev-a", "rev-b"), "admin-1", 5, T1);
        caso.registrarEvaluacion(1, "rev-a", Decision.RECHAZAR, T2);
        caso.registrarDecisionDeRonda(1, false, T2);
        guardarYLimpiar(caso);

        var ronda = repositorio.buscarPorPreguntaId(q).orElseThrow().rondaActual();

        assertThat(ronda.estado()).isEqualTo(EstadoRonda.RECHAZADA);
        assertThat(ronda.decididaEn()).contains(T2);
        assertThat(ronda.revisores()).containsExactly("rev-a", "rev-b");
        assertThat(ronda.asignaciones().get(0).asignadoPor()).isEqualTo("admin-1");
        assertThat(ronda.asignaciones().get(0).asignadoEn()).isEqualTo(T1);
        assertThat(ronda.asignaciones().get(0).automatica()).isFalse();
        assertThat(ronda.decisionDe("rev-a")).contains(Decision.RECHAZAR);
        assertThat(ronda.decisionDe("rev-b")).isEmpty();
    }

    @Test
    @DisplayName("guardar de nuevo agrega lo nuevo sin duplicar lo anterior")
    void guardarDeNuevo() {
        String q = id();
        CasoDeAsignacion caso = nuevoCaso(q, T0);
        caso.asignarRevisores(List.of("rev-a"), "admin-1", 5, T1);
        guardarYLimpiar(caso);

        CasoDeAsignacion leido = repositorio.buscarPorPreguntaId(q).orElseThrow();
        leido.asignarRevisores(List.of("rev-b"), "admin-1", 5, T2);
        leido.registrarEvaluacion(1, "rev-a", Decision.APROBAR, T2);
        guardarYLimpiar(leido);

        var ronda = repositorio.buscarPorPreguntaId(q).orElseThrow().rondaActual();
        assertThat(ronda.revisores()).containsExactly("rev-a", "rev-b");
        assertThat(ronda.decisionDe("rev-a")).contains(Decision.APROBAR);
    }

    @Test
    @DisplayName("conserva todas las rondas con su historial y la reasignación automática")
    void variasRondas() {
        String q = id();
        CasoDeAsignacion caso = nuevoCaso(q, T0);
        caso.asignarRevisores(List.of("rev-a", "rev-b"), "admin-1", 5, T0);
        caso.registrarDecisionDeRonda(1, false, T1);
        caso.iniciarRonda(2, T2);
        guardarYLimpiar(caso);

        CasoDeAsignacion leido = repositorio.buscarPorPreguntaId(q).orElseThrow();

        assertThat(leido.rondas()).hasSize(2);
        assertThat(leido.rondas().get(0).estado()).isEqualTo(EstadoRonda.CERRADA);
        assertThat(leido.rondas().get(1).estado()).isEqualTo(EstadoRonda.EN_CURSO);
        assertThat(leido.rondas().get(1).versionDePregunta()).isEqualTo(2);
        assertThat(leido.rondas().get(1).asignaciones()).allMatch(a -> a.automatica() && a.asignadoPor().equals("system"));
    }

    @Test
    @DisplayName("no admite dos casos para la misma pregunta")
    void unCasoPorPregunta() {
        String q = id();
        guardarYLimpiar(nuevoCaso(q, T0));
        CaseEntity duplicado = new CaseEntity();
        CaseMapper.sincronizar(nuevoCaso(q, T0), duplicado);

        assertThatThrownBy(() -> jpa.saveAndFlush(duplicado)).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("la lista de pendientes de asignación trae solo las rondas que esperan revisores, la más antigua primero")
    void pendientes() {
        String vieja = id();
        String nueva = id();
        String asignada = id();
        guardarYLimpiar(nuevoCaso(nueva, T1));
        guardarYLimpiar(nuevoCaso(vieja, T0));
        CasoDeAsignacion conRevisores = nuevoCaso(asignada, T0);
        conRevisores.asignarRevisores(List.of("r1"), "admin-1", 5, T1);
        guardarYLimpiar(conRevisores);

        var pagina = repositorio.pendientesDeAsignacion(1, 100);

        List<String> ids = pagina.elementos().stream().map(p -> p.preguntaId()).toList();
        assertThat(ids).contains(vieja, nueva).doesNotContain(asignada);
        assertThat(ids.indexOf(vieja)).isLessThan(ids.indexOf(nueva));
        var fila = pagina.elementos().stream().filter(p -> p.preguntaId().equals(vieja)).findFirst().orElseThrow();
        assertThat(fila.titulo()).isEqualTo("Patrón Observer");
        assertThat(fila.autorId()).isEqualTo("autor-1");
        assertThat(fila.ronda()).isEqualTo(1);
        assertThat(fila.enviadaEn()).isEqualTo(T0);
    }

    @Test
    @DisplayName("la paginación de pendientes respeta página y tamaño")
    void paginacionDePendientes() {
        for (int i = 0; i < 3; i++) {
            guardarYLimpiar(nuevoCaso(id(), T0.plusSeconds(i)));
        }

        var primera = repositorio.pendientesDeAsignacion(1, 2);
        var segunda = repositorio.pendientesDeAsignacion(2, 2);

        assertThat(primera.elementos()).hasSize(2);
        assertThat(primera.totalElementos()).isGreaterThanOrEqualTo(3);
        assertThat(segunda.numero()).isEqualTo(2);
        assertThat(segunda.elementos()).isNotEmpty();
    }

    @Test
    @DisplayName("la bandeja del revisor distingue pendientes y evaluadas con su decisión")
    void bandejaDelRevisor() {
        String revisor = "rev-" + id();
        String q1 = id();
        String q2 = id();
        CasoDeAsignacion c1 = nuevoCaso(q1, T0);
        c1.asignarRevisores(List.of(revisor, "otro"), "admin-1", 5, T0);
        c1.registrarEvaluacion(1, revisor, Decision.APROBAR, T1);
        guardarYLimpiar(c1);
        CasoDeAsignacion c2 = nuevoCaso(q2, T1);
        c2.asignarRevisores(List.of(revisor), "admin-1", 5, T1);
        guardarYLimpiar(c2);

        var pendientes = repositorio.asignadasA(revisor, FiltroAsignacion.PENDIENTES, 1, 10);
        var evaluadas = repositorio.asignadasA(revisor, FiltroAsignacion.EVALUADAS, 1, 10);
        var todas = repositorio.asignadasA(revisor, FiltroAsignacion.TODAS, 1, 10);

        assertThat(pendientes.elementos()).extracting(a -> a.preguntaId()).containsExactly(q2);
        assertThat(pendientes.elementos().get(0).miDecision()).isNull();
        assertThat(pendientes.elementos().get(0).pendiente()).isTrue();
        assertThat(evaluadas.elementos()).extracting(a -> a.preguntaId()).containsExactly(q1);
        assertThat(evaluadas.elementos().get(0).miDecision()).isEqualTo(Decision.APROBAR);
        assertThat(todas.elementos()).extracting(a -> a.preguntaId()).containsExactly(q2, q1); // la más reciente primero
        assertThat(todas.totalElementos()).isEqualTo(2);
    }

    @Test
    @DisplayName("tras un rechazo, el revisor que no evaluó la conserva como pendiente (observación tardía)")
    void pendienteTrasRechazo() {
        String revisor = "rev-" + id();
        CasoDeAsignacion caso = nuevoCaso(id(), T0);
        caso.asignarRevisores(List.of("otro", revisor), "admin-1", 5, T0);
        caso.registrarEvaluacion(1, "otro", Decision.RECHAZAR, T1);
        caso.registrarDecisionDeRonda(1, false, T1);
        guardarYLimpiar(caso);

        var pendientes = repositorio.asignadasA(revisor, FiltroAsignacion.PENDIENTES, 1, 10);

        assertThat(pendientes.elementos()).singleElement().satisfies(a -> {
            assertThat(a.estadoRonda()).isEqualTo(EstadoRonda.RECHAZADA);
            assertThat(a.pendiente()).isTrue();
        });
    }

    @Test
    @DisplayName("al cerrarse la ronda por reapertura, ya no queda pendiente para quien no evaluó")
    void cerradaNoEsPendiente() {
        String revisor = "rev-" + id();
        CasoDeAsignacion caso = nuevoCaso(id(), T0);
        caso.asignarRevisores(List.of("otro", revisor), "admin-1", 5, T0);
        caso.registrarDecisionDeRonda(1, false, T1);
        caso.cerrarRondaPorReapertura(T2);
        guardarYLimpiar(caso);

        assertThat(repositorio.asignadasA(revisor, FiltroAsignacion.PENDIENTES, 1, 10).elementos()).isEmpty();
        assertThat(repositorio.asignadasA(revisor, FiltroAsignacion.TODAS, 1, 10).elementos()).hasSize(1);
    }

    @Test
    @DisplayName("un revisor sin asignaciones tiene la bandeja vacía")
    void bandejaVacia() {
        var pagina = repositorio.asignadasA("nadie-" + id(), FiltroAsignacion.TODAS, 1, 10);

        assertThat(pagina.elementos()).isEmpty();
        assertThat(pagina.totalElementos()).isZero();
    }

    @Test
    @DisplayName("obtener para actualizar devuelve el caso (con bloqueo de fila)")
    void paraActualizar() {
        String q = id();
        guardarYLimpiar(nuevoCaso(q, T0));

        assertThat(repositorio.buscarPorPreguntaIdParaActualizar(q)).isPresent();
    }

    @Test
    @DisplayName("registra un evento procesado una sola vez")
    void eventosProcesados() {
        String evento = "evt-" + id();

        assertThat(procesados.registrarSiEsNuevo(evento)).isTrue();
        assertThat(procesados.registrarSiEsNuevo(evento)).isFalse();
        assertThat(procesados.registrarSiEsNuevo("otro-" + evento)).isTrue();
    }
}
