package co.unicauca.saberpro.preguntas.simulacro.domain;

import co.unicauca.saberpro.preguntas.domain.Competencia;
import co.unicauca.saberpro.preguntas.domain.Dificultad;
import co.unicauca.saberpro.preguntas.domain.Question;
import co.unicauca.saberpro.preguntas.domain.QuestionService;
import co.unicauca.saberpro.preguntas.simulacro.domain.exception.SinPreguntasDisponiblesException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * Servicio de dominio para la generación y presentación de simulacros
 * (HU-12 a HU-14). Depende de {@link QuestionService} (no de
 * {@code QuestionRepository} directamente) para reutilizar su lógica de
 * búsqueda de preguntas publicadas ({@code buscarPublicadas}), y de
 * {@link SimulacroRepository} para persistir simulacros e intentos —
 * ambas son abstracciones, nunca implementaciones concretas (DIP).
 */
public class SimulacroService {

    private final QuestionService questionService;
    private final SimulacroRepository repository;

    public SimulacroService(QuestionService questionService, SimulacroRepository repository) {
        this.questionService = questionService;
        this.repository = repository;
    }

    /**
     * Genera un simulacro para un grupo (HU-12, RF-21/RF-22): toma como
     * máximo {@code cantidadPreguntas} preguntas ya {@code PUBLICADA}s que
     * coincidan con los filtros dados (cualquiera puede ser {@code null}
     * para no filtrar por ese criterio), en orden aleatorio. Si ninguna
     * pregunta publicada coincide, no se puede generar el simulacro.
     */
    public Simulacro generarSimulacro(String grupo, Competencia competencia, String tema, Dificultad dificultad,
                                       int cantidadPreguntas, int tiempoMaximoMinutos) {
        if (cantidadPreguntas <= 0) {
            throw new IllegalArgumentException("La cantidad de preguntas debe ser mayor a cero");
        }
        List<Question> disponibles = new ArrayList<>(questionService.buscarPublicadas(competencia, tema, dificultad));
        if (disponibles.isEmpty()) {
            throw new SinPreguntasDisponiblesException(
                    "No hay preguntas publicadas que coincidan con los filtros seleccionados");
        }
        Collections.shuffle(disponibles);
        List<Question> seleccionadas = disponibles.subList(0, Math.min(cantidadPreguntas, disponibles.size()));

        String id = repository.generarNuevoIdSimulacro();
        Simulacro simulacro = new Simulacro(id, grupo, seleccionadas, tiempoMaximoMinutos,
                competencia, tema, dificultad);
        repository.guardarSimulacro(simulacro);
        return simulacro;
    }

    public List<Simulacro> listarSimulacros() {
        return repository.listarSimulacros();
    }

    public Simulacro obtenerSimulacro(String id) {
        Simulacro simulacro = repository.obtenerSimulacro(id);
        if (simulacro == null) {
            throw new NoSuchElementException("No existe un simulacro con id " + id);
        }
        return simulacro;
    }

    /** Comienza un nuevo intento de un estudiante sobre un simulacro (HU-13). */
    public IntentoSimulacro iniciarIntento(String simulacroId, String estudianteUsername) {
        obtenerSimulacro(simulacroId); // valida que exista
        String id = repository.generarNuevoIdIntento();
        IntentoSimulacro intento = new IntentoSimulacro(id, simulacroId, estudianteUsername);
        repository.guardarIntento(intento);
        return intento;
    }

    /** Registra (o corrige) la respuesta del estudiante a una pregunta del simulacro. */
    public void responder(String intentoId, String preguntaId, char respuesta) {
        IntentoSimulacro intento = obtenerIntento(intentoId);
        intento.responder(preguntaId, respuesta);
        repository.guardarIntento(intento);
    }

    /**
     * Finaliza un intento y lo califica automáticamente (HU-14, RF-24):
     * compara cada respuesta seleccionada contra la respuesta correcta de
     * la pregunta; las preguntas sin responder cuentan como incorrectas.
     */
    public IntentoSimulacro finalizar(String intentoId) {
        IntentoSimulacro intento = obtenerIntento(intentoId);
        Simulacro simulacro = obtenerSimulacro(intento.getSimulacroId());

        int aciertos = 0;
        for (Question pregunta : simulacro.getPreguntas()) {
            Character respuesta = intento.getRespuesta(pregunta.getId());
            if (respuesta != null && respuesta == pregunta.getRespuestaCorrecta()) {
                aciertos++;
            }
        }
        intento.finalizar(aciertos);
        repository.guardarIntento(intento);
        return intento;
    }

    public IntentoSimulacro obtenerIntento(String id) {
        IntentoSimulacro intento = repository.obtenerIntento(id);
        if (intento == null) {
            throw new NoSuchElementException("No existe un intento con id " + id);
        }
        return intento;
    }

    public List<IntentoSimulacro> listarIntentosDe(String estudianteUsername) {
        return repository.listarIntentosDeEstudiante(estudianteUsername);
    }
}
