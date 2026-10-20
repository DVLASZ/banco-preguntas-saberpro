package co.unicauca.bancopreguntas.simulation.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/** Fila de {@code simulation_item}: una pregunta del simulacro tal como se le mostró al estudiante. */
@Entity
@Table(name = "simulation_item")
class SimulationItemEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "simulation_id")
    SimulationEntity simulation;

    @Column(nullable = false)
    int position;

    @Column(name = "question_id", nullable = false, length = 36)
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

    @Column(length = 1)
    String answer;

    protected SimulationItemEntity() {
        // requerido por JPA
    }
}
