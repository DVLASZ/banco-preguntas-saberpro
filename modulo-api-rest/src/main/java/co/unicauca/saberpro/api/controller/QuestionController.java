package co.unicauca.saberpro.api.controller;

import co.unicauca.saberpro.api.dto.QuestionRequest;
import co.unicauca.saberpro.api.dto.QuestionResponse;
import co.unicauca.saberpro.api.service.IQuestionApiService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/questions")
public class QuestionController {

    private final IQuestionApiService questionService;

    public QuestionController(IQuestionApiService questionService) {
        this.questionService = questionService;
    }

    // HTTP GET: listar todas las preguntas
    @GetMapping
    public List<QuestionResponse> getAllQuestions() {
        return questionService.findAll();
    }

    // HTTP GET: buscar una pregunta por id
    @GetMapping("/{id}")
    public QuestionResponse getQuestionById(@PathVariable String id) {
        return questionService.findById(id);
    }

    // HTTP POST: crear una pregunta
    @PostMapping
    public ResponseEntity<QuestionResponse> createQuestion(@Valid @RequestBody QuestionRequest request) {
        QuestionResponse creada = questionService.save(request);
        URI ubicacion = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(creada.id())
                .toUri();
        return ResponseEntity.created(ubicacion).body(creada);
    }

    // HTTP PUT: actualizar el contenido de una pregunta
    @PutMapping("/{id}")
    public QuestionResponse updateQuestion(@PathVariable String id, @Valid @RequestBody QuestionRequest request) {
        return questionService.update(id, request);
    }

    // HTTP DELETE: archivar la pregunta (no se borra fisicamente, RNF-16)
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteQuestion(@PathVariable String id) {
        questionService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
