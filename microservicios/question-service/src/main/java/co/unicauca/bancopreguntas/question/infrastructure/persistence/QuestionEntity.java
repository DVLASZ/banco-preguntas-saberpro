package co.unicauca.bancopreguntas.question.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/** Fila de la tabla {@code question}. Es un detalle de persistencia: el dominio usa {@code Question}. */
@Entity
@Table(name = "question")
public class QuestionEntity {

    @Id
    @Column(length = 36)
    private String id;

    @Column(name = "author_id", nullable = false)
    private String authorId;

    @Column(nullable = false)
    private String nombre;

    @Column(nullable = false)
    private String contexto;

    @Column(nullable = false)
    private String enunciado;

    @Column(name = "opcion_a", nullable = false)
    private String opcionA;

    @Column(name = "opcion_b", nullable = false)
    private String opcionB;

    @Column(name = "opcion_c", nullable = false)
    private String opcionC;

    @Column(name = "opcion_d", nullable = false)
    private String opcionD;

    @Column(name = "respuesta_correcta", nullable = false, length = 1)
    private String respuestaCorrecta;

    @Column(nullable = false)
    private String justificacion;

    @Column(nullable = false)
    private String bibliografia;

    @Column(nullable = false)
    private String competencia;

    @Column(nullable = false)
    private String tema;

    @Column(nullable = false)
    private String subtema;

    @Column(nullable = false)
    private String dificultad;

    @Column(nullable = false)
    private String estado;

    @Column(name = "creada_en", nullable = false)
    private Instant creadaEn;

    @Column(name = "actualizada_en", nullable = false)
    private Instant actualizadaEn;

    @Column(name = "question_version", nullable = false)
    private int questionVersion = 1;

    protected QuestionEntity() {
        // requerido por JPA
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getAuthorId() { return authorId; }
    public void setAuthorId(String authorId) { this.authorId = authorId; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getContexto() { return contexto; }
    public void setContexto(String contexto) { this.contexto = contexto; }
    public String getEnunciado() { return enunciado; }
    public void setEnunciado(String enunciado) { this.enunciado = enunciado; }
    public String getOpcionA() { return opcionA; }
    public void setOpcionA(String opcionA) { this.opcionA = opcionA; }
    public String getOpcionB() { return opcionB; }
    public void setOpcionB(String opcionB) { this.opcionB = opcionB; }
    public String getOpcionC() { return opcionC; }
    public void setOpcionC(String opcionC) { this.opcionC = opcionC; }
    public String getOpcionD() { return opcionD; }
    public void setOpcionD(String opcionD) { this.opcionD = opcionD; }
    public String getRespuestaCorrecta() { return respuestaCorrecta; }
    public void setRespuestaCorrecta(String respuestaCorrecta) { this.respuestaCorrecta = respuestaCorrecta; }
    public String getJustificacion() { return justificacion; }
    public void setJustificacion(String justificacion) { this.justificacion = justificacion; }
    public String getBibliografia() { return bibliografia; }
    public void setBibliografia(String bibliografia) { this.bibliografia = bibliografia; }
    public String getCompetencia() { return competencia; }
    public void setCompetencia(String competencia) { this.competencia = competencia; }
    public String getTema() { return tema; }
    public void setTema(String tema) { this.tema = tema; }
    public String getSubtema() { return subtema; }
    public void setSubtema(String subtema) { this.subtema = subtema; }
    public String getDificultad() { return dificultad; }
    public void setDificultad(String dificultad) { this.dificultad = dificultad; }
    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
    public Instant getCreadaEn() { return creadaEn; }
    public void setCreadaEn(Instant creadaEn) { this.creadaEn = creadaEn; }
    public Instant getActualizadaEn() { return actualizadaEn; }
    public void setActualizadaEn(Instant actualizadaEn) { this.actualizadaEn = actualizadaEn; }
    public int getQuestionVersion() { return questionVersion; }
    public void setQuestionVersion(int questionVersion) { this.questionVersion = questionVersion; }
}
