package co.unicauca.saberpro.microkernel.plugins;

import co.unicauca.saberpro.microkernel.common.entities.QuestionRequest;
import co.unicauca.saberpro.microkernel.common.interfaces.QuestionPlugin;
import co.unicauca.saberpro.microkernel.pipeline.base.QuestionPipeline;
import co.unicauca.saberpro.microkernel.pipeline.filters.ClassificationFilter;
import co.unicauca.saberpro.microkernel.pipeline.filters.ContentValidationFilter;
import co.unicauca.saberpro.microkernel.pipeline.filters.CorrectAnswerValidationFilter;
import co.unicauca.saberpro.microkernel.pipeline.filters.OptionsValidationFilter;
import co.unicauca.saberpro.preguntas.domain.Competencia;
import co.unicauca.saberpro.preguntas.domain.EstadoPregunta;
import co.unicauca.saberpro.preguntas.domain.Question;
import co.unicauca.saberpro.preguntas.domain.QuestionDistractors;

import java.util.UUID;

/**
 * Implementación común a los plugins de generación de preguntas: todos
 * ejecutan el mismo pipeline de validación (Tuberías y Filtros) antes de
 * construir la {@link Question} real del banco — la guía del taller pide
 * integrarlo en "al menos uno" de los plugins, pero aquí se hace en los
 * tres, porque no tiene sentido que un plugin genere una pregunta sin
 * pasar por la misma validación que las demás.
 * <p>
 * Cada subclase concreta solo decide su {@code getName()}, qué
 * {@code type} soporta, y cómo se redacta el enunciado final a partir de
 * la solicitud (plantilla, patrón Template Method).
 */
public abstract class BaseQuestionPlugin implements QuestionPlugin {

    private final QuestionPipeline pipeline;

    protected BaseQuestionPlugin() {
        pipeline = new QuestionPipeline();
        pipeline.addFilter(new ContentValidationFilter());
        pipeline.addFilter(new OptionsValidationFilter());
        pipeline.addFilter(new ClassificationFilter());
        pipeline.addFilter(new CorrectAnswerValidationFilter());
    }

    @Override
    public final Question generate(QuestionRequest request) {
        if (!pipeline.execute(request)) {
            return null;
        }

        Competencia competencia = resolverCompetencia(request.getClassification());
        QuestionDistractors opciones = new QuestionDistractors(
                request.getOptions().get(0), request.getOptions().get(1),
                request.getOptions().get(2), request.getOptions().get(3));
        char respuestaCorrecta = (char) ('A' + request.getOptions().indexOf(request.getCorrectAnswer()));

        return new Question(
                UUID.randomUUID().toString(),
                request.getTitle(),
                construirEnunciado(request),
                opciones,
                respuestaCorrecta,
                EstadoPregunta.PENDIENTE_REVISION,
                competencia,
                request.getTema(),
                request.getDificultad());
    }

    /** Redacta el enunciado final de la pregunta a partir de la solicitud ya validada. */
    protected abstract String construirEnunciado(QuestionRequest request);

    /** Nombre simple de la clase del último filtro que hizo fallar la validación (para diagnóstico/UI). */
    public String getUltimoFiltroFallido() {
        return pipeline.getUltimoFiltroFallido();
    }

    private Competencia resolverCompetencia(String clasificacion) {
        for (Competencia competencia : Competencia.values()) {
            if (competencia.toString().equalsIgnoreCase(clasificacion.trim())) {
                return competencia;
            }
        }
        // No debería ocurrir: ClassificationFilter ya garantizó que es válida.
        throw new IllegalStateException("Clasificación no reconocida: " + clasificacion);
    }
}
