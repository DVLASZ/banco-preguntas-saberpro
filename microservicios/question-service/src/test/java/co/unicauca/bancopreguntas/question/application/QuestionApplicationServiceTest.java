package co.unicauca.bancopreguntas.question.application;

import co.unicauca.bancopreguntas.question.domain.AccesoDenegadoException;
import co.unicauca.bancopreguntas.question.domain.CambioEstado;
import co.unicauca.bancopreguntas.question.domain.Competencia;
import co.unicauca.bancopreguntas.question.domain.EstadoPregunta;
import co.unicauca.bancopreguntas.question.domain.FiltroPreguntas;
import co.unicauca.bancopreguntas.question.domain.OperacionNoPermitidaException;
import co.unicauca.bancopreguntas.question.domain.Pagina;
import co.unicauca.bancopreguntas.question.domain.Question;
import co.unicauca.bancopreguntas.question.domain.QuestionNotFoundException;
import co.unicauca.bancopreguntas.question.domain.event.QuestionPublished;
import co.unicauca.bancopreguntas.question.domain.event.QuestionReopened;
import co.unicauca.bancopreguntas.question.domain.event.QuestionStateChanged;
import co.unicauca.bancopreguntas.question.domain.event.QuestionSubmitted;
import co.unicauca.bancopreguntas.question.domain.validation.ContenidoDePrueba;
import co.unicauca.bancopreguntas.question.domain.validation.QuestionValidationException;
import co.unicauca.bancopreguntas.question.domain.validation.QuestionValidator;
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
    private RecordingEventPublisher eventos;
    private QuestionApplicationService servicio;

    @BeforeEach
    void preparar() {
        reloj = new RelojManual(INICIO);
        repositorio = new InMemoryQuestionRepository();
        eventos = new RecordingEventPublisher();
        AtomicInteger consecutivo = new AtomicInteger();
        servicio = new QuestionApplicationService(repositorio, eventos, new InMemoryProcessedEvents(),
                QuestionValidator.porDefecto(), reloj, () -> "Q-" + consecutivo.incrementAndGet());
    }

    private Question borrador() {
        return servicio.crearBorrador(ContenidoDePrueba.valido().build(), AUTOR);
    }

    private Question pendiente() {
        Question creada = borrador();
        servicio.enviarARevision(creada.getId(), AUTOR);
        eventos = new RecordingEventPublisher();
        servicio = nuevoServicio(eventos);
        return servicio.obtener(creada.getId());
    }

    /** Un servicio nuevo sobre el mismo repositorio, para empezar a contar eventos desde cero. */
    private QuestionApplicationService nuevoServicio(RecordingEventPublisher nuevosEventos) {
        AtomicInteger consecutivo = new AtomicInteger(100);
        return new QuestionApplicationService(repositorio, nuevosEventos, new InMemoryProcessedEvents(),
                QuestionValidator.porDefecto(), reloj, () -> "Q-" + consecutivo.incrementAndGet());
    }

    @Nested
    @DisplayName("Crear un borrador (HU-01)")
    class CrearBorrador {

        @Test
        @DisplayName("guarda la pregunta en Borrador, con su autor y sus fechas")
        void guardaEnBorrador() {
            Question creada = borrador();

            assertThat(creada.getId()).isEqualTo("Q-1");
            assertThat(creada.getEstado()).isEqualTo(EstadoPregunta.BORRADOR);
            assertThat(creada.getAutor()).isEqualTo(AUTOR);
            assertThat(creada.getCreadaEn()).isEqualTo(INICIO);
            assertThat(creada.getActualizadaEn()).isEqualTo(INICIO);
            assertThat(servicio.obtener("Q-1").getNombre()).isEqualTo("Patrón Observer");
        }

        @Test
        @DisplayName("registra la creación en el historial y no publica eventos")
        void registraLaCreacion() {
            borrador();

            List<CambioEstado> historial = servicio.historial("Q-1");

            assertThat(historial).containsExactly(new CambioEstado(null, EstadoPregunta.BORRADOR, AUTOR, INICIO));
            assertThat(eventos.publicados()).isEmpty();
        }

        @Test
        @DisplayName("no guarda nada si incumple la validación estructural y reporta todos los campos")
        void rechazaContenidoInvalido() {
            var invalido = ContenidoDePrueba.valido().contexto("").opcionB("Factory").build();

            assertThatThrownBy(() -> servicio.crearBorrador(invalido, AUTOR))
                    .isInstanceOf(QuestionValidationException.class)
                    .satisfies(e -> assertThat(((QuestionValidationException) e).getViolaciones()).hasSizeGreaterThan(1));
            assertThat(repositorio.cantidad()).isZero();
        }

        @Test
        @DisplayName("exige un autor")
        void exigeAutor() {
            var contenido = ContenidoDePrueba.valido().build();

            assertThatThrownBy(() -> servicio.crearBorrador(contenido, " "))
                    .isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> servicio.crearBorrador(contenido, null))
                    .isInstanceOf(IllegalArgumentException.class);
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
            assertThatThrownBy(() -> servicio.historial("no-existe"))
                    .isInstanceOf(QuestionNotFoundException.class);
        }

        @Test
        @DisplayName("el historial queda en orden cronológico")
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
        @DisplayName("pasa a Pendiente de revisión y publica question.submitted y question.state-changed")
        void pasaAPendienteYPublica() {
            Question creada = borrador();
            reloj.avanzar(Duration.ofMinutes(10));

            Question enviada = servicio.enviarARevision(creada.getId(), AUTOR);

            assertThat(enviada.getEstado()).isEqualTo(EstadoPregunta.PENDIENTE_REVISION);
            assertThat(enviada.getActualizadaEn()).isEqualTo(INICIO.plus(Duration.ofMinutes(10)));
            assertThat(eventos.tipos()).containsExactly("question.state-changed", "question.submitted");
            assertThat(eventos.publicados()).contains(
                    new QuestionSubmitted(creada.getId(), AUTOR, "Patrón Observer", "LECTURA_CRITICA", 1),
                    new QuestionStateChanged(creada.getId(), "BORRADOR", "PENDIENTE_REVISION", AUTOR));
        }

        @Test
        @DisplayName("registra el cambio en el historial")
        void registraElCambio() {
            Question creada = borrador();

            servicio.enviarARevision(creada.getId(), AUTOR);

            assertThat(servicio.historial(creada.getId())).extracting(CambioEstado::hacia)
                    .containsExactly(EstadoPregunta.BORRADOR, EstadoPregunta.PENDIENTE_REVISION);
        }

        @Test
        @DisplayName("solo el autor puede enviarla")
        void soloElAutor() {
            Question creada = borrador();

            assertThatThrownBy(() -> servicio.enviarARevision(creada.getId(), OTRO))
                    .isInstanceOf(AccesoDenegadoException.class);
            assertThat(eventos.publicados()).isEmpty();
        }

        @Test
        @DisplayName("solo se envía una pregunta en Borrador")
        void soloDesdeBorrador() {
            Question pendiente = pendiente();

            assertThatThrownBy(() -> servicio.enviarARevision(pendiente.getId(), AUTOR))
                    .isInstanceOf(OperacionNoPermitidaException.class);
            assertThat(eventos.publicados()).isEmpty();
        }

        @Test
        @DisplayName("una pregunta que no existe lanza QuestionNotFoundException")
        void noExistente() {
            assertThatThrownBy(() -> servicio.enviarARevision("no-existe", AUTOR))
                    .isInstanceOf(QuestionNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("Reacción a review.reviewers-assigned")
    class RevisoresAsignados {

        @Test
        @DisplayName("la pregunta pasa de Pendiente de revisión a En revisión, sin usuario responsable")
        void pasaAEnRevision() {
            Question pendiente = pendiente();

            servicio.aplicarRevisoresAsignados("evento-1", pendiente.getId());

            assertThat(servicio.obtener(pendiente.getId()).getEstado()).isEqualTo(EstadoPregunta.EN_REVISION);
            CambioEstado ultimo = servicio.historial(pendiente.getId()).getLast();
            assertThat(ultimo.desde()).isEqualTo(EstadoPregunta.PENDIENTE_REVISION);
            assertThat(ultimo.hacia()).isEqualTo(EstadoPregunta.EN_REVISION);
            assertThat(ultimo.cambiadoPor()).isNull();
            assertThat(eventos.publicados()).containsExactly(
                    new QuestionStateChanged(pendiente.getId(), "PENDIENTE_REVISION", "EN_REVISION", null));
        }

        @Test
        @DisplayName("es idempotente: un evento ya procesado no vuelve a aplicarse")
        void esIdempotente() {
            Question primera = pendiente();
            Question segunda = servicio.crearBorrador(ContenidoDePrueba.valido().build(), AUTOR);
            servicio.enviarARevision(segunda.getId(), AUTOR);

            servicio.aplicarRevisoresAsignados("evento-1", primera.getId());
            servicio.aplicarRevisoresAsignados("evento-1", segunda.getId());

            assertThat(servicio.obtener(primera.getId()).getEstado()).isEqualTo(EstadoPregunta.EN_REVISION);
            assertThat(servicio.obtener(segunda.getId()).getEstado()).isEqualTo(EstadoPregunta.PENDIENTE_REVISION);
        }

        @Test
        @DisplayName("si la pregunta no está pendiente, el evento se ignora")
        void ignoraUnEstadoInesperado() {
            Question creada = borrador();

            servicio.aplicarRevisoresAsignados("evento-1", creada.getId());

            assertThat(servicio.obtener(creada.getId()).getEstado()).isEqualTo(EstadoPregunta.BORRADOR);
            assertThat(eventos.publicados()).isEmpty();
        }

        @Test
        @DisplayName("si la pregunta no existe, el evento se ignora sin fallar")
        void ignoraUnaPreguntaInexistente() {
            servicio.aplicarRevisoresAsignados("evento-1", "no-existe");

            assertThat(eventos.publicados()).isEmpty();
        }
    }

    @Nested
    @DisplayName("Reacción a review.completed")
    class RevisionCompletada {

        private Question enRevision() {
            Question pendiente = pendiente();
            servicio.aplicarRevisoresAsignados("asignacion", pendiente.getId());
            return servicio.obtener(pendiente.getId());
        }

        @Test
        @DisplayName("aprobada: de En revisión a Aprobada")
        void aprobada() {
            Question pregunta = enRevision();

            servicio.aplicarRevisionCompletada("evento-2", pregunta.getId(), true);

            assertThat(servicio.obtener(pregunta.getId()).getEstado()).isEqualTo(EstadoPregunta.APROBADA);
            assertThat(eventos.publicados()).contains(
                    new QuestionStateChanged(pregunta.getId(), "EN_REVISION", "APROBADA", null));
        }

        @Test
        @DisplayName("rechazada: de En revisión a Rechazada")
        void rechazada() {
            Question pregunta = enRevision();

            servicio.aplicarRevisionCompletada("evento-2", pregunta.getId(), false);

            assertThat(servicio.obtener(pregunta.getId()).getEstado()).isEqualTo(EstadoPregunta.RECHAZADA);
        }

        @Test
        @DisplayName("la pregunta rechazada puede volver a Borrador para corregirse")
        void rechazadaVuelveABorrador() {
            Question pregunta = enRevision();
            servicio.aplicarRevisionCompletada("evento-2", pregunta.getId(), false);

            Question rechazada = servicio.obtener(pregunta.getId());

            assertThat(rechazada.getEstado().puedePasarA(EstadoPregunta.BORRADOR)).isTrue();
        }

        @Test
        @DisplayName("solo aplica si la pregunta está En revisión")
        void soloDesdeEnRevision() {
            Question pendiente = pendiente();

            servicio.aplicarRevisionCompletada("evento-2", pendiente.getId(), true);

            assertThat(servicio.obtener(pendiente.getId()).getEstado()).isEqualTo(EstadoPregunta.PENDIENTE_REVISION);
        }

        @Test
        @DisplayName("es idempotente: el mismo evento no se aplica dos veces")
        void esIdempotente() {
            Question pregunta = enRevision();

            servicio.aplicarRevisionCompletada("evento-2", pregunta.getId(), true);
            long publicadosAntes = eventos.publicados().size();
            servicio.aplicarRevisionCompletada("evento-2", pregunta.getId(), true);

            assertThat(eventos.publicados()).hasSize((int) publicadosAntes);
        }
    }

    @Nested
    @DisplayName("Reabrir una pregunta rechazada")
    class Reabrir {

        private Question rechazada() {
            Question pendiente = pendiente();
            servicio.aplicarRevisoresAsignados("asignada-" + pendiente.getId(), pendiente.getId());
            servicio.aplicarRevisionCompletada("completa-" + pendiente.getId(), pendiente.getId(), false);
            eventos = new RecordingEventPublisher();
            servicio = nuevoServicio(eventos);
            return servicio.obtener(pendiente.getId());
        }

        @Test
        @DisplayName("vuelve a Borrador como una versión nueva")
        void vuelveABorradorComoVersionNueva() {
            Question rechazada = rechazada();
            reloj.avanzar(Duration.ofHours(2));

            Question reabierta = servicio.reabrirParaCorregir(rechazada.getId(), AUTOR);

            assertThat(reabierta.getEstado()).isEqualTo(EstadoPregunta.BORRADOR);
            assertThat(reabierta.getVersion()).isEqualTo(2);
            assertThat(reabierta.getCreadaEn()).isEqualTo(rechazada.getCreadaEn());
            assertThat(reabierta.getActualizadaEn()).isEqualTo(INICIO.plus(Duration.ofHours(2)));
            CambioEstado ultimo = servicio.historial(rechazada.getId()).getLast();
            assertThat(ultimo).isEqualTo(new CambioEstado(EstadoPregunta.RECHAZADA, EstadoPregunta.BORRADOR,
                    AUTOR, INICIO.plus(Duration.ofHours(2))));
        }

        @Test
        @DisplayName("publica question.state-changed y question.reopened con la versión nueva")
        void publicaLosEventos() {
            Question rechazada = rechazada();

            servicio.reabrirParaCorregir(rechazada.getId(), AUTOR);

            assertThat(eventos.tipos()).containsExactly("question.state-changed", "question.reopened");
            assertThat(eventos.publicados()).containsExactly(
                    new QuestionStateChanged(rechazada.getId(), "RECHAZADA", "BORRADOR", AUTOR),
                    new QuestionReopened(rechazada.getId(), AUTOR, 2));
        }

        @Test
        @DisplayName("solo el autor puede reabrirla")
        void soloElAutor() {
            Question rechazada = rechazada();

            assertThatThrownBy(() -> servicio.reabrirParaCorregir(rechazada.getId(), OTRO))
                    .isInstanceOf(AccesoDenegadoException.class);
            assertThat(servicio.obtener(rechazada.getId()).getEstado()).isEqualTo(EstadoPregunta.RECHAZADA);
            assertThat(eventos.publicados()).isEmpty();
        }

        @Test
        @DisplayName("solo se reabre una pregunta Rechazada")
        void soloDesdeRechazada() {
            Question creada = borrador();
            Question pendiente = pendiente();

            assertThatThrownBy(() -> servicio.reabrirParaCorregir(creada.getId(), AUTOR))
                    .isInstanceOf(OperacionNoPermitidaException.class).hasMessageContaining("Rechazada");
            assertThatThrownBy(() -> servicio.reabrirParaCorregir(pendiente.getId(), AUTOR))
                    .isInstanceOf(OperacionNoPermitidaException.class);
            assertThatThrownBy(() -> servicio.reabrirParaCorregir("no-existe", AUTOR))
                    .isInstanceOf(QuestionNotFoundException.class);
        }

        @Test
        @DisplayName("después de reabrirla se puede editar y reenviar a revisión con la versión nueva")
        void sePuedeEditarYReenviar() {
            Question rechazada = rechazada();
            servicio.reabrirParaCorregir(rechazada.getId(), AUTOR);

            Question editada = servicio.actualizarContenido(rechazada.getId(),
                    ContenidoDePrueba.valido().nombre("Versión corregida").build(), AUTOR);
            eventos = new RecordingEventPublisher();
            servicio = nuevoServicio(eventos);
            servicio.enviarARevision(rechazada.getId(), AUTOR);

            assertThat(editada.getNombre()).isEqualTo("Versión corregida");
            assertThat(editada.getVersion()).isEqualTo(2);
            assertThat(eventos.publicados()).contains(
                    new QuestionSubmitted(rechazada.getId(), AUTOR, "Versión corregida", "LECTURA_CRITICA", 2));
        }

        @Test
        @DisplayName("cada reapertura sube la versión")
        void cadaReaperturaSubeLaVersion() {
            Question rechazada = rechazada();
            servicio.reabrirParaCorregir(rechazada.getId(), AUTOR);
            servicio.enviarARevision(rechazada.getId(), AUTOR);
            servicio.aplicarRevisoresAsignados("asignada-2", rechazada.getId());
            servicio.aplicarRevisionCompletada("completa-2", rechazada.getId(), false);

            Question segunda = servicio.reabrirParaCorregir(rechazada.getId(), AUTOR);

            assertThat(segunda.getVersion()).isEqualTo(3);
        }
    }

    @Nested
    @DisplayName("Publicar una pregunta aprobada (administrador)")
    class Publicar {

        private Question aprobada() {
            Question pendiente = pendiente();
            servicio.aplicarRevisoresAsignados("asignada-" + pendiente.getId(), pendiente.getId());
            servicio.aplicarRevisionCompletada("completa-" + pendiente.getId(), pendiente.getId(), true);
            eventos = new RecordingEventPublisher();
            servicio = nuevoServicio(eventos);
            return servicio.obtener(pendiente.getId());
        }

        @Test
        @DisplayName("pasa de Aprobada a Publicada y lo registra en el historial con el administrador")
        void publica() {
            Question aprobada = aprobada();
            reloj.avanzar(Duration.ofDays(1));

            Question publicada = servicio.publicar(aprobada.getId(), "admin-1");

            assertThat(publicada.getEstado()).isEqualTo(EstadoPregunta.PUBLICADA);
            assertThat(servicio.historial(aprobada.getId()).getLast()).isEqualTo(new CambioEstado(
                    EstadoPregunta.APROBADA, EstadoPregunta.PUBLICADA, "admin-1", INICIO.plus(Duration.ofDays(1))));
        }

        @Test
        @DisplayName("publica question.state-changed y question.published")
        void publicaLosEventos() {
            Question aprobada = aprobada();

            servicio.publicar(aprobada.getId(), "admin-1");

            assertThat(eventos.publicados()).containsExactly(
                    new QuestionStateChanged(aprobada.getId(), "APROBADA", "PUBLICADA", "admin-1"),
                    new QuestionPublished(aprobada.getId(), "admin-1"));
        }

        @Test
        @DisplayName("solo se publica una pregunta Aprobada")
        void soloDesdeAprobada() {
            Question creada = borrador();
            Question pendiente = pendiente();

            assertThatThrownBy(() -> servicio.publicar(creada.getId(), "admin-1"))
                    .isInstanceOf(OperacionNoPermitidaException.class).hasMessageContaining("Aprobada");
            assertThatThrownBy(() -> servicio.publicar(pendiente.getId(), "admin-1"))
                    .isInstanceOf(OperacionNoPermitidaException.class);
            assertThat(eventos.publicados()).isEmpty();
        }

        @Test
        @DisplayName("una pregunta ya publicada no se publica otra vez")
        void noSePublicaDosVeces() {
            Question aprobada = aprobada();
            servicio.publicar(aprobada.getId(), "admin-1");

            assertThatThrownBy(() -> servicio.publicar(aprobada.getId(), "admin-1"))
                    .isInstanceOf(OperacionNoPermitidaException.class);
        }

        @Test
        @DisplayName("una pregunta que no existe lanza QuestionNotFoundException")
        void noExistente() {
            assertThatThrownBy(() -> servicio.publicar("no-existe", "admin-1"))
                    .isInstanceOf(QuestionNotFoundException.class);
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
