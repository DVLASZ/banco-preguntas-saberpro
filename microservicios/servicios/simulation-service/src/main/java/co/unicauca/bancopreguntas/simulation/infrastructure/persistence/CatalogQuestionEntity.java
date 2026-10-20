package co.unicauca.bancopreguntas.simulation.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/** Fila de {@code catalog_question}: la copia de una pregunta publicada. */
@Entity
@Table(name = "catalog_question")
class CatalogQuestionEntity {

    @Id
    @Column(name = "question_id", length = 36)
    String questionId;

    @Column(name = "question_version", nullable = false)
    int version;

    @Column(length = 200)
    String title;

    @Column(nullable = false, length = 40)
    String competency;

    @Column(length = 200)
    String topic;

    @Column(length = 200)
    String subtopic;

    @Column(nullable = false, length = 20)
    String difficulty;

    @Column(columnDefinition = "text")
    String context;

    @Column(nullable = false, columnDefinition = "text")
    String statement;

    @Column(name = "option_a", nullable = false, length = 500)
    String optionA;

    @Column(name = "option_b", nullable = false, length = 500)
    String optionB;

    @Column(name = "option_c", nullable = false, length = 500)
    String optionC;

    @Column(name = "option_d", nullable = false, length = 500)
    String optionD;

    @Column(name = "correct_answer", nullable = false, length = 1)
    String correctAnswer;

    @Column(name = "published_at")
    Instant publishedAt;

    @Column(nullable = false)
    boolean available;

    protected CatalogQuestionEntity() {
        // requerido por JPA
    }
}
