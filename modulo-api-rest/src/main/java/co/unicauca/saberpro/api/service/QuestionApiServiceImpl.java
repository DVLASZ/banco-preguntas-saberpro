package co.unicauca.saberpro.api.service;

import co.unicauca.saberpro.api.dto.QuestionRequest;
import co.unicauca.saberpro.api.dto.QuestionResponse;
import co.unicauca.saberpro.api.model.QuestionMapper;
import co.unicauca.saberpro.preguntas.domain.EstadoPregunta;
import co.unicauca.saberpro.preguntas.domain.Question;
import co.unicauca.saberpro.preguntas.domain.QuestionDistractors;
import co.unicauca.saberpro.preguntas.domain.QuestionService;
import org.springframework.stereotype.Service;

import java.util.List;

/** Traduce entre el JSON de la API y el servicio de dominio del banco de preguntas. */
@Service
public class QuestionApiServiceImpl implements IQuestionApiService {

    private final QuestionService questionService;

    public QuestionApiServiceImpl(QuestionService questionService) {
        this.questionService = questionService;
    }

    @Override
    public List<QuestionResponse> findAll() {
        return questionService.listarPreguntas().stream().map(QuestionMapper::toResponse).toList();
    }

    @Override
    public QuestionResponse findById(String id) {
        return QuestionMapper.toResponse(questionService.obtenerPregunta(id));
    }

    @Override
    public QuestionResponse save(QuestionRequest request) {
        Question creada = questionService.crearPregunta(
                request.nombre().trim(),
                request.enunciado().trim(),
                distractoresDe(request),
                letraDe(request),
                request.competencia(),
                request.tema().trim(),
                request.dificultad());
        return QuestionMapper.toResponse(creada);
    }

    @Override
    public QuestionResponse update(String id, QuestionRequest request) {
        questionService.actualizarContenido(
                id,
                request.nombre().trim(),
                request.enunciado().trim(),
                distractoresDe(request),
                letraDe(request),
                request.competencia(),
                request.tema().trim(),
                request.dificultad());
        return findById(id);
    }

    @Override
    public void delete(String id) {
        questionService.cambiarEstado(id, EstadoPregunta.ARCHIVADA);
    }

    private static QuestionDistractors distractoresDe(QuestionRequest request) {
        return new QuestionDistractors(request.opcionA().trim(), request.opcionB().trim(),
                request.opcionC().trim(), request.opcionD().trim());
    }

    private static char letraDe(QuestionRequest request) {
        return request.respuestaCorrecta().trim().charAt(0);
    }
}
