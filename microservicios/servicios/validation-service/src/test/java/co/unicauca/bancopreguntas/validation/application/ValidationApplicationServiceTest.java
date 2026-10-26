package co.unicauca.bancopreguntas.validation.application;

import co.unicauca.bancopreguntas.validation.domain.validation.ContenidoDePrueba;
import co.unicauca.bancopreguntas.validation.domain.validation.QuestionValidator;
import co.unicauca.bancopreguntas.validation.domain.validation.Violacion;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ValidationApplicationService")
class ValidationApplicationServiceTest {

    private final ValidationApplicationService servicio = new ValidationApplicationService(QuestionValidator.porDefecto());

    @Test
    @DisplayName("un contenido que cumple las reglas no tiene violaciones")
    void valido() {
        assertThat(servicio.validar(ContenidoDePrueba.valido().build())).isEmpty();
    }

    @Test
    @DisplayName("un contenido sin nombre ni contexto reporta cada campo en una sola respuesta")
    void invalido() {
        List<Violacion> violaciones = servicio.validar(ContenidoDePrueba.valido().nombre("").contexto(" ").build());

        assertThat(violaciones).extracting(Violacion::campo).contains("nombre", "contexto");
    }
}
