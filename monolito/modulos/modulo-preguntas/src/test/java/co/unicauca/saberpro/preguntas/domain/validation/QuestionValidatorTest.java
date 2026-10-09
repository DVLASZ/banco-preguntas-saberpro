package co.unicauca.saberpro.preguntas.domain.validation;

import co.unicauca.saberpro.preguntas.domain.ContenidoPregunta;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QuestionValidatorTest {

    private final QuestionValidator validador = QuestionValidator.porDefecto();

    @Test
    void unContenidoCompletoYBienFormadoNoTieneViolaciones() {
        assertTrue(validador.validar(ContenidoDePrueba.valido().build()).isEmpty());
    }

    @Test
    void reuneLasViolacionesDeTodasLasReglasEnUnaSolaPasada() {
        ContenidoPregunta malo = ContenidoDePrueba.valido()
                .contexto("")
                .enunciado("Sin signo de pregunta")
                .opcionB("Todas las anteriores")
                .respuesta("Z")
                .build();

        List<String> campos = validador.validar(malo).stream().map(Violacion::campo).toList();

        assertTrue(campos.containsAll(List.of("contexto", "enunciado", "opcionB", "respuestaCorrecta")),
                "debe reportar todos los campos inválidos, no solo el primero: " + campos);
    }

    @Test
    void validarOLanzarLanzaUnaExcepcionConCadaViolacion() {
        ContenidoPregunta malo = ContenidoDePrueba.valido().contexto("").tema(" ").build();

        QuestionValidationException ex = assertThrows(QuestionValidationException.class,
                () -> validador.validarOLanzar(malo));

        assertEquals(2, ex.getViolaciones().size());
        assertTrue(ex.getMessage().contains("El contexto es obligatorio"));
        assertTrue(ex.getMessage().contains("El tema es obligatorio"));
    }

    @Test
    void laExcepcionEsUnaIllegalArgumentExceptionParaQuienYaLaAtrapaba() {
        ContenidoPregunta malo = ContenidoDePrueba.valido().contexto("").build();

        Exception ex = assertThrows(Exception.class, () -> validador.validarOLanzar(malo));

        assertInstanceOf(IllegalArgumentException.class, ex);
    }

    @Test
    void validarOLanzarNoHaceNadaSiElContenidoEsValido() {
        validador.validarOLanzar(ContenidoDePrueba.valido().build());
    }

    @Test
    void sePuedeComponerConOtrasReglasSinTocarLasExistentes() {
        ValidationRule soloExigeTemaCorto = c -> c.tema().length() > 5
                ? List.of(new Violacion("tema", "El tema es demasiado largo"))
                : List.of();
        QuestionValidator personalizado = new QuestionValidator(List.of(soloExigeTemaCorto));

        List<Violacion> violaciones = personalizado.validar(ContenidoDePrueba.valido().contexto("").build());

        assertEquals(List.of(new Violacion("tema", "El tema es demasiado largo")), violaciones);
    }
}
