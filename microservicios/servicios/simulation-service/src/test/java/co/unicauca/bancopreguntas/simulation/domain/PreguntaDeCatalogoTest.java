package co.unicauca.bancopreguntas.simulation.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("PreguntaDeCatalogo")
class PreguntaDeCatalogoTest {

    @Test
    @DisplayName("normaliza la competencia, la dificultad y la letra de la respuesta")
    void normaliza() {
        PreguntaDeCatalogo p = new PreguntaDeCatalogo("q-1", 0, null, " lectura_critica ", " Tema ", null, "basico", null,
                "¿Cuál?", "a", "b", "c", "d", 'c', null, true);

        assertThat(p.competencia()).isEqualTo("LECTURA_CRITICA");
        assertThat(p.dificultad()).isEqualTo("BASICO");
        assertThat(p.respuestaCorrecta()).isEqualTo('C');
        assertThat(p.version()).isEqualTo(1);
        assertThat(p.tema()).isEqualTo("Tema");
        assertThat(p.subtema()).isEmpty();
        assertThat(p.contexto()).isEmpty();
    }

    @Test
    @DisplayName("sin dificultad se asume intermedia")
    void dificultadPorDefecto() {
        PreguntaDeCatalogo p = new PreguntaDeCatalogo("q-1", 1, "t", "INGLES", "t", "s", " ", "c", "e", "a", "b", "c", "d",
                'A', null, true);

        assertThat(p.dificultad()).isEqualTo("INTERMEDIO");
    }

    @Test
    @DisplayName("exige identificador, competencia, enunciado y las cuatro opciones")
    void obligatorios() {
        assertThatThrownBy(() -> new PreguntaDeCatalogo(" ", 1, "t", "C", "t", "s", "BASICO", "c", "e", "a", "b", "c", "d", 'A', null, true))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new PreguntaDeCatalogo("q", 1, "t", null, "t", "s", "BASICO", "c", "e", "a", "b", "c", "d", 'A', null, true))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new PreguntaDeCatalogo("q", 1, "t", "C", "t", "s", "BASICO", "c", "", "a", "b", "c", "d", 'A', null, true))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new PreguntaDeCatalogo("q", 1, "t", "C", "t", "s", "BASICO", "c", "e", "a", "b", "c", null, 'A', null, true))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("la respuesta correcta debe ser A, B, C o D")
    void respuestaValida() {
        assertThatThrownBy(() -> new PreguntaDeCatalogo("q", 1, "t", "C", "t", "s", "BASICO", "c", "e", "a", "b", "c", "d", 'E', null, true))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("A, B, C o D");
    }

    @Test
    @DisplayName("entrega el texto de cada opción y rechaza una letra inválida")
    void opcion() {
        PreguntaDeCatalogo p = Preguntas.pregunta("q-1");

        assertThat(p.opcion('a')).isEqualTo("Opción A");
        assertThat(p.opcion('D')).isEqualTo("Opción D");
        assertThatThrownBy(() -> p.opcion('Z')).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("retirarla la deja no disponible sin cambiar nada más")
    void retirada() {
        PreguntaDeCatalogo p = Preguntas.pregunta("q-1");

        PreguntaDeCatalogo retirada = p.retirada();

        assertThat(retirada.disponible()).isFalse();
        assertThat(retirada.preguntaId()).isEqualTo("q-1");
        assertThat(retirada.respuestaCorrecta()).isEqualTo(p.respuestaCorrecta());
        assertThat(p.disponible()).isTrue();
    }
}
