package co.unicauca.saberpro.api.service;

import co.unicauca.saberpro.api.dto.QuestionRequest;
import co.unicauca.saberpro.api.dto.QuestionResponse;
import co.unicauca.saberpro.api.model.QuestionMapper;
import co.unicauca.saberpro.preguntas.domain.EstadoPregunta;
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
        return QuestionMapper.toResponse(
                questionService.crearBorrador(QuestionMapper.toContenido(request), request.autor()));
    }

    @Override
    public QuestionResponse update(String id, QuestionRequest request) {
        questionService.actualizarContenido(id, QuestionMapper.toContenido(request), request.autor());
        return findById(id);
    }

    @Override
    public void delete(String id) {
        questionService.cambiarEstado(id, EstadoPregunta.ARCHIVADA);
    }
}
