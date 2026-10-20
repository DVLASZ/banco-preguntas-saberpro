package co.unicauca.bancopreguntas.simulation.infrastructure.persistence;

import co.unicauca.bancopreguntas.simulation.EmbeddedPostgresConfig;
import co.unicauca.bancopreguntas.simulation.domain.CriteriosDeSimulacro;
import co.unicauca.bancopreguntas.simulation.domain.EstadoSimulacro;
import co.unicauca.bancopreguntas.simulation.domain.Pagina;
import co.unicauca.bancopreguntas.simulation.domain.PreguntaDeCatalogo;
import co.unicauca.bancopreguntas.simulation.domain.Preguntas;
import co.unicauca.bancopreguntas.simulation.domain.Simulacro;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Prueba de los adaptadores de persistencia contra un PostgreSQL real embebido (sin Docker): aplica las
 * migraciones de Flyway y valida el mapeo JPA contra el esquema.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({JpaSimulacroRepository.class, JpaCatalogRepository.class, JpaProcessedEvents.class,
        EmbeddedPostgresConfig.class, JpaSimulacroRepositoryTest.Config.class})
@DisplayName("Persistencia de simulacros (PostgreSQL)")
class JpaSimulacroRepositoryTest {

    private static final Instant T0 = Instant.parse("2026-10-16T10:00:00Z");

    @TestConfiguration
    static class Config {

        @Bean
        Clock clock() {
            return Clock.systemUTC();
        }

        @Bean
        ObjectMapper objectMapper() {
            return new ObjectMapper();
        }
    }

    @Autowired
    private JpaSimulacroRepository simulacros;

    @Autowired
    private JpaCatalogRepository catalogo;

    @Autowired
    private JpaProcessedEvents procesados;

    @Autowired
    private EntityManager em;

    private static String id() {
        return UUID.randomUUID().toString();
    }

    private static Simulacro nuevo(String estudiante, Instant inicio) {
        List<PreguntaDeCatalogo> preguntas = List.of(Preguntas.pregunta(id()), Preguntas.pregunta(id(), "INGLES", "AVANZADO", "Verbos", 'A'));
        return Simulacro.iniciar(id(), estudiante, new CriteriosDeSimulacro(Set.of("LECTURA_CRITICA", "INGLES"), Set.of("Verbos"), null, 2, 20),
                preguntas, inicio);
    }

    // ---------------------------------------------------------------- catálogo

    @Test
    @DisplayName("guarda una pregunta en el catálogo, la lee igual y actualiza la existente")
    void catalogoGuardaYActualiza() {
        String qid = id();
        catalogo.guardar(Preguntas.pregunta(qid));
        em.flush();
        em.clear();

        PreguntaDeCatalogo leida = catalogo.buscar(qid).orElseThrow();
        assertThat(leida).isEqualTo(Preguntas.pregunta(qid));

        catalogo.guardar(leida.retirada());
        em.clear();

        assertThat(catalogo.buscar(qid).orElseThrow().disponible()).isFalse();
        assertThat(catalogo.disponibles()).extracting(PreguntaDeCatalogo::preguntaId).doesNotContain(qid);
        assertThat(catalogo.buscar("no-existe")).isEmpty();
    }

    @Test
    @DisplayName("el resumen cuenta solo las preguntas disponibles, por competencia y dificultad")
    void resumen() {
        int antes = catalogo.resumen().total();
        catalogo.guardar(Preguntas.pregunta(id(), "INGLES", "BASICO", "t", 'A'));
        catalogo.guardar(Preguntas.pregunta(id(), "INGLES", "AVANZADO", "t", 'A'));
        catalogo.guardar(Preguntas.pregunta(id(), "INGLES", "AVANZADO", "t", 'A').retirada());

        var resumen = catalogo.resumen();

        assertThat(resumen.total()).isEqualTo(antes + 2);
        assertThat(resumen.porCompetencia().get("INGLES")).isGreaterThanOrEqualTo(2);
        assertThat(resumen.porDificultad()).containsKeys("BASICO", "AVANZADO");
    }

    // ---------------------------------------------------------------- simulacros

    @Test
    @DisplayName("guarda un simulacro con sus preguntas y criterios y lo lee igual")
    void guardaYLee() {
        Simulacro s = nuevo("estudiante1", T0);
        simulacros.guardar(s);
        em.clear();

        Simulacro leido = simulacros.buscar(s.getId()).orElseThrow();

        assertThat(leido.getEstudianteId()).isEqualTo("estudiante1");
        assertThat(leido.getEstado()).isEqualTo(EstadoSimulacro.EN_CURSO);
        assertThat(leido.getIniciadoEn()).isEqualTo(T0);
        assertThat(leido.getVenceEn()).isEqualTo(T0.plus(Duration.ofMinutes(20)));
        assertThat(leido.getItems()).hasSize(2);
        assertThat(leido.getItems().get(1).pregunta().respuestaCorrecta()).isEqualTo('A');
        assertThat(leido.getItems().get(1).pregunta().competencia()).isEqualTo("INGLES");
        assertThat(leido.getItems().get(0).pregunta().preguntaId()).isEqualTo(s.getItems().get(0).pregunta().preguntaId());
        assertThat(leido.getCriterios().competencias()).containsExactlyInAnyOrder("LECTURA_CRITICA", "INGLES");
        assertThat(leido.getCriterios().temas()).containsExactly("verbos");
        assertThat(leido.getCriterios().dificultades()).isEmpty();
        assertThat(leido.getCriterios().cantidad()).isEqualTo(2);
        assertThat(leido.getResultado()).isNull();
    }

    @Test
    @DisplayName("las respuestas y la calificación se actualizan sobre el mismo simulacro")
    void actualiza() {
        Simulacro s = nuevo("estudiante1", T0);
        simulacros.guardar(s);
        em.clear();

        Simulacro cargado = simulacros.buscar(s.getId()).orElseThrow();
        String primera = cargado.getItems().get(0).pregunta().preguntaId();
        cargado.responder(primera, 'B', T0.plusSeconds(30));
        simulacros.guardar(cargado);
        em.clear();
        assertThat(simulacros.buscar(s.getId()).orElseThrow().getItems().get(0).respuesta()).isEqualTo('B');

        Simulacro enCurso = simulacros.buscar(s.getId()).orElseThrow();
        enCurso.finalizar(T0.plusSeconds(120));
        simulacros.guardar(enCurso);
        em.clear();

        Simulacro terminado = simulacros.buscar(s.getId()).orElseThrow();
        assertThat(terminado.getEstado()).isEqualTo(EstadoSimulacro.FINALIZADO);
        assertThat(terminado.getFinalizadoEn()).isEqualTo(T0.plusSeconds(120));
        assertThat(terminado.getResultado().correctas()).isEqualTo(1);
        assertThat(terminado.getResultado().sinResponder()).isEqualTo(1);
        assertThat(terminado.getResultado().puntaje()).isEqualTo(50.0);
        assertThat(terminado.getResultado().porCompetencia()).hasSize(2);
        assertThat(terminado.revelaRespuestas()).isTrue();
    }

    @Test
    @DisplayName("lista los simulacros de un estudiante, el más reciente primero y paginados")
    void delEstudiante() {
        String estudiante = "est-" + id();
        simulacros.guardar(nuevo(estudiante, T0));
        Simulacro reciente = nuevo(estudiante, T0.plusSeconds(60));
        simulacros.guardar(reciente);
        simulacros.guardar(nuevo("otro-" + id(), T0));
        em.clear();

        Pagina<Simulacro> pagina = simulacros.delEstudiante(estudiante, 1, 1);

        assertThat(pagina.totalElementos()).isEqualTo(2);
        assertThat(pagina.totalPaginas()).isEqualTo(2);
        assertThat(pagina.elementos()).singleElement().satisfies(s -> assertThat(s.getId()).isEqualTo(reciente.getId()));
        assertThat(simulacros.delEstudiante(estudiante, 2, 1).elementos()).hasSize(1);
    }

    @Test
    @DisplayName("encuentra los simulacros en curso cuyo tiempo ya se agotó")
    void vencidos() {
        Simulacro viejo = nuevo("est-" + id(), T0.minus(Duration.ofHours(2)));
        Simulacro vigente = nuevo("est-" + id(), T0);
        Simulacro terminadoViejo = nuevo("est-" + id(), T0.minus(Duration.ofHours(2)));
        terminadoViejo.finalizar(T0.minus(Duration.ofHours(1)).minusSeconds(1800));
        simulacros.guardar(viejo);
        simulacros.guardar(vigente);
        simulacros.guardar(terminadoViejo);
        em.clear();

        List<Simulacro> vencidos = simulacros.vencidos(T0, 100);

        assertThat(vencidos).extracting(Simulacro::getId).contains(viejo.getId())
                .doesNotContain(vigente.getId(), terminadoViejo.getId());
    }

    @Test
    @DisplayName("registra cada evento una sola vez")
    void idempotencia() {
        String evento = id();

        assertThat(procesados.registrarSiEsNuevo(evento)).isTrue();
        assertThat(procesados.registrarSiEsNuevo(evento)).isFalse();
    }
}
