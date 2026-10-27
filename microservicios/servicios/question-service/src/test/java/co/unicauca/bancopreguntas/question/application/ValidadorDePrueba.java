package co.unicauca.bancopreguntas.question.application;

import co.unicauca.bancopreguntas.question.application.port.ValidadorDeContenido;
import co.unicauca.bancopreguntas.question.domain.ContenidoPregunta;
import co.unicauca.bancopreguntas.question.domain.DependenciaNoDisponibleException;
import co.unicauca.bancopreguntas.question.domain.validation.QuestionValidationException;
import co.unicauca.bancopreguntas.question.domain.validation.Violacion;

import java.util.ArrayList;
import java.util.List;

/**
 * Doble de prueba de validation-service con tres reglas sencillas: nombre y contexto no vacíos y opción B distinta
 * de la A. Puede simular que el servicio está caído.
 */
class ValidadorDePrueba implements ValidadorDeContenido {

    private boolean caido;
    private int llamadas;

    void caer() {
        caido = true;
    }

    int llamadas() {
        return llamadas;
    }

    @Override
    public void validarOLanzar(ContenidoPregunta c) {
        llamadas++;
        if (caido) {
            throw new DependenciaNoDisponibleException("El servicio de validación no está disponible", null);
        }
        List<Violacion> violaciones = new ArrayList<>();
        if (c.nombre() == null || c.nombre().isBlank()) {
            violaciones.add(new Violacion("nombre", "El nombre es obligatorio"));
        }
        if (c.contexto() == null || c.contexto().isBlank()) {
            violaciones.add(new Violacion("contexto", "El contexto es obligatorio"));
        }
        if (c.opcionA() != null && c.opcionA().equals(c.opcionB())) {
            violaciones.add(new Violacion("opcionB", "Las opciones no pueden repetirse"));
        }
        if (!violaciones.isEmpty()) {
            throw new QuestionValidationException(violaciones);
        }
    }
}
