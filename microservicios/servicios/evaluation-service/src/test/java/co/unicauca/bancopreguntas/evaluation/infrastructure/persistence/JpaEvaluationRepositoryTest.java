package co.unicauca.bancopreguntas.evaluation.infrastructure.persistence;

import co.unicauca.bancopreguntas.evaluation.EmbeddedPostgresConfig;
import co.unicauca.bancopreguntas.evaluation.domain.CasoDeEvaluacion;
import co.unicauca.bancopreguntas.evaluation.domain.Decision;
import co.unicauca.bancopreguntas.evaluation.domain.EstadoRonda;
import co.unicauca.bancopreguntas.evaluation.domain.RondaDeEvaluacion;
import co.unicauca.bancopreguntas.evaluation.domain.Unanimidad;
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
 * Prueba del adaptador de persistencia contra un PostgreSQL real embebido (sin Docker): aplica las
 * migraciones de Flyway, valida el mapeo JPA contra el esquema y ejecuta las consultas de las bandejas.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({JpaEvaluationRepository.class, JpaProcessedEvents.class, EmbeddedPostgresConfig.class,
        JpaEvaluationRepositoryTest.RelojConfig.class})
@DisplayName("Persistencia de revisiones (PostgreSQL)")
class JpaEvaluationRepositoryTest {

    private static final Instant T0 = Instant.parse("2026-10-08T10:00:00Z");
    private static final Instant T1 = T0.plusSeconds(60);
    private static final Instant T2 = T0.plusSeconds(120);
    private static final Unanimidad POLITICA = new Unanimidad();

    @TestConfiguration
    static class RelojConfig {

        @Bean
        Clock clock() {
            return Clock.systemUTC();
        }
    }

    @Autowired
    private JpaEvaluationRepository repositorio;

    @Autowired
    private JpaProcessedEvents procesados;

    @Autowired
    private ReviewCaseJpaRepository jpa;

    @Autowired
    private EntityManager em;

    private static String id() {
        return UUID.randomUUID().toString();
    }

    private CasoDeEvaluacion nuevoCaso(String preguntaId, Instant ahora) {
        CasoDeEvaluacion caso = CasoDeEvaluacion.nuevo(id(), preguntaId, "autor-1", "Patrón Observer", "LECTURA_CRITICA", ahora);
        caso.iniciarRonda(1, ahora);
        return caso;
    }

    private void guardarYLimpiar(CasoDeEvaluacion caso) {
        repositorio.guardar(caso);
        jpa.flush();
        em.clear(); // las lecturas siguientes van a la base, no a la caché de la sesión
    }

    @Test
    @DisplayName("guarda y recupera un caso con su ronda pendiente")
    void guardaYRecupera() {
        String q = id();
        guardarYLimpiar(nuevoCaso(q, T0));

        CasoDeEvaluacion leido = repositorio.buscarPorPreguntaId(q).orElseThrow();

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
    @DisplayName("guarda asignaciones y evaluaciones, y las rehidrata en orden")
    void asignacionesYEvaluaciones() {
        String q = id();
        CasoDeEvaluacion caso = nuevoCaso(q, T0);
        caso.registrarAsignaciones(caso.rondaActual().numero(), List.of("r1", "r2"), "admin-1", false, T1);
        caso.evaluar("r1", Decision.APROBAR, "Buena", POLITICA, T2);
        guardarYLimpiar(caso);

        CasoDeEvaluacion leido = repositorio.buscarPorPreguntaId(q).orElseThrow();

        RondaDeEvaluacion ronda = leido.rondaActual();
        assertThat(ronda.estado()).isEqualTo(EstadoRonda.EN_CURSO);
        assertThat(ronda.revisores()).containsExactly("r1", "r2");
        assertThat(ronda.asignaciones().get(0).asignadoPor()).isEqualTo("admin-1");
        assertThat(ronda.asignaciones().get(0).automatica()).isFalse();
        assertThat(ronda.evaluacionDe("r1")).get().satisfies(e -> {
            assertThat(e.decision()).isEqualTo(Decision.APROBAR);
            assertThat(e.observacion()).isEqualTo("Buena");
            assertThat(e.enviadaEn()).isEqualTo(T2);
            assertThat(e.tardia()).isFalse();
        });
        assertThat(ronda.evaluacionDe("r2")).isEmpty();
    }

    @Test
    @DisplayName("guardar de nuevo agrega lo nuevo sin duplicar lo anterior")
    void guardarVariasVeces() {
        String q = id();
        CasoDeEvaluacion caso = nuevoCaso(q, T0);
        caso.registrarAsignaciones(caso.rondaActual().numero(), List.of("r1"), "admin-1", false, T1);
        guardarYLimpiar(caso);

        CasoDeEvaluacion releido = repositorio.buscarPorPreguntaId(q).orElseThrow();
        releido.registrarAsignaciones(releido.rondaActual().numero(), List.of("r2"), "admin-1", false, T1);
        releido.evaluar("r1", Decision.APROBAR, null, POLITICA, T2);
        guardarYLimpiar(releido);
        guardarYLimpiar(releido); // un guardado idempotente

        CasoDeEvaluacion final_ = repositorio.buscarPorPreguntaId(q).orElseThrow();
        assertThat(final_.rondaActual().revisores()).containsExactly("r1", "r2");
        assertThat(final_.rondaActual().cantidadDeEvaluaciones()).isEqualTo(1);
    }

    @Test
    @DisplayName("conserva todas las rondas con su historial y los revisores que reasigna el sistema")
    void variasRondas() {
        String q = id();
        CasoDeEvaluacion caso = nuevoCaso(q, T0);
        caso.registrarAsignaciones(caso.rondaActual().numero(), List.of("r1", "r2"), "admin-1", false, T1);
        caso.evaluar("r1", Decision.RECHAZAR, "Falta contexto", POLITICA, T1);
        caso.evaluar("r2", Decision.APROBAR, "Bien", POLITICA, T2); // tardía
        caso.cerrarRondaPorReapertura(T2);
        caso.iniciarRonda(2, T2);
        caso.registrarAsignaciones(2, List.of("r1", "r2"), "system", true, T2);
        guardarYLimpiar(caso);

        CasoDeEvaluacion leido = repositorio.buscarPorPreguntaId(q).orElseThrow();

        assertThat(leido.rondas()).hasSize(2);
        RondaDeEvaluacion primera = leido.rondas().get(0);
        assertThat(primera.estado()).isEqualTo(EstadoRonda.CERRADA);
        assertThat(primera.decididaEn()).contains(T1);
        assertThat(primera.cerradaEn()).contains(T2);
        assertThat(primera.evaluacionDe("r2").orElseThrow().tardia()).isTrue();
        RondaDeEvaluacion segunda = leido.rondas().get(1);
        assertThat(segunda.estado()).isEqualTo(EstadoRonda.EN_CURSO);
        assertThat(segunda.revisores()).containsExactly("r1", "r2");
        assertThat(segunda.asignaciones()).allMatch(a -> a.automatica() && a.asignadoPor().equals("system"));
        assertThat(segunda.evaluaciones()).isEmpty();
    }

    @Test
    @DisplayName("la observación es opcional al aprobar y se guarda nula")
    void observacionNula() {
        String q = id();
        CasoDeEvaluacion caso = nuevoCaso(q, T0);
        caso.registrarAsignaciones(caso.rondaActual().numero(), List.of("r1"), "admin-1", false, T1);
        caso.evaluar("r1", Decision.APROBAR, null, POLITICA, T2);
        guardarYLimpiar(caso);

        CasoDeEvaluacion leido = repositorio.buscarPorPreguntaId(q).orElseThrow();

        assertThat(leido.rondaActual().estado()).isEqualTo(EstadoRonda.APROBADA);
        assertThat(leido.rondaActual().evaluacionDe("r1").orElseThrow().observacion()).isNull();
    }

    @Test
    @DisplayName("no admite dos casos para la misma pregunta")
    void unCasoPorPregunta() {
        String q = id();
        guardarYLimpiar(nuevoCaso(q, T0));
        ReviewCaseEntity duplicado = new ReviewCaseEntity();
        ReviewCaseMapper.sincronizar(nuevoCaso(q, T0), duplicado);

        assertThatThrownBy(() -> {
            jpa.saveAndFlush(duplicado);
        }).isInstanceOf(DataIntegrityViolationException.class);
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
