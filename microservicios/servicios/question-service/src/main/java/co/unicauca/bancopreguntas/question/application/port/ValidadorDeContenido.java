package co.unicauca.bancopreguntas.question.application.port;

import co.unicauca.bancopreguntas.question.domain.ContenidoPregunta;
import co.unicauca.bancopreguntas.question.domain.DependenciaNoDisponibleException;
import co.unicauca.bancopreguntas.question.domain.validation.QuestionValidationException;

/** Puerto de salida: la validación estructural del contenido, que hace validation-service (HU-03). */
public interface ValidadorDeContenido {

    /**
     * @throws QuestionValidationException        con todos los campos que incumplen, si hay alguno
     * @throws DependenciaNoDisponibleException si no se pudo consultar la validación
     */
    void validarOLanzar(ContenidoPregunta contenido);
}
