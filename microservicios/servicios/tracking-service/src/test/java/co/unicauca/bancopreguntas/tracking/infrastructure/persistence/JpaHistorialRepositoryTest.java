package co.unicauca.bancopreguntas.tracking.infrastructure.persistence;

import co.unicauca.bancopreguntas.tracking.EmbeddedPostgresConfig;
import co.unicauca.bancopreguntas.tracking.domain.Pagina;
import co.unicauca.bancopreguntas.tracking.domain.ResultadoRegistrado;
import co.unicauca.bancopreguntas.tracking.domain.Resultados;
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
import java.util.UUID;

import static co.unicauca.bancopreguntas.tracking.domain.Resultados.comp;
import static co.unicauca.bancopreguntas.tracking.domain.Resultados.resultado;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Prueba de los adaptadores de persistencia contra un PostgreSQL real embebido (sin Docker): aplica las
 * migraciones de Flyway y valida el mapeo JPA contra el esquema.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({JpaHistorialRepository.class, JpaProcessedEvents.class, EmbeddedPostgresConfig.class,
        JpaHistorialRepositoryTest.Config.class})
@DisplayName("Persistencia del seguimiento (PostgreSQL)")
class JpaHistorialRepositoryTest {

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
    private JpaHistorialRepository historial;

    @Autowired
    private JpaProcessedEvents procesados;

    @Autowired
    private EntityManager em;

    private static String id() {
        return UUID.randomUUID().toString();
    }

    @Test
    @DisplayName("guarda un resultado con su detalle por competencia y lo lee igual")
    void guardaYLee() {
        String estudiante = "est-" + id();
        ResultadoRegistrado original = resultado(id(), estudiante, 1, 7, 10, comp("INGLES", 3, 4), comp("LECTURA_CRITICA", 4, 6));
        historial.guardar(original);
        em.clear();

        ResultadoRegistrado leido = historial.todosDelEstudiante(estudiante).get(0);

        assertThat(leido).isEqualTo(original);
    }

    @Test
    @DisplayName("guardar dos veces el mismo simulacro deja un solo resultado")
    void sinDuplicados() {
        String estudiante = "est-" + id();
        String simulacro = id();

        historial.guardar(resultado(simulacro, estudiante, 1, 7, 10));
        historial.guardar(resultado(simulacro, estudiante, 1, 2, 10));
        em.clear();

        assertThat(historial.todosDelEstudiante(estudiante)).singleElement()
                .satisfies(r -> assertThat(r.correctas()).isEqualTo(7));
    }

    @Test
    @DisplayName("lista el historial de un estudiante, el más reciente primero y paginado")
    void paginado() {
        String estudiante = "est-" + id();
        String viejo = id();
        String reciente = id();
        historial.guardar(resultado(viejo, estudiante, 1, 5, 10));
        historial.guardar(resultado(reciente, estudiante, 5, 6, 10));
        historial.guardar(resultado(id(), "otro-" + id(), 3, 6, 10));
        em.clear();

        Pagina<ResultadoRegistrado> primera = historial.delEstudiante(estudiante, 1, 1);

        assertThat(primera.totalElementos()).isEqualTo(2);
        assertThat(primera.totalPaginas()).isEqualTo(2);
        assertThat(primera.elementos()).singleElement().satisfies(r -> assertThat(r.simulacroId()).isEqualTo(reciente));
        assertThat(historial.delEstudiante(estudiante, 2, 1).elementos()).singleElement()
                .satisfies(r -> assertThat(r.simulacroId()).isEqualTo(viejo));
    }

    @Test
    @DisplayName("todos() trae los resultados de todos los estudiantes")
    void todos() {
        String a = "est-" + id();
        String b = "est-" + id();
        historial.guardar(resultado(id(), a, 1, 5, 10));
        historial.guardar(resultado(id(), b, 1, 5, 10));
        em.clear();

        assertThat(historial.todos()).extracting(ResultadoRegistrado::estudianteId).contains(a, b);
    }

    @Test
    @DisplayName("un estudiante sin simulacros tiene el historial vacío")
    void vacio() {
        assertThat(historial.todosDelEstudiante("nadie-" + id())).isEmpty();
        assertThat(historial.delEstudiante("nadie-" + id(), 1, 10).totalElementos()).isZero();
        assertThat(Resultados.T0).isNotNull();
    }

    @Test
    @DisplayName("registra cada evento una sola vez")
    void idempotencia() {
        String evento = id();

        assertThat(procesados.registrarSiEsNuevo(evento)).isTrue();
        assertThat(procesados.registrarSiEsNuevo(evento)).isFalse();
    }
}
