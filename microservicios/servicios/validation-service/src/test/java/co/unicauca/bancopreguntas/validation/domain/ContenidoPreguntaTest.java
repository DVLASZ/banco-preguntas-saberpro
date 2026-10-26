package co.unicauca.bancopreguntas.validation.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ContenidoPregunta")
class ContenidoPreguntaTest {

    @Test
    @DisplayName("guarda los textos tal como llegan, sin recortarlos ni corregirlos")
    void guardaLosTextosCrudos() {
        ContenidoPregunta c = new ContenidoPregunta(" Nombre ", null, "¿Qué?", "a", "b", "c", "d", "z", "j", "b",
                Competencia.LECTURA_CRITICA, "tema", "sub", Dificultad.BASICO);

        assertThat(c.nombre()).isEqualTo(" Nombre ");
        assertThat(c.contexto()).isNull();
        assertThat(c.respuestaCorrecta()).isEqualTo("z");
        assertThat(c.competencia()).isEqualTo(Competencia.LECTURA_CRITICA);
    }
}
