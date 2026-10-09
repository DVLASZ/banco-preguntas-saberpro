package co.unicauca.bancopreguntas.question.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Question: cambio de estado y marcas de tiempo")
class QuestionTransicionTest {

    private static final Instant CREADA = Instant.parse("2026-10-08T10:00:00Z");
    private static final Instant DESPUES = Instant.parse("2026-10-08T11:30:00Z");

    private static Question borrador() {
        return Question.builder()
                .id("Q-1").nombre("Observer").contexto("Contexto").enunciado("¿Qué patrón notifica?")
                .opciones(new QuestionDistractors("Factory", "Observer", "Singleton", "Adapter"))
                .respuestaCorrecta('B').justificacion("Porque sí").bibliografia("GoF")
                .estado(EstadoPregunta.BORRADOR).competencia(Competencia.LECTURA_CRITICA)
                .tema("Patrones").subtema("Observer").dificultad(Dificultad.BASICO).autor("autor-1")
                .creadaEn(CREADA).actualizadaEn(CREADA)
                .build();
    }

    @Test
    @DisplayName("conEstado devuelve una copia en el nuevo estado y con la fecha de actualización")
    void devuelveUnaCopiaEnElNuevoEstado() {
        Question original = borrador();

        Question enviada = original.conEstado(EstadoPregunta.PENDIENTE_REVISION, DESPUES);

        assertThat(enviada.getEstado()).isEqualTo(EstadoPregunta.PENDIENTE_REVISION);
        assertThat(enviada.getActualizadaEn()).isEqualTo(DESPUES);
        assertThat(enviada.getCreadaEn()).isEqualTo(CREADA);
        assertThat(enviada.getId()).isEqualTo(original.getId());
        assertThat(enviada.getAutor()).isEqualTo("autor-1");
        assertThat(enviada.getOpciones().getOpcionB()).isEqualTo("Observer");
        assertThat(enviada.getRespuestaCorrecta()).isEqualTo('B');
    }

    @Test
    @DisplayName("conEstado no modifica la entidad original")
    void noModificaLaOriginal() {
        Question original = borrador();

        original.conEstado(EstadoPregunta.PENDIENTE_REVISION, DESPUES);

        assertThat(original.getEstado()).isEqualTo(EstadoPregunta.BORRADOR);
        assertThat(original.getActualizadaEn()).isEqualTo(CREADA);
    }

    @Test
    @DisplayName("conEstado rechaza una transición que el ciclo de vida no permite")
    void rechazaTransicionesInvalidas() {
        Question original = borrador();

        assertThatThrownBy(() -> original.conEstado(EstadoPregunta.APROBADA, DESPUES))
                .isInstanceOf(OperacionNoPermitidaException.class)
                .hasMessageContaining("Borrador")
                .hasMessageContaining("Aprobada");
    }

    @Test
    @DisplayName("conEstado permite archivar desde cualquier estado")
    void permiteArchivar() {
        Question archivada = borrador().conEstado(EstadoPregunta.ARCHIVADA, DESPUES);

        assertThat(archivada.getEstado()).isEqualTo(EstadoPregunta.ARCHIVADA);
    }

    @Test
    @DisplayName("Una pregunta sin persistir no tiene marcas de tiempo")
    void sinMarcasDeTiempoMientrasNoSePersiste() {
        Question sinPersistir = new Question("Q-2", "Nombre", "Enunciado",
                new QuestionDistractors("a", "b", "c", "d"), 'A', EstadoPregunta.BORRADOR,
                Competencia.INGLES, "Tema", Dificultad.AVANZADO);

        assertThat(sinPersistir.getCreadaEn()).isNull();
        assertThat(sinPersistir.getActualizadaEn()).isNull();
    }

    private static Question rechazada() {
        return borrador().conEstado(EstadoPregunta.PENDIENTE_REVISION, DESPUES)
                .conEstado(EstadoPregunta.EN_REVISION, DESPUES)
                .conEstado(EstadoPregunta.RECHAZADA, DESPUES);
    }

    @Test
    @DisplayName("una pregunta nueva empieza en la versión 1")
    void versionInicial() {
        assertThat(borrador().getVersion()).isEqualTo(1);
    }

    @Test
    @DisplayName("la versión debe ser al menos 1")
    void versionInvalida() {
        assertThatThrownBy(() -> Question.builder()
                .id("Q-1").nombre("N").enunciado("E")
                .opciones(new QuestionDistractors("a", "b", "c", "d")).respuestaCorrecta('A')
                .estado(EstadoPregunta.BORRADOR).competencia(Competencia.INGLES).tema("T")
                .dificultad(Dificultad.BASICO).version(0).build())
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("versión");
    }

    @Test
    @DisplayName("conEstado conserva la versión")
    void conEstadoConservaLaVersion() {
        Question v3 = Question.builder()
                .id("Q-1").nombre("N").enunciado("E")
                .opciones(new QuestionDistractors("a", "b", "c", "d")).respuestaCorrecta('A')
                .estado(EstadoPregunta.BORRADOR).competencia(Competencia.INGLES).tema("T")
                .dificultad(Dificultad.BASICO).version(3).build();

        assertThat(v3.conEstado(EstadoPregunta.PENDIENTE_REVISION, DESPUES).getVersion()).isEqualTo(3);
    }

    @Test
    @DisplayName("reabrir devuelve la pregunta rechazada a Borrador con la versión siguiente")
    void reabrirSubeLaVersion() {
        Question rechazada = rechazada();

        Question reabierta = rechazada.reabrir(DESPUES.plusSeconds(60));

        assertThat(reabierta.getEstado()).isEqualTo(EstadoPregunta.BORRADOR);
        assertThat(reabierta.getVersion()).isEqualTo(2);
        assertThat(reabierta.getActualizadaEn()).isEqualTo(DESPUES.plusSeconds(60));
        assertThat(reabierta.getCreadaEn()).isEqualTo(CREADA);
        assertThat(reabierta.getId()).isEqualTo("Q-1");
        assertThat(reabierta.getAutor()).isEqualTo("autor-1");
        assertThat(reabierta.getOpciones().getOpcionB()).isEqualTo("Observer");
    }

    @Test
    @DisplayName("reabrir no modifica la entidad original")
    void reabrirNoModificaLaOriginal() {
        Question rechazada = rechazada();

        rechazada.reabrir(DESPUES);

        assertThat(rechazada.getEstado()).isEqualTo(EstadoPregunta.RECHAZADA);
        assertThat(rechazada.getVersion()).isEqualTo(1);
    }

    @ParameterizedTest
    @EnumSource(value = EstadoPregunta.class, names = "RECHAZADA", mode = EnumSource.Mode.EXCLUDE)
    @DisplayName("reabrir solo es posible desde Rechazada")
    void reabrirSoloDesdeRechazada(EstadoPregunta estado) {
        Question pregunta = Question.builder()
                .id("Q-1").nombre("N").enunciado("E")
                .opciones(new QuestionDistractors("a", "b", "c", "d")).respuestaCorrecta('A')
                .estado(estado).competencia(Competencia.INGLES).tema("T")
                .dificultad(Dificultad.BASICO).build();

        assertThatThrownBy(() -> pregunta.reabrir(DESPUES))
                .isInstanceOf(OperacionNoPermitidaException.class)
                .hasMessageContaining("Rechazada");
    }

    @Test
    @DisplayName("una pregunta puede reabrirse varias veces y la versión sigue subiendo")
    void variasReaperturas() {
        Question segunda = rechazada().reabrir(DESPUES)
                .conEstado(EstadoPregunta.PENDIENTE_REVISION, DESPUES)
                .conEstado(EstadoPregunta.EN_REVISION, DESPUES)
                .conEstado(EstadoPregunta.RECHAZADA, DESPUES)
                .reabrir(DESPUES);

        assertThat(segunda.getVersion()).isEqualTo(3);
        assertThat(segunda.getEstado()).isEqualTo(EstadoPregunta.BORRADOR);
    }
}
