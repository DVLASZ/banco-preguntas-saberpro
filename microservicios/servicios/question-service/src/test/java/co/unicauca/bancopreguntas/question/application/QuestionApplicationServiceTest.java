package co.unicauca.bancopreguntas.question.application;

import co.unicauca.bancopreguntas.question.application.port.EstadoDeCiclo;
import co.unicauca.bancopreguntas.question.domain.AccesoDenegadoException;
import co.unicauca.bancopreguntas.question.domain.CambioEstado;
import co.unicauca.bancopreguntas.question.domain.Competencia;
import co.unicauca.bancopreguntas.question.domain.DependenciaNoDisponibleException;
import co.unicauca.bancopreguntas.question.domain.EstadoPregunta;
import co.unicauca.bancopreguntas.question.domain.FiltroPreguntas;
import co.unicauca.bancopreguntas.question.domain.OperacionNoPermitidaException;
import co.unicauca.bancopreguntas.question.domain.Pagina;
import co.unicauca.bancopreguntas.question.domain.Question;
import co.unicauca.bancopreguntas.question.domain.QuestionNotFoundException;
import co.unicauca.bancopreguntas.question.domain.validation.ContenidoDePrueba;
import co.unicauca.bancopreguntas.question.domain.validation.QuestionValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("QuestionApplicationService")
class QuestionApplicationServiceTest {

    private static final String AUTOR = "autor-1";
    private static final String OTRO = "autor-2";
    private static final Instant INICIO = Instant.parse("2026-10-08T10:00:00Z");

    private RelojManual reloj;
    private InMemoryQuestionRepository repositorio;
    private ValidadorDePrueba validador;
    private CicloDeVidaDePrueba ciclo;
    private QuestionApplicationService servicio;

    @BeforeEach
    void preparar() {
        reloj = new RelojManual(INICIO);
        repositorio = new InMemoryQuestionRepository();
        validador = new ValidadorDePrueba();
        ciclo = new CicloDeVidaDePrueba(reloj);
        AtomicInteger consecutivo = new AtomicInteger();
        servicio = new QuestionApplicationService(repositorio, validador, ciclo, reloj, () -> "Q-" + consecutivo.incrementAndGet());
    }

    private Question borrador() {
        return servicio.crearBorrador(ContenidoDePrueba.valido().build(), AUTOR);
    }

    private Question pendiente() {
        Question creada = borrador();
        servicio.enviarARevision(creada.getId(), AUTOR);
        return servicio.obtener(creada.getId());
    }

    /** Deja la pregunta en el estado que dejaría la revisión y aplica el aviso de lifecycle-service. */
    private Question enEstado(Question pregunta, EstadoPregunta estado) {
        EstadoDeCiclo nuevo = ciclo.forzar(pregunta.getId(), estado);
        servicio.alCambiarElEstado(pregunta.getId(), nuevo.estado(), nuevo.version(), nuevo.secuencia());
        return servicio.obtener(pregunta.getId());
    }

    private Question aprobada() {
        return enEstado(enEstado(pendiente(), EstadoPregunta.EN_REVISION), EstadoPregunta.APROBADA);
    }

    private Question rechazada() {
        return enEstado(enEstado(pendiente(), EstadoPregunta.EN_REVISION), EstadoPregunta.RECHAZADA);
    }

    @Nested
    @DisplayName("Crear un borrador (HU-01)")
    class CrearBorrador {

        @Test
        @DisplayName("guarda la pregunta en Borrador, con su autor, sus fechas y el estado que informó el ciclo de vida")
        void guardaEnBorrador() {
            Question creada = borrador();

            assertThat(creada.getId()).isEqualTo("Q-1");
            assertThat(creada.getEstado()).isEqualTo(EstadoPregunta.BORRADOR);
            assertThat(creada.getAutor()).isEqualTo(AUTOR);
            assertThat(creada.getCreadaEn()).isEqualTo(INICIO);
            assertThat(creada.getActualizadaEn()).isEqualTo(INICIO);
            assertThat(creada.getVersion()).isEqualTo(1);
            assertThat(creada.getEstadoSecuencia()).isEqualTo(1);
            assertThat(servicio.obtener("Q-1").getNombre()).isEqualTo("Patrón Observer");
            assertThat(ciclo.llamadas()).containsExactly("registrar");
        }

        @Test
        @DisplayName("no guarda ni registra nada si incumple la validación estructural y reporta todos los campos")
        void rechazaContenidoInvalido() {
            var invalido = ContenidoDePrueba.valido().contexto("").opcionB("Factory").build();

            assertThatThrownBy(() -> servicio.crearBorrador(invalido, AUTOR))
                    .isInstanceOf(QuestionValidationException.class)
                    .satisfies(e -> assertThat(((QuestionValidationException) e).getViolaciones()).hasSizeGreaterThan(1));
            assertThat(repositorio.cantidad()).isZero();
            assertThat(ciclo.llamadas()).isEmpty();
        }

        @Test
        @DisplayName("exige un autor")
        void exigeAutor() {
            var contenido = ContenidoDePrueba.valido().build();

            assertThatThrownBy(() -> servicio.crearBorrador(contenido, " ")).isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> servicio.crearBorrador(contenido, null)).isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("si el servicio de validación o el de ciclo de vida están caídos, no se guarda nada")
        void dependenciasCaidas() {
            validador.caer();
            assertThatThrownBy(this::crear).isInstanceOf(DependenciaNoDisponibleException.class);
            assertThat(repositorio.cantidad()).isZero();

            QuestionApplicationService otro = new QuestionApplicationService(repositorio, new ValidadorDePrueba(), ciclo, reloj, () -> "Q-9");
            ciclo.caer();
            assertThatThrownBy(() -> otro.crearBorrador(ContenidoDePrueba.valido().build(), AUTOR))
                    .isInstanceOf(DependenciaNoDisponibleException.class);
            assertThat(repositorio.cantidad()).isZero();
        }

        private Question crear() {
            return servicio.crearBorrador(ContenidoDePrueba.valido().build(), AUTOR);
        }
    }

    @Nested
    @DisplayName("Consultar")
    class Consultar {

        @Test
        @DisplayName("una pregunta que no existe lanza QuestionNotFoundException")
        void noExistente() {
            assertThatThrownBy(() -> servicio.obtener("no-existe"))
                    .isInstanceOf(QuestionNotFoundException.class)
                    .hasMessageContaining("no-existe");
            assertThatThrownBy(() -> servicio.historial("no-existe")).isInstanceOf(QuestionNotFoundException.class);
        }

        @Test
        @DisplayName("el historial lo entrega el servicio de ciclo de vida, en orden cronológico")
        void historialEnOrden() {
            Question creada = borrador();
            reloj.avanzar(Duration.ofMinutes(5));
            servicio.enviarARevision(creada.getId(), AUTOR);

            List<CambioEstado> historial = servicio.historial(creada.getId());

            assertThat(historial).extracting(CambioEstado::hacia)
                    .containsExactly(EstadoPregunta.BORRADOR, EstadoPregunta.PENDIENTE_REVISION);
            assertThat(historial.get(1).desde()).isEqualTo(EstadoPregunta.BORRADOR);
            assertThat(historial.get(1).fecha()).isEqualTo(INICIO.plus(Duration.ofMinutes(5)));
        }

        @Test
        @DisplayName("si el servicio de ciclo de vida no responde, el historial falla pero la pregunta se sigue leyendo")
        void historialSinCiclo() {
            Question creada = borrador();
            ciclo.caer();

            assertThatThrownBy(() -> servicio.historial(creada.getId())).isInstanceOf(DependenciaNoDisponibleException.class);
            assertThat(servicio.obtener(creada.getId()).getId()).isEqualTo(creada.getId());
        }
    }

    @Nested
    @DisplayName("Listar las preguntas de un autor (HU-03)")
    class Listar {

        @Test
        @DisplayName("devuelve solo las preguntas del autor, las más recientes primero")
        void soloLasDelAutor() {
            Question primera = borrador();
            reloj.avanzar(Duration.ofMinutes(1));
            Question segunda = borrador();
            servicio.crearBorrador(ContenidoDePrueba.valido().build(), OTRO);

            Pagina<Question> pagina = servicio.buscarDelAutor(AUTOR, null, 1, 10);

            assertThat(pagina.elementos()).extracting(Question::getId)
                    .containsExactly(segunda.getId(), primera.getId());
            assertThat(pagina.totalElementos()).isEqualTo(2);
        }

        @Test
        @DisplayName("aplica los filtros de estado, competencia y texto sin distinguir tildes")
        void aplicaFiltros() {
            Question borr = servicio.crearBorrador(ContenidoDePrueba.valido().nombre("Educación vial").build(), AUTOR);
            Question ingles = servicio.crearBorrador(
                    ContenidoDePrueba.valido().competencia(Competencia.INGLES).build(), AUTOR);
            servicio.enviarARevision(ingles.getId(), AUTOR);

            assertThat(servicio.buscarDelAutor(AUTOR, new FiltroPreguntas(EstadoPregunta.PENDIENTE_REVISION, null, null), 1, 10)
                    .elementos()).extracting(Question::getId).containsExactly(ingles.getId());
            assertThat(servicio.buscarDelAutor(AUTOR, new FiltroPreguntas(null, Competencia.INGLES, null), 1, 10)
                    .elementos()).extracting(Question::getId).containsExactly(ingles.getId());
            assertThat(servicio.buscarDelAutor(AUTOR, new FiltroPreguntas(null, null, "EDUCACION"), 1, 10)
                    .elementos()).extracting(Question::getId).containsExactly(borr.getId());
        }

        @Test
        @DisplayName("pagina los resultados")
        void paginaLosResultados() {
            for (int i = 0; i < 5; i++) {
                borrador();
                reloj.avanzar(Duration.ofMinutes(1));
            }

            Pagina<Question> segunda = servicio.buscarDelAutor(AUTOR, null, 2, 2);

            assertThat(segunda.numero()).isEqualTo(2);
            assertThat(segunda.elementos()).hasSize(2);
            assertThat(segunda.totalElementos()).isEqualTo(5);
            assertThat(segunda.totalPaginas()).isEqualTo(3);
        }

        @Test
        @DisplayName("si la página pedida no existe devuelve la última")
        void paginaInexistenteDevuelveLaUltima() {
            for (int i = 0; i < 3; i++) {
                borrador();
            }

            Pagina<Question> pagina = servicio.buscarDelAutor(AUTOR, null, 9, 2);

            assertThat(pagina.numero()).isEqualTo(2);
            assertThat(pagina.elementos()).hasSize(1);
        }

        @Test
        @DisplayName("sin resultados devuelve la primera página vacía")
        void sinResultados() {
            Pagina<Question> pagina = servicio.buscarDelAutor(AUTOR, null, 1, 10);

            assertThat(pagina.elementos()).isEmpty();
            assertThat(pagina.totalPaginas()).isEqualTo(1);
        }

        @Test
        @DisplayName("rechaza una página o un tamaño inválidos y un autor vacío")
        void rechazaArgumentosInvalidos() {
            assertThatThrownBy(() -> servicio.buscarDelAutor(AUTOR, null, 0, 10))
                    .isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> servicio.buscarDelAutor(AUTOR, null, 1, 0))
                    .isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> servicio.buscarDelAutor(" ", null, 1, 10))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("las pendientes de revisión incluyen las de todos los autores")
        void pendientesDeRevision() {
            Question mia = borrador();
            Question ajena = servicio.crearBorrador(ContenidoDePrueba.valido().build(), OTRO);
            borrador();
            servicio.enviarARevision(mia.getId(), AUTOR);
            servicio.enviarARevision(ajena.getId(), OTRO);

            Pagina<Question> pendientes = servicio.buscarPendientesDeRevision(1, 10);

            assertThat(pendientes.elementos()).extracting(Question::getId)
                    .containsExactlyInAnyOrder(mia.getId(), ajena.getId());
        }
    }

    @Nested
    @DisplayName("Modificar el contenido (RF-06)")
    class Modificar {

        @Test
        @DisplayName("el autor cambia el contenido de su borrador y se actualiza la fecha")
        void cambiaElContenido() {
            Question creada = borrador();
            reloj.avanzar(Duration.ofHours(1));

            Question modificada = servicio.actualizarContenido(creada.getId(),
                    ContenidoDePrueba.valido().nombre("Nuevo nombre").build(), AUTOR);

            assertThat(modificada.getNombre()).isEqualTo("Nuevo nombre");
            assertThat(modificada.getEstado()).isEqualTo(EstadoPregunta.BORRADOR);
            assertThat(modificada.getCreadaEn()).isEqualTo(INICIO);
            assertThat(modificada.getActualizadaEn()).isEqualTo(INICIO.plus(Duration.ofHours(1)));
            assertThat(servicio.obtener(creada.getId()).getNombre()).isEqualTo("Nuevo nombre");
        }

        @Test
        @DisplayName("solo el autor puede modificarla")
        void soloElAutor() {
            Question creada = borrador();

            assertThatThrownBy(() -> servicio.actualizarContenido(creada.getId(),
                    ContenidoDePrueba.valido().build(), OTRO))
                    .isInstanceOf(AccesoDenegadoException.class);
        }

        @Test
        @DisplayName("fuera del estado Borrador no se puede modificar")
        void soloEnBorrador() {
            Question pendiente = pendiente();

            assertThatThrownBy(() -> servicio.actualizarContenido(pendiente.getId(),
                    ContenidoDePrueba.valido().build(), AUTOR))
                    .isInstanceOf(OperacionNoPermitidaException.class)
                    .hasMessageContaining("Borrador");
        }

        @Test
        @DisplayName("aplica la validación estructural y no cambia nada si falla")
        void validaLaEstructura() {
            Question creada = borrador();

            assertThatThrownBy(() -> servicio.actualizarContenido(creada.getId(),
                    ContenidoDePrueba.valido().contexto("").build(), AUTOR))
                    .isInstanceOf(QuestionValidationException.class);
            assertThat(servicio.obtener(creada.getId()).getContexto()).isNotBlank();
        }
    }

    @Nested
    @DisplayName("Enviar a revisión (HU-02)")
    class EnviarARevision {

        @Test
        @DisplayName("valida de nuevo, pide la transición y guarda el estado Pendiente de revisión")
        void pasaAPendiente() {
            Question creada = borrador();
            reloj.avanzar(Duration.ofMinutes(10));

            Question enviada = servicio.enviarARevision(creada.getId(), AUTOR);

            assertThat(enviada.getEstado()).isEqualTo(EstadoPregunta.PENDIENTE_REVISION);
            assertThat(enviada.getEstadoSecuencia()).isEqualTo(2);
            assertThat(enviada.getActualizadaEn()).isEqualTo(INICIO.plus(Duration.ofMinutes(10)));
            assertThat(servicio.obtener(creada.getId()).getEstado()).isEqualTo(EstadoPregunta.PENDIENTE_REVISION);
            assertThat(validador.llamadas()).isEqualTo(2);
            assertThat(ciclo.llamadas()).containsExactly("registrar", "enviar");
        }

        @Test
        @DisplayName("solo el autor puede enviarla y el ciclo de vida no se entera si no lo es")
        void soloElAutor() {
            Question creada = borrador();

            assertThatThrownBy(() -> servicio.enviarARevision(creada.getId(), OTRO)).isInstanceOf(AccesoDenegadoException.class);
            assertThat(ciclo.llamadas()).containsExactly("registrar");
        }

        @Test
        @DisplayName("solo se envía una pregunta en Borrador")
        void soloDesdeBorrador() {
            Question pendiente = pendiente();

            assertThatThrownBy(() -> servicio.enviarARevision(pendiente.getId(), AUTOR))
                    .isInstanceOf(OperacionNoPermitidaException.class);
        }

        @Test
        @DisplayName("si la validación estructural falla, no cambia de estado")
        void validaAlEnviar() {
            Question creada = borrador();
            repositorio.guardar(Question.builder().id(creada.getId()).nombre("n").contexto(" ").enunciado("e")
                    .opciones(creada.getOpciones()).respuestaCorrecta('B').justificacion("j").bibliografia("b")
                    .estado(EstadoPregunta.BORRADOR).competencia(Competencia.INGLES).tema("t").subtema("s")
                    .dificultad(creada.getDificultad()).autor(AUTOR).creadaEn(INICIO).actualizadaEn(INICIO).build());

            assertThatThrownBy(() -> servicio.enviarARevision(creada.getId(), AUTOR)).isInstanceOf(QuestionValidationException.class);
            assertThat(servicio.obtener(creada.getId()).getEstado()).isEqualTo(EstadoPregunta.BORRADOR);
            assertThat(ciclo.llamadas()).containsExactly("registrar");
        }

        @Test
        @DisplayName("si el ciclo de vida está caído, la pregunta sigue en Borrador")
        void cicloCaido() {
            Question creada = borrador();
            ciclo.caer();

            assertThatThrownBy(() -> servicio.enviarARevision(creada.getId(), AUTOR)).isInstanceOf(DependenciaNoDisponibleException.class);
            assertThat(servicio.obtener(creada.getId()).getEstado()).isEqualTo(EstadoPregunta.BORRADOR);
        }

        @Test
        @DisplayName("una pregunta que no existe lanza QuestionNotFoundException")
        void noExistente() {
            assertThatThrownBy(() -> servicio.enviarARevision("no-existe", AUTOR)).isInstanceOf(QuestionNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("Reacción a question.state-changed")
    class AlCambiarElEstado {

        @Test
        @DisplayName("la copia toma el estado nuevo, con su versión y su secuencia")
        void actualizaLaCopia() {
            Question enRevision = enEstado(pendiente(), EstadoPregunta.EN_REVISION);

            assertThat(enRevision.getEstado()).isEqualTo(EstadoPregunta.EN_REVISION);
            assertThat(enRevision.getEstadoSecuencia()).isEqualTo(3);
        }

        @Test
        @DisplayName("un aviso repetido o atrasado no hace retroceder la copia")
        void ignoraLoViejo() {
            Question pendiente = pendiente();
            Question aprobada = enEstado(enEstado(pendiente, EstadoPregunta.EN_REVISION), EstadoPregunta.APROBADA);

            servicio.alCambiarElEstado(pendiente.getId(), EstadoPregunta.EN_REVISION, 1, 3);
            servicio.alCambiarElEstado(pendiente.getId(), EstadoPregunta.APROBADA, 1, 4);

            assertThat(servicio.obtener(aprobada.getId()).getEstado()).isEqualTo(EstadoPregunta.APROBADA);
            assertThat(servicio.obtener(aprobada.getId()).getEstadoSecuencia()).isEqualTo(4);
        }

        @Test
        @DisplayName("un aviso de una pregunta que no se conoce se ignora sin fallar")
        void preguntaDesconocida() {
            servicio.alCambiarElEstado("no-existe", EstadoPregunta.APROBADA, 1, 4);

            assertThat(repositorio.cantidad()).isZero();
        }

        @Test
        @DisplayName("una pregunta rechazada deja de ser editable y solo vuelve a serlo al reabrirla")
        void editabilidad() {
            Question rechazada = rechazada();

            assertThat(rechazada.esEditable()).isFalse();
            assertThatThrownBy(() -> servicio.actualizarContenido(rechazada.getId(), ContenidoDePrueba.valido().build(), AUTOR))
                    .isInstanceOf(OperacionNoPermitidaException.class);
            assertThat(servicio.reabrirParaCorregir(rechazada.getId(), AUTOR).esEditable()).isTrue();
        }
    }

    @Nested
    @DisplayName("Reabrir una pregunta rechazada")
    class Reabrir {

        @Test
        @DisplayName("vuelve a Borrador como una versión nueva, con la secuencia que informó el ciclo de vida")
        void vuelveABorradorComoVersionNueva() {
            Question rechazada = rechazada();

            Question reabierta = servicio.reabrirParaCorregir(rechazada.getId(), AUTOR);

            assertThat(reabierta.getEstado()).isEqualTo(EstadoPregunta.BORRADOR);
            assertThat(reabierta.getVersion()).isEqualTo(2);
            assertThat(reabierta.getEstadoSecuencia()).isEqualTo(5);
            assertThat(servicio.obtener(rechazada.getId()).getVersion()).isEqualTo(2);
        }

        @Test
        @DisplayName("solo el autor puede reabrirla")
        void soloElAutor() {
            Question rechazada = rechazada();

            assertThatThrownBy(() -> servicio.reabrirParaCorregir(rechazada.getId(), OTRO)).isInstanceOf(AccesoDenegadoException.class);
            assertThat(servicio.obtener(rechazada.getId()).getEstado()).isEqualTo(EstadoPregunta.RECHAZADA);
        }

        @Test
        @DisplayName("solo se reabre una pregunta Rechazada")
        void soloDesdeRechazada() {
            Question creada = borrador();

            assertThatThrownBy(() -> servicio.reabrirParaCorregir(creada.getId(), AUTOR)).isInstanceOf(OperacionNoPermitidaException.class);
        }

        @Test
        @DisplayName("después de reabrirla se puede editar y reenviar a revisión; cada reapertura sube la versión")
        void sePuedeEditarYReenviar() {
            Question rechazada = rechazada();
            servicio.reabrirParaCorregir(rechazada.getId(), AUTOR);
            servicio.actualizarContenido(rechazada.getId(), ContenidoDePrueba.valido().nombre("Corregida").build(), AUTOR);
            servicio.enviarARevision(rechazada.getId(), AUTOR);
            enEstado(enEstado(servicio.obtener(rechazada.getId()), EstadoPregunta.EN_REVISION), EstadoPregunta.RECHAZADA);

            Question segunda = servicio.reabrirParaCorregir(rechazada.getId(), AUTOR);

            assertThat(segunda.getVersion()).isEqualTo(3);
            assertThat(segunda.getNombre()).isEqualTo("Corregida");
        }
    }

    @Nested
    @DisplayName("Publicar una pregunta aprobada (administrador)")
    class Publicar {

        @Test
        @DisplayName("pasa de Aprobada a Publicada")
        void publica() {
            Question aprobada = aprobada();

            Question publicada = servicio.publicar(aprobada.getId());

            assertThat(publicada.getEstado()).isEqualTo(EstadoPregunta.PUBLICADA);
            assertThat(servicio.obtener(aprobada.getId()).getEstado()).isEqualTo(EstadoPregunta.PUBLICADA);
        }

        @Test
        @DisplayName("solo se publica una pregunta Aprobada, y una sola vez")
        void soloDesdeAprobada() {
            Question pendiente = pendiente();
            assertThatThrownBy(() -> servicio.publicar(pendiente.getId())).isInstanceOf(OperacionNoPermitidaException.class);

            Question aprobada = enEstado(enEstado(pendiente, EstadoPregunta.EN_REVISION), EstadoPregunta.APROBADA);
            servicio.publicar(aprobada.getId());
            assertThatThrownBy(() -> servicio.publicar(aprobada.getId())).isInstanceOf(OperacionNoPermitidaException.class);
        }

        @Test
        @DisplayName("una pregunta que no existe lanza QuestionNotFoundException")
        void noExistente() {
            assertThatThrownBy(() -> servicio.publicar("no-existe")).isInstanceOf(QuestionNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("Búsqueda del administrador sobre todos los autores")
    class BuscarTodas {

        @Test
        @DisplayName("incluye las preguntas de todos los autores y filtra por estado")
        void todosLosAutores() {
            Question mia = borrador();
            Question ajena = servicio.crearBorrador(ContenidoDePrueba.valido().build(), OTRO);
            servicio.enviarARevision(ajena.getId(), OTRO);

            Pagina<Question> todas = servicio.buscarTodas(null, FiltroPreguntas.sinFiltros(), 1, 10);
            Pagina<Question> pendientes = servicio.buscarTodas("  ",
                    new FiltroPreguntas(EstadoPregunta.PENDIENTE_REVISION, null, null), 1, 10);

            assertThat(todas.elementos()).extracting(Question::getId).containsExactlyInAnyOrder(mia.getId(), ajena.getId());
            assertThat(pendientes.elementos()).extracting(Question::getId).containsExactly(ajena.getId());
        }

        @Test
        @DisplayName("puede restringirse a un autor")
        void porAutor() {
            Question mia = borrador();
            servicio.crearBorrador(ContenidoDePrueba.valido().build(), OTRO);

            Pagina<Question> resultado = servicio.buscarTodas(AUTOR, null, 1, 10);

            assertThat(resultado.elementos()).extracting(Question::getId).containsExactly(mia.getId());
        }
    }
}
