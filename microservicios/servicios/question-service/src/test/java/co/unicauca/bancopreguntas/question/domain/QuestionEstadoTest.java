package co.unicauca.bancopreguntas.question.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Question: copia del estado, edición y marcas de tiempo")
class QuestionEstadoTest {

    private static final Instant CREADA = Instant.parse("2026-10-08T10:00:00Z");
    private static final Instant DESPUES = Instant.parse("2026-10-08T11:30:00Z");

    private static Question borrador() {
        return Question.builder()
                .id("Q-1").nombre("Observer").contexto("Contexto").enunciado("¿Qué patrón notifica?")
                .opciones(new QuestionDistractors("Factory", "Observer", "Singleton", "Adapter"))
                .respuestaCorrecta('B').justificacion("Porque sí").bibliografia("GoF")
                .estado(EstadoPregunta.BORRADOR).competencia(Competencia.LECTURA_CRITICA)
                .tema("Patrones").subtema("Observer").dificultad(Dificultad.BASICO).autor("autor-1")
                .creadaEn(CREADA).actualizadaEn(CREADA).estadoSecuencia(1)
                .build();
    }

    @Test
    @DisplayName("conEstado devuelve una copia con el estado, la versión y la secuencia nuevos")
    void devuelveUnaCopia() {
        Question original = borrador();

        Question enviada = original.conEstado(EstadoPregunta.PENDIENTE_REVISION, 1, 2, DESPUES);

        assertThat(enviada.getEstado()).isEqualTo(EstadoPregunta.PENDIENTE_REVISION);
        assertThat(enviada.getEstadoSecuencia()).isEqualTo(2);
        assertThat(enviada.getActualizadaEn()).isEqualTo(DESPUES);
        assertThat(enviada.getCreadaEn()).isEqualTo(CREADA);
        assertThat(enviada.getId()).isEqualTo("Q-1");
        assertThat(enviada.getAutor()).isEqualTo("autor-1");
        assertThat(enviada.getOpciones().getOpcionB()).isEqualTo("Observer");
        assertThat(original.getEstado()).isEqualTo(EstadoPregunta.BORRADOR);
        assertThat(original.getActualizadaEn()).isEqualTo(CREADA);
    }

    @Test
    @DisplayName("un cambio con una secuencia que ya se aplicó se ignora: devuelve la misma pregunta")
    void ignoraLoAtrasado() {
        Question enviada = borrador().conEstado(EstadoPregunta.PENDIENTE_REVISION, 1, 2, DESPUES);

        assertThat(enviada.conEstado(EstadoPregunta.BORRADOR, 1, 2, DESPUES)).isSameAs(enviada);
        assertThat(enviada.conEstado(EstadoPregunta.BORRADOR, 1, 1, DESPUES)).isSameAs(enviada);
    }

    @Test
    @DisplayName("al reabrirla sube la versión del contenido")
    void reabierta() {
        Question reabierta = borrador().conEstado(EstadoPregunta.BORRADOR, 2, 5, DESPUES);

        assertThat(reabierta.getVersion()).isEqualTo(2);
        assertThat(reabierta.esEditable()).isTrue();
    }

    @Test
    @DisplayName("solo se edita en Borrador")
    void soloSeEditaEnBorrador() {
        Question pendiente = borrador().conEstado(EstadoPregunta.PENDIENTE_REVISION, 1, 2, DESPUES);

        assertThat(borrador().esEditable()).isTrue();
        assertThat(pendiente.esEditable()).isFalse();
        assertThatThrownBy(pendiente::exigirEditable).isInstanceOf(OperacionNoPermitidaException.class)
                .hasMessageContaining("Borrador");
        for (EstadoPregunta e : EstadoPregunta.values()) {
            assertThat(e.editable()).isEqualTo(e == EstadoPregunta.BORRADOR);
        }
    }

    @Test
    @DisplayName("exige un estado y una secuencia no negativa")
    void invariantes() {
        assertThatThrownBy(() -> borrador().conEstado(null, 1, 9, DESPUES)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Question.builder().id("Q-2").nombre("n").enunciado("e")
                .opciones(new QuestionDistractors("a", "b", "c", "d")).respuestaCorrecta('A')
                .estado(EstadoPregunta.BORRADOR).competencia(Competencia.INGLES).tema("t")
                .dificultad(Dificultad.BASICO).estadoSecuencia(-1).build()).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Una pregunta sin persistir no tiene marcas de tiempo")
    void sinMarcasDeTiempoMientrasNoSePersiste() {
        Question sinPersistir = new Question("Q-2", "Nombre", "Enunciado",
                new QuestionDistractors("a", "b", "c", "d"), 'A', EstadoPregunta.BORRADOR,
                Competencia.INGLES, "Tema", Dificultad.AVANZADO);

        assertThat(sinPersistir.getCreadaEn()).isNull();
        assertThat(sinPersistir.getActualizadaEn()).isNull();
        assertThat(sinPersistir.getEstadoSecuencia()).isZero();
    }
}
