package co.unicauca.bancopreguntas.question.infrastructure.persistence;

import co.unicauca.bancopreguntas.question.domain.CambioEstado;
import co.unicauca.bancopreguntas.question.domain.Competencia;
import co.unicauca.bancopreguntas.question.domain.Dificultad;
import co.unicauca.bancopreguntas.question.domain.EstadoPregunta;
import co.unicauca.bancopreguntas.question.domain.FiltroPreguntas;
import co.unicauca.bancopreguntas.question.domain.Pagina;
import co.unicauca.bancopreguntas.question.domain.Question;
import co.unicauca.bancopreguntas.question.domain.QuestionDistractors;
import co.unicauca.bancopreguntas.question.EmbeddedPostgresConfig;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Prueba del adaptador de persistencia contra un PostgreSQL real embebido (sin Docker): aplica las
 * migraciones de Flyway, valida el mapeo JPA contra el esquema y ejecuta la búsqueda nativa con
 * {@code unaccent}.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({JpaQuestionRepository.class, JpaProcessedEvents.class, EmbeddedPostgresConfig.class,
        JpaQuestionRepositoryTest.RelojConfig.class})
@DisplayName("Persistencia de preguntas (PostgreSQL)")
class JpaQuestionRepositoryTest {

    private static final Instant T0 = Instant.parse("2026-10-08T10:00:00Z");

    @TestConfiguration
    static class RelojConfig {

        @Bean
        Clock clock() {
            return Clock.systemUTC();
        }
    }

    @Autowired
    private JpaQuestionRepository repositorio;

    @Autowired
    private JpaProcessedEvents procesados;

    private static Question.Builder base(String id) {
        return Question.builder()
                .id(id).nombre("Patrón Observer").contexto("Contexto de la pregunta")
                .enunciado("¿Qué patrón notifica a varios objetos?")
                .opciones(new QuestionDistractors("Factory", "Observer", "Singleton", "Adapter"))
                .respuestaCorrecta('B').justificacion("Define una dependencia uno a muchos")
                .bibliografia("Gamma et al. (1994)").estado(EstadoPregunta.BORRADOR)
                .competencia(Competencia.LECTURA_CRITICA).tema("Patrones").subtema("Observer")
                .dificultad(Dificultad.INTERMEDIO).autor("autor-1")
                .creadaEn(T0).actualizadaEn(T0);
    }

    @Test
    @DisplayName("guarda una pregunta y la recupera con todos sus campos")
    void guardaYRecupera() {
        repositorio.guardar(base("Q-1").build());

        Question leida = repositorio.buscarPorId("Q-1").orElseThrow();

        assertThat(leida.getNombre()).isEqualTo("Patrón Observer");
        assertThat(leida.getContexto()).isEqualTo("Contexto de la pregunta");
        assertThat(leida.getEnunciado()).isEqualTo("¿Qué patrón notifica a varios objetos?");
        assertThat(leida.getOpciones().getOpcionA()).isEqualTo("Factory");
        assertThat(leida.getOpciones().getOpcionD()).isEqualTo("Adapter");
        assertThat(leida.getRespuestaCorrecta()).isEqualTo('B');
        assertThat(leida.getJustificacion()).isEqualTo("Define una dependencia uno a muchos");
        assertThat(leida.getBibliografia()).isEqualTo("Gamma et al. (1994)");
        assertThat(leida.getEstado()).isEqualTo(EstadoPregunta.BORRADOR);
        assertThat(leida.getCompetencia()).isEqualTo(Competencia.LECTURA_CRITICA);
        assertThat(leida.getTema()).isEqualTo("Patrones");
        assertThat(leida.getSubtema()).isEqualTo("Observer");
        assertThat(leida.getDificultad()).isEqualTo(Dificultad.INTERMEDIO);
        assertThat(leida.getAutor()).isEqualTo("autor-1");
        assertThat(leida.getCreadaEn()).isEqualTo(T0);
        assertThat(leida.getActualizadaEn()).isEqualTo(T0);
    }

    @Test
    @DisplayName("persiste el número de versión y lo actualiza")
    void version() {
        repositorio.guardar(base("Q-1").build());
        assertThat(repositorio.buscarPorId("Q-1").orElseThrow().getVersion()).isEqualTo(1);

        repositorio.guardar(base("Q-1").version(3).build());
        assertThat(repositorio.buscarPorId("Q-1").orElseThrow().getVersion()).isEqualTo(3);

        repositorio.guardar(base("Q-1").version(4).build());
        assertThat(repositorio.buscarPorId("Q-1").orElseThrow().getVersion()).isEqualTo(4);
    }

    @Test
    @DisplayName("buscarPorId devuelve vacío si no existe")
    void noExiste() {
        assertThat(repositorio.buscarPorId("no-existe")).isEmpty();
    }

    @Test
    @DisplayName("guardar una pregunta existente la actualiza en vez de duplicarla")
    void actualiza() {
        repositorio.guardar(base("Q-1").build());

        Question enviada = base("Q-1").estado(EstadoPregunta.PENDIENTE_REVISION).nombre("Otro nombre")
                .actualizadaEn(T0.plusSeconds(60)).build();
        repositorio.guardar(enviada);

        Question leida = repositorio.buscarPorId("Q-1").orElseThrow();
        assertThat(leida.getEstado()).isEqualTo(EstadoPregunta.PENDIENTE_REVISION);
        assertThat(leida.getNombre()).isEqualTo("Otro nombre");
        assertThat(leida.getCreadaEn()).isEqualTo(T0);
        assertThat(leida.getActualizadaEn()).isEqualTo(T0.plusSeconds(60));
        assertThat(repositorio.buscar(null, FiltroPreguntas.sinFiltros(), 1, 10).totalElementos()).isEqualTo(1);
    }

    @Test
    @DisplayName("el historial de estados se devuelve en orden y admite un origen nulo")
    void historial() {
        repositorio.guardar(base("Q-1").build());
        repositorio.registrarCambioEstado("Q-1", new CambioEstado(null, EstadoPregunta.BORRADOR, "autor-1", T0));
        repositorio.registrarCambioEstado("Q-1",
                new CambioEstado(EstadoPregunta.BORRADOR, EstadoPregunta.PENDIENTE_REVISION, "autor-1", T0.plusSeconds(10)));
        repositorio.registrarCambioEstado("Q-1",
                new CambioEstado(EstadoPregunta.PENDIENTE_REVISION, EstadoPregunta.EN_REVISION, null, T0.plusSeconds(20)));

        List<CambioEstado> historial = repositorio.historial("Q-1");

        assertThat(historial).extracting(CambioEstado::hacia).containsExactly(
                EstadoPregunta.BORRADOR, EstadoPregunta.PENDIENTE_REVISION, EstadoPregunta.EN_REVISION);
        assertThat(historial.get(0).desde()).isNull();
        assertThat(historial.get(2).cambiadoPor()).isNull();
        assertThat(historial.get(1).fecha()).isEqualTo(T0.plusSeconds(10));
    }

    @Test
    @DisplayName("no se puede registrar historial de una pregunta que no existe")
    void historialDeUnaPreguntaInexistente() {
        assertThatThrownBy(() -> repositorio.registrarCambioEstado("fantasma",
                new CambioEstado(null, EstadoPregunta.BORRADOR, "autor-1", T0)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("busca solo las preguntas del autor indicado")
    void porAutor() {
        repositorio.guardar(base("Q-1").build());
        repositorio.guardar(base("Q-2").autor("autor-2").build());

        Pagina<Question> mias = repositorio.buscar("autor-1", FiltroPreguntas.sinFiltros(), 1, 10);
        Pagina<Question> todas = repositorio.buscar(null, FiltroPreguntas.sinFiltros(), 1, 10);

        assertThat(mias.elementos()).extracting(Question::getId).containsExactly("Q-1");
        assertThat(todas.totalElementos()).isEqualTo(2);
    }

    @Test
    @DisplayName("filtra por estado y por competencia")
    void porEstadoYCompetencia() {
        repositorio.guardar(base("Q-1").build());
        repositorio.guardar(base("Q-2").estado(EstadoPregunta.PENDIENTE_REVISION).build());
        repositorio.guardar(base("Q-3").competencia(Competencia.INGLES).build());

        assertThat(repositorio.buscar(null, new FiltroPreguntas(EstadoPregunta.PENDIENTE_REVISION, null, null), 1, 10)
                .elementos()).extracting(Question::getId).containsExactly("Q-2");
        assertThat(repositorio.buscar(null, new FiltroPreguntas(null, Competencia.INGLES, null), 1, 10)
                .elementos()).extracting(Question::getId).containsExactly("Q-3");
        assertThat(repositorio.buscar(null, new FiltroPreguntas(EstadoPregunta.BORRADOR, Competencia.INGLES, null), 1, 10)
                .elementos()).extracting(Question::getId).containsExactly("Q-3");
    }

    @Test
    @DisplayName("busca texto sin distinguir mayúsculas ni tildes en nombre, pregunta, tema y subtema")
    void textoSinTildes() {
        repositorio.guardar(base("Q-1").nombre("Educación vial").build());
        repositorio.guardar(base("Q-2").enunciado("¿Cuál es la mejor opción EDUCATIVA?").build());
        repositorio.guardar(base("Q-3").tema("Pedagogía").build());
        repositorio.guardar(base("Q-4").subtema("Ñandú").build());

        assertThat(ids(repositorio.buscar(null, new FiltroPreguntas(null, null, "EDUCACION"), 1, 10)))
                .containsExactly("Q-1");
        assertThat(ids(repositorio.buscar(null, new FiltroPreguntas(null, null, "educativa"), 1, 10)))
                .containsExactly("Q-2");
        assertThat(ids(repositorio.buscar(null, new FiltroPreguntas(null, null, "pedagogia"), 1, 10)))
                .containsExactly("Q-3");
        assertThat(ids(repositorio.buscar(null, new FiltroPreguntas(null, null, "nandu"), 1, 10)))
                .containsExactly("Q-4");
    }

    @Test
    @DisplayName("los comodines de LIKE escritos por el usuario se buscan como texto literal")
    void comodinesLiterales() {
        repositorio.guardar(base("Q-1").tema("Descuento 100%").build());
        repositorio.guardar(base("Q-2").tema("Descuento total").build());
        repositorio.guardar(base("Q-3").tema("snake_case").build());

        assertThat(ids(repositorio.buscar(null, new FiltroPreguntas(null, null, "%"), 1, 10))).containsExactly("Q-1");
        assertThat(ids(repositorio.buscar(null, new FiltroPreguntas(null, null, "_"), 1, 10))).containsExactly("Q-3");
    }

    @Test
    @DisplayName("un texto en blanco no filtra")
    void textoEnBlanco() {
        repositorio.guardar(base("Q-1").build());

        assertThat(repositorio.buscar(null, new FiltroPreguntas(null, null, "   "), 1, 10).totalElementos())
                .isEqualTo(1);
    }

    @Test
    @DisplayName("pagina los resultados, los más recientes primero, con el total correcto")
    void paginacion() {
        for (int i = 1; i <= 5; i++) {
            repositorio.guardar(base("Q-" + i).creadaEn(T0.plusSeconds(i)).actualizadaEn(T0.plusSeconds(i)).build());
        }

        Pagina<Question> primera = repositorio.buscar(null, FiltroPreguntas.sinFiltros(), 1, 2);
        Pagina<Question> ultima = repositorio.buscar(null, FiltroPreguntas.sinFiltros(), 3, 2);

        assertThat(ids(primera)).containsExactly("Q-5", "Q-4");
        assertThat(ids(ultima)).containsExactly("Q-1");
        assertThat(primera.totalElementos()).isEqualTo(5);
        assertThat(primera.totalPaginas()).isEqualTo(3);
        assertThat(primera.numero()).isEqualTo(1);
    }

    @Test
    @DisplayName("una página fuera de rango viene vacía pero con el total real")
    void paginaFueraDeRango() {
        repositorio.guardar(base("Q-1").build());
        repositorio.guardar(base("Q-2").build());

        Pagina<Question> pagina = repositorio.buscar(null, FiltroPreguntas.sinFiltros(), 9, 2);

        assertThat(pagina.elementos()).isEmpty();
        assertThat(pagina.totalElementos()).isEqualTo(2);
    }

    @Test
    @DisplayName("los eventos procesados son idempotentes")
    void eventosProcesados() {
        assertThat(procesados.registrarSiEsNuevo("evento-1")).isTrue();
        assertThat(procesados.registrarSiEsNuevo("evento-1")).isFalse();
        assertThat(procesados.registrarSiEsNuevo("evento-2")).isTrue();
    }

    private static List<String> ids(Pagina<Question> pagina) {
        return pagina.elementos().stream().map(Question::getId).toList();
    }
}
