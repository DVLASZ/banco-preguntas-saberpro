package co.unicauca.saberpro.microkernel.common.entities;

import co.unicauca.saberpro.preguntas.domain.Dificultad;

import java.util.List;

/**
 * Datos que un cliente (la interfaz Swing, un importador, etc.) entrega a
 * un {@link co.unicauca.saberpro.microkernel.common.interfaces.QuestionPlugin}
 * para que genere una pregunta real del banco. No es la pregunta en sí
 * (eso es {@link co.unicauca.saberpro.preguntas.domain.Question}) sino la
 * solicitud sin validar todavía — por eso {@code classification} viaja
 * como texto libre y {@code correctAnswer} como el texto de la opción, en
 * vez de ya venir como {@code Competencia}/letra: son justamente lo que
 * el pipeline de validación (HU del taller) debe verificar antes de que
 * un plugin pueda construir la pregunta real.
 */
public class QuestionRequest {

    private final String title;
    private final String content;
    private final String type;
    private final String classification;
    private final String tema;
    private final Dificultad dificultad;
    private final List<String> options;
    private final String correctAnswer;
    private final String recursoMultimedia;

    public QuestionRequest(String title, String content, String type, String classification,
                            String tema, Dificultad dificultad, List<String> options,
                            String correctAnswer, String recursoMultimedia) {
        this.title = title;
        this.content = content;
        this.type = type;
        this.classification = classification;
        this.tema = tema;
        this.dificultad = dificultad;
        this.options = options;
        this.correctAnswer = correctAnswer;
        this.recursoMultimedia = recursoMultimedia;
    }

    public String getTitle() {
        return title;
    }

    public String getContent() {
        return content;
    }

    public String getType() {
        return type;
    }

    public String getClassification() {
        return classification;
    }

    public String getTema() {
        return tema;
    }

    public Dificultad getDificultad() {
        return dificultad;
    }

    public List<String> getOptions() {
        return options;
    }

    public String getCorrectAnswer() {
        return correctAnswer;
    }

    /** Referencia (URL, ruta, descripción) al recurso multimedia. Solo la usa MultimediaQuestionPlugin. */
    public String getRecursoMultimedia() {
        return recursoMultimedia;
    }
}
