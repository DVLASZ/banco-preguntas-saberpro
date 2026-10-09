package co.unicauca.bancopreguntas.question.domain;

/** La pregunta pedida no existe. */
public class QuestionNotFoundException extends RuntimeException {

    public QuestionNotFoundException(String id) {
        super("No existe una pregunta con id " + id);
    }
}
