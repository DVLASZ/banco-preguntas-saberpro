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
 * adaptador se encarga de convertir entre ambos (ver {@code QuestionMapper},
 * el unico que la llena).
 */
@Entity
@Table(name = "questions")
public class QuestionEntity {

    @Id
    @Column(length = 20)
    private String id;

    @Column(nullable = false, length = 200)
    private String nombre;

    @Column(length = 4000)
    private String contexto;

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

    @Column(length = 4000)
    private String justificacion;

    @Column(length = 1000)
    private String bibliografia;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private EstadoPregunta estado;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private Competencia competencia;

    @Column(nullable = false, length = 200)
    private String tema;

    @Column(length = 200)
    private String subtema;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Dificultad dificultad;

    @Column(length = 50)
    private String autor;

    protected QuestionEntity() {
    }

    public String getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public String getContexto() {
        return contexto;
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

    public String getJustificacion() {
        return justificacion;
    }

    public String getBibliografia() {
        return bibliografia;
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

    public String getSubtema() {
        return subtema;
    }

    public Dificultad getDificultad() {
        return dificultad;
    }

    public String getAutor() {
        return autor;
    }

    void setId(String id) {
        this.id = id;
    }

    void setNombre(String nombre) {
        this.nombre = nombre;
    }

    void setContexto(String contexto) {
        this.contexto = contexto;
    }

    void setEnunciado(String enunciado) {
        this.enunciado = enunciado;
    }

    void setOpcionA(String opcionA) {
        this.opcionA = opcionA;
    }

    void setOpcionB(String opcionB) {
        this.opcionB = opcionB;
    }

    void setOpcionC(String opcionC) {
        this.opcionC = opcionC;
    }

    void setOpcionD(String opcionD) {
        this.opcionD = opcionD;
    }

    void setRespuestaCorrecta(String respuestaCorrecta) {
        this.respuestaCorrecta = respuestaCorrecta;
    }

    void setJustificacion(String justificacion) {
        this.justificacion = justificacion;
    }

    void setBibliografia(String bibliografia) {
        this.bibliografia = bibliografia;
    }

    void setEstado(EstadoPregunta estado) {
        this.estado = estado;
    }

    void setCompetencia(Competencia competencia) {
        this.competencia = competencia;
    }

    void setTema(String tema) {
        this.tema = tema;
    }

    void setSubtema(String subtema) {
        this.subtema = subtema;
    }

    void setDificultad(Dificultad dificultad) {
        this.dificultad = dificultad;
    }

    void setAutor(String autor) {
        this.autor = autor;
    }
}
