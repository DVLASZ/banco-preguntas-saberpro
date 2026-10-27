package co.unicauca.bancopreguntas.lifecycle.application;

import co.unicauca.bancopreguntas.lifecycle.domain.AccesoDenegadoException;
import co.unicauca.bancopreguntas.lifecycle.domain.CambioEstado;
import co.unicauca.bancopreguntas.lifecycle.domain.CicloDeVida;
import co.unicauca.bancopreguntas.lifecycle.domain.ContenidoPublicado;
import co.unicauca.bancopreguntas.lifecycle.domain.EstadoPregunta;
import co.unicauca.bancopreguntas.lifecycle.domain.OperacionNoPermitidaException;
import co.unicauca.bancopreguntas.lifecycle.domain.PreguntaNoRegistradaException;
import co.unicauca.bancopreguntas.lifecycle.domain.event.DomainEvent;
import co.unicauca.bancopreguntas.lifecycle.domain.event.QuestionPublished;
import co.unicauca.bancopreguntas.lifecycle.domain.event.QuestionReopened;
import co.unicauca.bancopreguntas.lifecycle.domain.event.QuestionStateChanged;
import co.unicauca.bancopreguntas.lifecycle.domain.event.QuestionSubmitted;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("LifecycleApplicationService")
class LifecycleApplicationServiceTest {

    private static final String AUTOR = "autor-1";
    private static final String OTRO = "autor-2";
    private static final Instant INICIO = Instant.parse("2026-10-08T10:00:00Z");
    private static final ContenidoPublicado CONTENIDO = new ContenidoPublicado("Observer", "LECTURA_CRITICA", "Patrones",
            "Observer", "INTERMEDIO", "Contexto", "¿Qué patrón notifica?", "Factory", "Observer", "Singleton", "Adapter", "B");

    private RelojManual reloj;
    private InMemoryLifecycleRepository repositorio;
    private RecordingEventPublisher eventos;
    private LifecycleApplicationService servicio;

    @BeforeEach
    void preparar() {
        reloj = new RelojManual(INICIO);
        repositorio = new InMemoryLifecycleRepository();
        eventos = new RecordingEventPublisher();
        servicio = new LifecycleApplicationService(repositorio, eventos, new InMemoryProcessedEvents(), reloj);
    }

    private CicloDeVida registrada() {
        return servicio.registrar("Q-1", AUTOR);
    }

    private CicloDeVida pendiente() {
        registrada();
        return servicio.enviarARevision("Q-1", AUTOR, "Observer", "LECTURA_CRITICA");
    }

    private void enRevision() {
        pendiente();
        servicio.aplicarRevisoresAsignados("e-asignados", "Q-1");
    }

    private void aprobada() {
        enRevision();
        servicio.aplicarRevisionCompletada("e-completada", "Q-1", true);
    }

    private void rechazada() {
        enRevision();
        servicio.aplicarRevisionCompletada("e-completada", "Q-1", false);
    }

    @Nested
    @DisplayName("Registrar una pregunta nueva (HU-01)")
    class Registrar {

        @Test
        @DisplayName("queda en Borrador, versión 1, con la creación en el historial y sin eventos")
        void quedaEnBorrador() {
            CicloDeVida ciclo = registrada();

            assertThat(ciclo.getEstado()).isEqualTo(EstadoPregunta.BORRADOR);
            assertThat(ciclo.getVersion()).isEqualTo(1);
            assertThat(servicio.historial("Q-1")).singleElement().satisfies(c -> {
                assertThat(c.desde()).isNull();
                assertThat(c.hacia()).isEqualTo(EstadoPregunta.BORRADOR);
                assertThat(c.cambiadoPor()).isEqualTo(AUTOR);
                assertThat(c.fecha()).isEqualTo(INICIO);
            });
            assertThat(eventos.publicados()).isEmpty();
        }

        @Test
        @DisplayName("registrar dos veces para el mismo autor no duplica nada")
        void esIdempotente() {
            registrada();
            reloj.avanzar(Duration.ofMinutes(5));

            CicloDeVida otra = servicio.registrar("Q-1", AUTOR);

            assertThat(otra.getActualizadoEn()).isEqualTo(INICIO);
            assertThat(servicio.historial("Q-1")).hasSize(1);
        }

        @Test
        @DisplayName("una pregunta ya registrada a nombre de otro autor se rechaza")
        void otroAutor() {
            registrada();

            assertThatThrownBy(() -> servicio.registrar("Q-1", OTRO)).isInstanceOf(OperacionNoPermitidaException.class);
        }

        @Test
        @DisplayName("una pregunta que no se registró lanza PreguntaNoRegistradaException")
        void noExistente() {
            assertThatThrownBy(() -> servicio.obtener("Q-X")).isInstanceOf(PreguntaNoRegistradaException.class);
            assertThatThrownBy(() -> servicio.historial("Q-X")).isInstanceOf(PreguntaNoRegistradaException.class);
        }
    }

    @Nested
    @DisplayName("Enviar a revisión (HU-02)")
    class Enviar {

        @Test
        @DisplayName("pasa a Pendiente de revisión y publica question.state-changed y question.submitted")
        void pasaAPendienteYPublica() {
            registrada();
            reloj.avanzar(Duration.ofMinutes(10));

            CicloDeVida enviada = servicio.enviarARevision("Q-1", AUTOR, "Observer", "LECTURA_CRITICA");

            assertThat(enviada.getEstado()).isEqualTo(EstadoPregunta.PENDIENTE_REVISION);
            assertThat(eventos.tipos()).containsExactly("question.state-changed", "question.submitted");
            assertThat(eventos.publicados().get(0)).isEqualTo(new QuestionStateChanged("Q-1", "BORRADOR",
                    "PENDIENTE_REVISION", AUTOR, 2, 1));
            assertThat(eventos.publicados().get(1)).isEqualTo(new QuestionSubmitted("Q-1", AUTOR, "Observer",
                    "LECTURA_CRITICA", 1));
        }

        @Test
        @DisplayName("registra el cambio en el historial con la fecha en que ocurrió")
        void registraElCambio() {
            registrada();
            reloj.avanzar(Duration.ofMinutes(10));

            servicio.enviarARevision("Q-1", AUTOR, "Observer", "LECTURA_CRITICA");

            List<CambioEstado> historial = servicio.historial("Q-1");
            assertThat(historial).hasSize(2);
            assertThat(historial.get(1).desde()).isEqualTo(EstadoPregunta.BORRADOR);
            assertThat(historial.get(1).hacia()).isEqualTo(EstadoPregunta.PENDIENTE_REVISION);
            assertThat(historial.get(1).cambiadoPor()).isEqualTo(AUTOR);
            assertThat(historial.get(1).fecha()).isEqualTo(INICIO.plus(Duration.ofMinutes(10)));
        }

        @Test
        @DisplayName("solo el autor puede enviarla")
        void soloElAutor() {
            registrada();

            assertThatThrownBy(() -> servicio.enviarARevision("Q-1", OTRO, "t", "c")).isInstanceOf(AccesoDenegadoException.class);
            assertThat(servicio.obtener("Q-1").getEstado()).isEqualTo(EstadoPregunta.BORRADOR);
            assertThat(eventos.publicados()).isEmpty();
        }

        @Test
        @DisplayName("solo se envía una pregunta en Borrador")
        void soloDesdeBorrador() {
            pendiente();

            assertThatThrownBy(() -> servicio.enviarARevision("Q-1", AUTOR, "t", "c")).isInstanceOf(OperacionNoPermitidaException.class);
        }

        @Test
        @DisplayName("una pregunta que no existe lanza PreguntaNoRegistradaException")
        void noExistente() {
            assertThatThrownBy(() -> servicio.enviarARevision("Q-X", AUTOR, "t", "c")).isInstanceOf(PreguntaNoRegistradaException.class);
        }
    }

    @Nested
    @DisplayName("Reacción a review.reviewers-assigned")
    class AlAsignarRevisores {

        @Test
        @DisplayName("la pregunta pasa de Pendiente de revisión a En revisión, sin usuario responsable")
        void pasaAEnRevision() {
            pendiente();

            servicio.aplicarRevisoresAsignados("e-1", "Q-1");

            assertThat(servicio.obtener("Q-1").getEstado()).isEqualTo(EstadoPregunta.EN_REVISION);
            CambioEstado ultimo = servicio.historial("Q-1").get(2);
            assertThat(ultimo.hacia()).isEqualTo(EstadoPregunta.EN_REVISION);
            assertThat(ultimo.cambiadoPor()).isNull();
            assertThat(eventos.publicados().get(eventos.publicados().size() - 1))
                    .isEqualTo(new QuestionStateChanged("Q-1", "PENDIENTE_REVISION", "EN_REVISION", null, 3, 1));
        }

        @Test
        @DisplayName("es idempotente: un evento ya procesado no vuelve a aplicarse")
        void esIdempotente() {
            pendiente();
            servicio.aplicarRevisoresAsignados("e-1", "Q-1");
            int publicados = eventos.publicados().size();

            servicio.aplicarRevisoresAsignados("e-1", "Q-1");

            assertThat(eventos.publicados()).hasSize(publicados);
            assertThat(servicio.historial("Q-1")).hasSize(3);
        }

        @Test
        @DisplayName("si la pregunta no está pendiente, el evento se ignora")
        void ignoraUnEstadoInesperado() {
            registrada();

            servicio.aplicarRevisoresAsignados("e-1", "Q-1");

            assertThat(servicio.obtener("Q-1").getEstado()).isEqualTo(EstadoPregunta.BORRADOR);
            assertThat(eventos.publicados()).isEmpty();
        }

        @Test
        @DisplayName("si la pregunta no existe, el evento se ignora sin fallar")
        void ignoraUnaPreguntaInexistente() {
            servicio.aplicarRevisoresAsignados("e-1", "Q-X");

            assertThat(eventos.publicados()).isEmpty();
        }
    }

    @Nested
    @DisplayName("Reacción a review.completed")
    class AlCompletarseLaRevision {

        @Test
        @DisplayName("aprobada: de En revisión a Aprobada")
        void aprobada() {
            enRevision();

            servicio.aplicarRevisionCompletada("e-2", "Q-1", true);

            assertThat(servicio.obtener("Q-1").getEstado()).isEqualTo(EstadoPregunta.APROBADA);
        }

        @Test
        @DisplayName("rechazada: de En revisión a Rechazada")
        void rechazada() {
            enRevision();

            servicio.aplicarRevisionCompletada("e-2", "Q-1", false);

            assertThat(servicio.obtener("Q-1").getEstado()).isEqualTo(EstadoPregunta.RECHAZADA);
        }

        @Test
        @DisplayName("solo aplica si la pregunta está En revisión")
        void soloDesdeEnRevision() {
            pendiente();

            servicio.aplicarRevisionCompletada("e-2", "Q-1", true);

            assertThat(servicio.obtener("Q-1").getEstado()).isEqualTo(EstadoPregunta.PENDIENTE_REVISION);
        }

        @Test
        @DisplayName("es idempotente: el mismo evento no se aplica dos veces")
        void esIdempotente() {
            enRevision();
            servicio.aplicarRevisionCompletada("e-2", "Q-1", true);
            int publicados = eventos.publicados().size();

            servicio.aplicarRevisionCompletada("e-2", "Q-1", false);

            assertThat(servicio.obtener("Q-1").getEstado()).isEqualTo(EstadoPregunta.APROBADA);
            assertThat(eventos.publicados()).hasSize(publicados);
        }
    }

    @Nested
    @DisplayName("Reabrir una pregunta rechazada")
    class Reabrir {

        @Test
        @DisplayName("vuelve a Borrador como una versión nueva")
        void vuelveABorradorComoVersionNueva() {
            rechazada();

            CicloDeVida reabierta = servicio.reabrir("Q-1", AUTOR);

            assertThat(reabierta.getEstado()).isEqualTo(EstadoPregunta.BORRADOR);
            assertThat(reabierta.getVersion()).isEqualTo(2);
            assertThat(reabierta.esEditable()).isTrue();
        }

        @Test
        @DisplayName("publica question.state-changed y question.reopened con la versión nueva")
        void publicaLosEventos() {
            rechazada();
            int antes = eventos.publicados().size();

            servicio.reabrir("Q-1", AUTOR);

            List<DomainEvent> nuevos = eventos.publicados().subList(antes, eventos.publicados().size());
            assertThat(nuevos).containsExactly(
                    new QuestionStateChanged("Q-1", "RECHAZADA", "BORRADOR", AUTOR, 5, 2),
                    new QuestionReopened("Q-1", AUTOR, 2));
        }

        @Test
        @DisplayName("solo el autor puede reabrirla")
        void soloElAutor() {
            rechazada();

            assertThatThrownBy(() -> servicio.reabrir("Q-1", OTRO)).isInstanceOf(AccesoDenegadoException.class);
            assertThat(servicio.obtener("Q-1").getEstado()).isEqualTo(EstadoPregunta.RECHAZADA);
        }

        @Test
        @DisplayName("solo se reabre una pregunta Rechazada")
        void soloDesdeRechazada() {
            registrada();

            assertThatThrownBy(() -> servicio.reabrir("Q-1", AUTOR)).isInstanceOf(OperacionNoPermitidaException.class);
        }

        @Test
        @DisplayName("cada reapertura sube la versión y se puede reenviar a revisión con ella")
        void cadaReaperturaSubeLaVersion() {
            rechazada();
            servicio.reabrir("Q-1", AUTOR);
            servicio.enviarARevision("Q-1", AUTOR, "Observer", "LECTURA_CRITICA");
            servicio.aplicarRevisoresAsignados("e-a2", "Q-1");
            servicio.aplicarRevisionCompletada("e-c2", "Q-1", false);

            CicloDeVida segunda = servicio.reabrir("Q-1", AUTOR);

            assertThat(segunda.getVersion()).isEqualTo(3);
            assertThat(eventos.publicados()).contains(new QuestionSubmitted("Q-1", AUTOR, "Observer", "LECTURA_CRITICA", 2));
        }
    }

    @Nested
    @DisplayName("Publicar una pregunta aprobada (administrador)")
    class Publicar {

        @Test
        @DisplayName("pasa de Aprobada a Publicada y lo registra en el historial con el administrador")
        void publica() {
            aprobada();

            CicloDeVida publicada = servicio.publicar("Q-1", "admin-1", CONTENIDO);

            assertThat(publicada.getEstado()).isEqualTo(EstadoPregunta.PUBLICADA);
            CambioEstado ultimo = servicio.historial("Q-1").get(servicio.historial("Q-1").size() - 1);
            assertThat(ultimo.hacia()).isEqualTo(EstadoPregunta.PUBLICADA);
            assertThat(ultimo.cambiadoPor()).isEqualTo("admin-1");
        }

        @Test
        @DisplayName("publica question.state-changed y question.published con el contenido recibido")
        void publicaLosEventos() {
            aprobada();
            int antes = eventos.publicados().size();

            servicio.publicar("Q-1", "admin-1", CONTENIDO);

            List<DomainEvent> nuevos = eventos.publicados().subList(antes, eventos.publicados().size());
            assertThat(nuevos).containsExactly(
                    new QuestionStateChanged("Q-1", "APROBADA", "PUBLICADA", "admin-1", 5, 1),
                    QuestionPublished.de("Q-1", 1, "admin-1", CONTENIDO));
        }

        @Test
        @DisplayName("solo se publica una pregunta Aprobada, y una sola vez")
        void soloDesdeAprobada() {
            pendiente();
            assertThatThrownBy(() -> servicio.publicar("Q-1", "admin-1", CONTENIDO)).isInstanceOf(OperacionNoPermitidaException.class);

            repositorio.guardar(servicio.obtener("Q-1").iniciarRevision(INICIO).aprobar(INICIO));
            servicio.publicar("Q-1", "admin-1", CONTENIDO);
            assertThatThrownBy(() -> servicio.publicar("Q-1", "admin-1", CONTENIDO)).isInstanceOf(OperacionNoPermitidaException.class);
        }

        @Test
        @DisplayName("una pregunta que no existe lanza PreguntaNoRegistradaException")
        void noExistente() {
            assertThatThrownBy(() -> servicio.publicar("Q-X", "admin-1", CONTENIDO)).isInstanceOf(PreguntaNoRegistradaException.class);
        }
    }
}
