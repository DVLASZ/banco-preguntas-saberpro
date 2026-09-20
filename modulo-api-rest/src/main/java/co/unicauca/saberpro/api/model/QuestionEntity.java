package co.unicauca.saberpro.api.model;

import co.unicauca.saberpro.preguntas.domain.Competencia;
import co.unicauca.saberpro.preguntas.domain.Dificultad;
import co.unicauca.saberpro.preguntas.domain.EstadoPregunta;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Entidad JPA de una pregunta. Vive solo en el modulo de la API: el dominio
 * ({@code Question}) queda libre de anotaciones de persistencia y este
 * adaptador se encarga de convertir entre ambos (ver {@code QuestionMapper}).
 */
@Entity
@Table(name = "questions")
public class QuestionEntity {

    @Id
    @Column(length = 20)
    private String id;

    @Column(nullable = false, length = 200)
    private String nombre;

    @Column(nullable = false, length = 2000)
    private String enunciado;

    @Column(name = "opcion_a", nullable = false, length = 500)
    private String opcionA;

    @Column(name = "opcion_b", nullable = false, length = 500)
    private String opcionB;

    @Column(name = "opcion_c", nullable = false, length = 500)
    private String opcionC;

    @Column(name = "opcion_d", nullable = false, length = 500)
    private String opcionD;

    @Column(name = "respuesta_correcta", nullable = false, length = 1)
    private String respuestaCorrecta;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private EstadoPregunta estado;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private Competencia competencia;

    @Column(nullable = false, length = 200)
    private String tema;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Dificultad dificultad;

    protected QuestionEntity() {
    }

    public QuestionEntity(String id, String nombre, String enunciado, String opcionA, String opcionB,
                          String opcionC, String opcionD, String respuestaCorrecta, EstadoPregunta estado,
                          Competencia competencia, String tema, Dificultad dificultad) {
        this.id = id;
        this.nombre = nombre;
        this.enunciado = enunciado;
        this.opcionA = opcionA;
        this.opcionB = opcionB;
        this.opcionC = opcionC;
        this.opcionD = opcionD;
        this.respuestaCorrecta = respuestaCorrecta;
        this.estado = estado;
        this.competencia = competencia;
        this.tema = tema;
        this.dificultad = dificultad;
    }

    public String getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public String getEnunciado() {
        return enunciado;
    }

    public String getOpcionA() {
        return opcionA;
    }

    public String getOpcionB() {
        return opcionB;
    }

    public String getOpcionC() {
        return opcionC;
    }

    public String getOpcionD() {
        return opcionD;
    }

    public String getRespuestaCorrecta() {
        return respuestaCorrecta;
    }

    public EstadoPregunta getEstado() {
        return estado;
    }

    public Competencia getCompetencia() {
        return competencia;
    }

    public String getTema() {
        return tema;
    }

    public Dificultad getDificultad() {
        return dificultad;
    }
}
