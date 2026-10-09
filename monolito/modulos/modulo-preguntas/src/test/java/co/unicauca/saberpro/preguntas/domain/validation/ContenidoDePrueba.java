package co.unicauca.saberpro.preguntas.domain.validation;

import co.unicauca.saberpro.preguntas.domain.Competencia;
import co.unicauca.saberpro.preguntas.domain.ContenidoPregunta;
import co.unicauca.saberpro.preguntas.domain.Dificultad;

/** Contenido de pregunta que cumple toda la validación estructural, modificable campo a campo. */
public final class ContenidoDePrueba {

    private String nombre = "Patrón Observer";
    private String contexto = "Una aplicación debe actualizar varias ventanas cuando cambian los datos de una pregunta.";
    private String enunciado = "¿Qué patrón notifica a varios objetos cuando cambia el estado de otro?";
    private String opcionA = "Factory";
    private String opcionB = "Observer";
    private String opcionC = "Singleton";
    private String opcionD = "Adapter";
    private String respuesta = "B";
    private String justificacion = "Observer define una dependencia uno a muchos para notificar los cambios de estado.";
    private String bibliografia = "Gamma, E. et al. (1994). Design Patterns. Addison-Wesley.";
    private Competencia competencia = Competencia.LECTURA_CRITICA;
    private String tema = "Patrones de diseño";
    private String subtema = "Observer";
    private Dificultad dificultad = Dificultad.INTERMEDIO;

    private ContenidoDePrueba() {
    }

    public static ContenidoDePrueba valido() {
        return new ContenidoDePrueba();
    }

    public ContenidoDePrueba nombre(String valor) {
        this.nombre = valor;
        return this;
    }

    public ContenidoDePrueba contexto(String valor) {
        this.contexto = valor;
        return this;
    }

    public ContenidoDePrueba enunciado(String valor) {
        this.enunciado = valor;
        return this;
    }

    public ContenidoDePrueba opcionA(String valor) {
        this.opcionA = valor;
        return this;
    }

    public ContenidoDePrueba opcionB(String valor) {
        this.opcionB = valor;
        return this;
    }

    public ContenidoDePrueba opcionC(String valor) {
        this.opcionC = valor;
        return this;
    }

    public ContenidoDePrueba opcionD(String valor) {
        this.opcionD = valor;
        return this;
    }

    public ContenidoDePrueba respuesta(String valor) {
        this.respuesta = valor;
        return this;
    }

    public ContenidoDePrueba justificacion(String valor) {
        this.justificacion = valor;
        return this;
    }

    public ContenidoDePrueba bibliografia(String valor) {
        this.bibliografia = valor;
        return this;
    }

    public ContenidoDePrueba competencia(Competencia valor) {
        this.competencia = valor;
        return this;
    }

    public ContenidoDePrueba tema(String valor) {
        this.tema = valor;
        return this;
    }

    public ContenidoDePrueba subtema(String valor) {
        this.subtema = valor;
        return this;
    }

    public ContenidoDePrueba dificultad(Dificultad valor) {
        this.dificultad = valor;
        return this;
    }

    public ContenidoPregunta build() {
        return new ContenidoPregunta(nombre, contexto, enunciado, opcionA, opcionB, opcionC, opcionD,
                respuesta, justificacion, bibliografia, competencia, tema, subtema, dificultad);
    }
}
