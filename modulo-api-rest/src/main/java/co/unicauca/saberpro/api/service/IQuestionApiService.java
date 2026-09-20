package co.unicauca.saberpro.api.service;

import co.unicauca.saberpro.api.dto.QuestionRequest;
import co.unicauca.saberpro.api.dto.QuestionResponse;

import java.util.List;

public interface IQuestionApiService {

    List<QuestionResponse> findAll();

    /** @throws java.util.NoSuchElementException si no existe una pregunta con ese id */
    QuestionResponse findById(String id);

    QuestionResponse save(QuestionRequest request);

    /** @throws java.util.NoSuchElementException si no existe una pregunta con ese id */
    QuestionResponse update(String id, QuestionRequest request);

    /**
     * "Elimina" archivando la pregunta: RNF-16 prohibe borrarla fisicamente.
     *
     * @throws java.util.NoSuchElementException si no existe una pregunta con ese id
     */
    void delete(String id);
}
