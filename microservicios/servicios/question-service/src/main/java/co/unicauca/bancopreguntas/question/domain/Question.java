package co.unicauca.bancopreguntas.question.domain;

import java.time.Instant;

/**
 * Entidad de dominio: una pregunta del banco de preguntas Saber Pro.
 *
 * <p>La entidad solo garantiza sus invariantes mínimas (identificador,
 * enunciado, opciones, respuesta A-D, estado, competencia, tema y
 * dificultad). Los demás campos de contenido (contexto, justificación,
 * bibliografía, subtema) y el autor son opcionales aquí: que una pregunta
 * los tenga completos lo exige el
 * {@link co.unicauca.bancopreguntas.question.domain.validation.QuestionValidator}
 * cuando el autor la guarda o la envía a revisión (HU03, RF-08 a RF-13).
 */
public class Question {

    private final String id;
    private final String nombre;
    private final String contexto;
    private final String enunciado;
    private final QuestionDistractors opciones;
    private final char respuestaCorrecta;
    private final String justificacion;
    private final String bibliografia;
    private EstadoPregunta estado;
    private final Competencia competencia;
    private final String tema;
    private final String subtema;
    private final Dificultad dificultad;
    private final String autor;
    private final Instant creadaEn;
    private final Instant actualizadaEn;
    private final int version;

    /** Constructor con los campos básicos; el contenido extendido queda vacío. */
    public Question(String id, String nombre, String enunciado, QuestionDistractors opciones,
                     char respuestaCorrecta, EstadoPregunta estado,
                     Competencia competencia, String tema, Dificultad dificultad) {
        this(id, nombre, "", enunciado, opciones, respuestaCorrecta, "", "", estado,
                competencia, tema, "", dificultad, "", null, null, 1);
    }

    private Question(String id, String nombre, String contexto, String enunciado,
                      QuestionDistractors opciones, char respuestaCorrecta, String justificacion,
                      String bibliografia, EstadoPregunta estado, Competencia competencia,
                      String tema, String subtema, Dificultad dificultad, String autor,
                      Instant creadaEn, Instant actualizadaEn, int version) {
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
        this.contexto = textoOVacio(contexto);
        this.enunciado = enunciado;
        this.opciones = opciones;
        this.respuestaCorrecta = letra;
        this.justificacion = textoOVacio(justificacion);
        this.bibliografia = textoOVacio(bibliografia);
        this.estado = estado;
        this.competencia = competencia;
        this.tema = tema;
        this.subtema = textoOVacio(subtema);
        this.dificultad = dificultad;
        this.autor = textoOVacio(autor);
        if (version < 1) {
            throw new IllegalArgumentException("La versión de la pregunta debe ser al menos 1");
        }
        this.creadaEn = creadaEn;
        this.actualizadaEn = actualizadaEn;
        this.version = version;
    }

    private static String textoOVacio(String texto) {
        return texto == null ? "" : texto;
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    /** Texto de contexto que antecede a la pregunta directa (vacío si no se diligenció). */
    public String getContexto() {
        return contexto;
    }

    /** La pregunta directa. */
    public String getEnunciado() {
        return enunciado;
    }

    public QuestionDistractors getOpciones() {
        return opciones;
    }

    public char getRespuestaCorrecta() {
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

    public String getSubtema() {
        return subtema;
    }

    public Dificultad getDificultad() {
        return dificultad;
    }

    /** Nombre de usuario del autor (vacío para preguntas sin autor, p. ej. las generadas por plugins). */
    public String getAutor() {
        return autor;
    }

    /** Momento de creación; {@code null} mientras la pregunta no se haya persistido. */
    public Instant getCreadaEn() {
        return creadaEn;
    }

    /** Momento de la última modificación; {@code null} mientras no se haya persistido. */
    public Instant getActualizadaEn() {
        return actualizadaEn;
    }

    /**
     * Número de versión del contenido: empieza en 1 y sube cada vez que el autor reabre la pregunta
     * rechazada para corregirla. Cada ronda de revisión corresponde a una versión.
     */
    public int getVersion() {
        return version;
    }

    /**
     * Devuelve una copia de la pregunta en el nuevo estado, si la transición es válida (RF-15).
     * La entidad original no se modifica.
     *
     * @throws OperacionNoPermitidaException si el estado actual no puede pasar al nuevo
     */
    public Question conEstado(EstadoPregunta nuevoEstado, Instant ahora) {
        if (!estado.puedePasarA(nuevoEstado)) {
            throw new OperacionNoPermitidaException(
                    "No se puede pasar una pregunta de " + estado + " a " + nuevoEstado);
        }
        return Question.builder()
                .id(id).nombre(nombre).contexto(contexto).enunciado(enunciado).opciones(opciones)
                .respuestaCorrecta(respuestaCorrecta).justificacion(justificacion)
                .bibliografia(bibliografia).estado(nuevoEstado).competencia(competencia)
                .tema(tema).subtema(subtema).dificultad(dificultad).autor(autor)
                .creadaEn(creadaEn).actualizadaEn(ahora).version(version)
                .build();
    }

    /**
     * Devuelve la pregunta rechazada al estado Borrador para que el autor la corrija, como una versión
     * nueva (la versión sube en uno). La entidad original no se modifica.
     *
     * @throws OperacionNoPermitidaException si la pregunta no está en un estado desde el que pueda volver a Borrador
     */
    public Question reabrir(Instant ahora) {
        if (!estado.puedePasarA(EstadoPregunta.BORRADOR)) {
            throw new OperacionNoPermitidaException(
                    "Solo se puede reabrir una pregunta Rechazada; esta está " + estado);
        }
        return Question.builder()
                .id(id).nombre(nombre).contexto(contexto).enunciado(enunciado).opciones(opciones)
                .respuestaCorrecta(respuestaCorrecta).justificacion(justificacion)
                .bibliografia(bibliografia).estado(EstadoPregunta.BORRADOR).competencia(competencia)
                .tema(tema).subtema(subtema).dificultad(dificultad).autor(autor)
                .creadaEn(creadaEn).actualizadaEn(ahora).version(version + 1)
                .build();
    }

    @Override
    public String toString() {
        return id + " - " + nombre;
    }

    /** Construye una pregunta con todos sus campos, sin la lista de 14 argumentos. */
    public static final class Builder {
        private String id;
        private String nombre;
        private String contexto = "";
        private String enunciado;
        private QuestionDistractors opciones;
        private char respuestaCorrecta;
        private String justificacion = "";
        private String bibliografia = "";
        private EstadoPregunta estado;
        private Competencia competencia;
        private String tema;
        private String subtema = "";
        private Dificultad dificultad;
        private String autor = "";
        private Instant creadaEn;
        private Instant actualizadaEn;
        private int version = 1;

        private Builder() {
        }

        public Builder id(String id) {
            this.id = id;
            return this;
        }

        public Builder nombre(String nombre) {
            this.nombre = nombre;
            return this;
        }

        public Builder contexto(String contexto) {
            this.contexto = contexto;
            return this;
        }

        public Builder enunciado(String enunciado) {
            this.enunciado = enunciado;
            return this;
        }

        public Builder opciones(QuestionDistractors opciones) {
            this.opciones = opciones;
            return this;
        }

        public Builder respuestaCorrecta(char respuestaCorrecta) {
            this.respuestaCorrecta = respuestaCorrecta;
            return this;
        }

        public Builder justificacion(String justificacion) {
            this.justificacion = justificacion;
            return this;
        }

        public Builder bibliografia(String bibliografia) {
            this.bibliografia = bibliografia;
            return this;
        }

        public Builder estado(EstadoPregunta estado) {
            this.estado = estado;
            return this;
        }

        public Builder competencia(Competencia competencia) {
            this.competencia = competencia;
            return this;
        }

        public Builder tema(String tema) {
            this.tema = tema;
            return this;
        }

        public Builder subtema(String subtema) {
            this.subtema = subtema;
            return this;
        }

        public Builder dificultad(Dificultad dificultad) {
            this.dificultad = dificultad;
            return this;
        }

        public Builder autor(String autor) {
            this.autor = autor;
            return this;
        }

        public Builder creadaEn(Instant creadaEn) {
            this.creadaEn = creadaEn;
            return this;
        }

        public Builder actualizadaEn(Instant actualizadaEn) {
            this.actualizadaEn = actualizadaEn;
            return this;
        }

        public Builder version(int version) {
            this.version = version;
            return this;
        }

        public Question build() {
            return new Question(id, nombre, contexto, enunciado, opciones, respuestaCorrecta,
                    justificacion, bibliografia, estado, competencia, tema, subtema, dificultad, autor,
                    creadaEn, actualizadaEn, version);
        }
    }
}
