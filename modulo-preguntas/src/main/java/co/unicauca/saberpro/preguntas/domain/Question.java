package co.unicauca.saberpro.preguntas.domain;

/**
 * Entidad de dominio: una pregunta del banco de preguntas Saber Pro.
 */
public class Question {

    private final String id;
    private final String nombre;
    private final String enunciado;
    private final QuestionDistractors opciones;
    private final char respuestaCorrecta;
    private EstadoPregunta estado;
    private final Competencia competencia;
    private final String tema;
    private final Dificultad dificultad;

    public Question(String id, String nombre, String enunciado, QuestionDistractors opciones,
                     char respuestaCorrecta, EstadoPregunta estado,
                     Competencia competencia, String tema, Dificultad dificultad) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("El id de la pregunta es obligatorio");
        }
        if (nombre == null || nombre.isBlank()) {
            throw new IllegalArgumentException("El nombre de la pregunta es obligatorio");
        }
        if (enunciado == null || enunciado.isBlank()) {
            throw new IllegalArgumentException("El enunciado de la pregunta es obligatorio");
        }
        if (opciones == null) {
            throw new IllegalArgumentException("Las opciones de la pregunta son obligatorias");
        }
        char letra = Character.toUpperCase(respuestaCorrecta);
        if (letra != 'A' && letra != 'B' && letra != 'C' && letra != 'D') {
            throw new IllegalArgumentException("La respuesta correcta debe ser A, B, C o D");
        }
        if (estado == null) {
            throw new IllegalArgumentException("El estado de la pregunta es obligatorio");
        }
        if (competencia == null) {
            throw new IllegalArgumentException("La competencia de la pregunta es obligatoria");
        }
        if (tema == null || tema.isBlank()) {
            throw new IllegalArgumentException("El tema de la pregunta es obligatorio");
        }
        if (dificultad == null) {
            throw new IllegalArgumentException("La dificultad de la pregunta es obligatoria");
        }
        this.id = id;
        this.nombre = nombre;
        this.enunciado = enunciado;
        this.opciones = opciones;
        this.respuestaCorrecta = letra;
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

    public QuestionDistractors getOpciones() {
        return opciones;
    }

    public char getRespuestaCorrecta() {
        return respuestaCorrecta;
    }

    public EstadoPregunta getEstado() {
        return estado;
    }

    public void setEstado(EstadoPregunta nuevoEstado) {
        if (nuevoEstado == null) {
            throw new IllegalArgumentException("El nuevo estado es obligatorio");
        }
        this.estado = nuevoEstado;
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

    @Override
    public String toString() {
        return id + " - " + nombre;
    }
}
