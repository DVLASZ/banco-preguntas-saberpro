package co.unicauca.saberpro.preguntas.domain;

import co.unicauca.saberpro.preguntas.domain.validation.ContenidoDePrueba;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.lenient;

/** Búsqueda de "mis preguntas" con filtros y paginación (HU-03, RF-07). */
@ExtendWith(MockitoExtension.class)
class BusquedaPaginadaTest {

    private static final String AUTOR = "autor1";

    @Mock
    private QuestionRepository repository;

    private QuestionService service;

    @BeforeEach
    void setUp() {
        service = new QuestionService(repository);
        List<Question> banco = new ArrayList<>();
        banco.add(pregunta("P-001", "Diseño guiado por el dominio", EstadoPregunta.BORRADOR,
                Competencia.LECTURA_CRITICA, "Diseño de software", AUTOR));
        banco.add(pregunta("P-002", "Principio de responsabilidad única", EstadoPregunta.BORRADOR,
                Competencia.RAZONAMIENTO_CUANTITATIVO, "Principios SOLID", AUTOR));
        banco.add(pregunta("P-003", "Patrón Observer", EstadoPregunta.PENDIENTE_REVISION,
                Competencia.COMPETENCIAS_CIUDADANAS, "Patrones de diseño", AUTOR));
        banco.add(pregunta("P-004", "Arquitectura en capas", EstadoPregunta.EN_REVISION,
                Competencia.COMUNICACION_ESCRITA, "Arquitectura", AUTOR));
        banco.add(pregunta("P-005", "Micropatrón MVC", EstadoPregunta.APROBADA,
                Competencia.INGLES, "Patrones de diseño", AUTOR));
        banco.add(pregunta("P-006", "Inversión de dependencias", EstadoPregunta.RECHAZADA,
                Competencia.LECTURA_CRITICA, "Principios SOLID", AUTOR));
        banco.add(pregunta("P-007", "Patrón Factory", EstadoPregunta.PUBLICADA,
                Competencia.LECTURA_CRITICA, "Patrones de diseño", "autor2"));
        lenient().when(repository.obtenerTodas()).thenReturn(banco);
    }

    private static Question pregunta(String id, String nombre, EstadoPregunta estado, Competencia competencia,
                                     String tema, String autor) {
        ContenidoPregunta c = ContenidoDePrueba.valido().build();
        return Question.builder().id(id).nombre(nombre).contexto(c.contexto()).enunciado(c.enunciado())
                .opciones(new QuestionDistractors(c.opcionA(), c.opcionB(), c.opcionC(), c.opcionD()))
                .respuestaCorrecta(c.respuestaCorrecta().charAt(0)).justificacion(c.justificacion())
                .bibliografia(c.bibliografia()).estado(estado).competencia(competencia).tema(tema)
                .subtema(c.subtema()).dificultad(c.dificultad()).autor(autor).build();
    }

    private static List<String> ids(Pagina<Question> pagina) {
        return pagina.elementos().stream().map(Question::getId).toList();
    }

    // ---- paginación ----

    @Test
    void primeraPagina_traeSoloElTamanoPedidoYSoloLasDelAutor() {
        Pagina<Question> pagina = service.buscarDelAutor(AUTOR, FiltroPreguntas.sinFiltros(), 1, 4);

        assertEquals(List.of("P-001", "P-002", "P-003", "P-004"), ids(pagina));
        assertEquals(1, pagina.numero());
        assertEquals(6, pagina.totalElementos());
        assertEquals(2, pagina.totalPaginas());
        assertTrue(pagina.haySiguiente());
        assertFalse(pagina.hayAnterior());
    }

    @Test
    void ultimaPagina_traeLasQueSobran() {
        Pagina<Question> pagina = service.buscarDelAutor(AUTOR, FiltroPreguntas.sinFiltros(), 2, 4);

        assertEquals(List.of("P-005", "P-006"), ids(pagina));
        assertFalse(pagina.haySiguiente());
        assertTrue(pagina.hayAnterior());
    }

    @Test
    void paginaInexistente_devuelveLaUltima() {
        Pagina<Question> pagina = service.buscarDelAutor(AUTOR, FiltroPreguntas.sinFiltros(), 99, 4);

        assertEquals(2, pagina.numero());
        assertEquals(List.of("P-005", "P-006"), ids(pagina));
    }

    @Test
    void sinResultados_devuelvePrimeraPaginaVaciaYUnaPaginaEnTotal() {
        Pagina<Question> pagina = service.buscarDelAutor("nadie", FiltroPreguntas.sinFiltros(), 1, 5);

        assertTrue(pagina.elementos().isEmpty());
        assertEquals(1, pagina.numero());
        assertEquals(1, pagina.totalPaginas());
        assertEquals(0, pagina.totalElementos());
    }

    @Test
    void tamanoOPaginaInvalidos_seRechazan() {
        assertThrows(IllegalArgumentException.class,
                () -> service.buscarDelAutor(AUTOR, FiltroPreguntas.sinFiltros(), 1, 0));
        assertThrows(IllegalArgumentException.class,
                () -> service.buscarDelAutor(AUTOR, FiltroPreguntas.sinFiltros(), 0, 5));
    }

    @Test
    void filtroNulo_seTrataComoSinFiltros() {
        assertEquals(6, service.buscarDelAutor(AUTOR, null, 1, 10).totalElementos());
    }

    // ---- filtros ----

    @Test
    void filtroPorEstado() {
        Pagina<Question> pagina = service.buscarDelAutor(AUTOR,
                new FiltroPreguntas(EstadoPregunta.BORRADOR, null, null), 1, 10);

        assertEquals(List.of("P-001", "P-002"), ids(pagina));
    }

    @Test
    void filtroPorCompetencia() {
        Pagina<Question> pagina = service.buscarDelAutor(AUTOR,
                new FiltroPreguntas(null, Competencia.LECTURA_CRITICA, null), 1, 10);

        assertEquals(List.of("P-001", "P-006"), ids(pagina));
    }

    @Test
    void filtroPorTexto_buscaEnNombreYTemaSinImportarTildesNiMayusculas() {
        assertEquals(List.of("P-003"), ids(service.buscarDelAutor(AUTOR,
                new FiltroPreguntas(null, null, "PATRON OBSERVER"), 1, 10)));
        assertEquals(List.of("P-002", "P-006"), ids(service.buscarDelAutor(AUTOR,
                new FiltroPreguntas(null, null, "solid"), 1, 10)));
    }

    @Test
    void filtrosCombinados_seExigenTodos() {
        Pagina<Question> pagina = service.buscarDelAutor(AUTOR,
                new FiltroPreguntas(EstadoPregunta.RECHAZADA, Competencia.LECTURA_CRITICA, "solid"), 1, 10);

        assertEquals(List.of("P-006"), ids(pagina));
        assertTrue(service.buscarDelAutor(AUTOR,
                new FiltroPreguntas(EstadoPregunta.BORRADOR, Competencia.INGLES, null), 1, 10)
                .elementos().isEmpty());
    }

    @Test
    void textoEnBlanco_noFiltra() {
        assertEquals(6, service.buscarDelAutor(AUTOR, new FiltroPreguntas(null, null, "   "), 1, 10)
                .totalElementos());
    }

    @Test
    void filtroYPaginacion_seCombinan() {
        Pagina<Question> pagina = service.buscarDelAutor(AUTOR,
                new FiltroPreguntas(null, null, "patrones de diseno"), 1, 1);

        assertEquals(List.of("P-003"), ids(pagina));
        assertEquals(2, pagina.totalPaginas());
        assertEquals(List.of("P-005"), ids(service.buscarDelAutor(AUTOR,
                new FiltroPreguntas(null, null, "patrones de diseno"), 2, 1)));
    }
}
