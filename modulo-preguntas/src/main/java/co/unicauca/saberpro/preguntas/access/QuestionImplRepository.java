package co.unicauca.saberpro.preguntas.access;

import co.unicauca.saberpro.preguntas.domain.Competencia;
import co.unicauca.saberpro.preguntas.domain.Dificultad;
import co.unicauca.saberpro.preguntas.domain.EstadoPregunta;
import co.unicauca.saberpro.preguntas.domain.Question;
import co.unicauca.saberpro.preguntas.domain.QuestionDistractors;
import co.unicauca.saberpro.preguntas.domain.QuestionRepository;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Implementación en memoria de {@link QuestionRepository}. Como indica la
 * guía del taller, para efectos del ejercicio no se requiere una base de
 * datos relacional: basta con una estructura simple (aquí, un mapa
 * id -&gt; Question).
 */
public class QuestionImplRepository implements QuestionRepository {

    private final Map<String, Question> preguntas = new LinkedHashMap<>();
    private int consecutivo = 0;

    public QuestionImplRepository() {
        cargarDatosDeEjemplo();
        consecutivo = preguntas.size();
    }

    @Override
    public List<Question> obtenerTodas() {
        return new ArrayList<>(preguntas.values());
    }

    @Override
    public Question obtenerPorId(String id) {
        return preguntas.get(id);
    }

    @Override
    public void actualizar(Question pregunta) {
        if (!preguntas.containsKey(pregunta.getId())) {
            throw new IllegalArgumentException("No existe una pregunta con id " + pregunta.getId());
        }
        preguntas.put(pregunta.getId(), pregunta);
    }

    @Override
    public void crear(Question pregunta) {
        if (preguntas.containsKey(pregunta.getId())) {
            throw new IllegalArgumentException("Ya existe una pregunta con id " + pregunta.getId());
        }
        preguntas.put(pregunta.getId(), pregunta);
    }

    @Override
    public String generarNuevoId() {
        consecutivo++;
        return String.format("P-%03d", consecutivo);
    }

    private void agregar(Question pregunta) {
        preguntas.put(pregunta.getId(), pregunta);
    }

    private void cargarDatosDeEjemplo() {
        agregar(new Question("P-001", "Pregunta sobre DDD",
                "¿Cuál es el objetivo principal de DDD?",
                new QuestionDistractors("Diseñar bases de datos", "Modelar el dominio del negocio",
                        "Eliminar UML", "Crear interfaces gráficas"),
                'B', EstadoPregunta.BORRADOR,
                Competencia.LECTURA_CRITICA, "Diseño de software", Dificultad.BASICO));
        agregar(new Question("P-002", "Principio de Responsabilidad Única",
                "¿Qué establece el principio SRP de SOLID?",
                new QuestionDistractors("Una clase debe tener una única razón para cambiar",
                        "Una clase debe implementar múltiples interfaces",
                        "Una clase no debe tener atributos privados",
                        "Una clase debe heredar de una sola clase abstracta"),
                'A', EstadoPregunta.BORRADOR,
                Competencia.RAZONAMIENTO_CUANTITATIVO, "Principios SOLID", Dificultad.BASICO));
        agregar(new Question("P-003", "Patrón Observer",
                "¿Qué relación existe entre un Subject y sus Observers?",
                new QuestionDistractors("El Subject hereda de cada Observer",
                        "El Subject notifica a los Observers suscritos cuando cambia su estado",
                        "Los Observers modifican directamente los atributos del Subject",
                        "Solo puede existir un Observer por Subject"),
                'B', EstadoPregunta.PENDIENTE_REVISION,
                Competencia.COMPETENCIAS_CIUDADANAS, "Patrones de diseño", Dificultad.INTERMEDIO));
        agregar(new Question("P-004", "Arquitectura en capas",
                "¿Cuál es la responsabilidad de la capa de acceso a datos?",
                new QuestionDistractors("Interactuar con el usuario", "Contener las reglas de negocio",
                        "Persistir y recuperar información", "Definir la interfaz gráfica"),
                'C', EstadoPregunta.EN_REVISION,
                Competencia.COMUNICACION_ESCRITA, "Arquitectura en capas", Dificultad.INTERMEDIO));
        agregar(new Question("P-005", "Micropatrón MVC",
                "¿Qué componente del patrón MVC actualiza la vista cuando cambia el modelo?",
                new QuestionDistractors("El controlador, directamente", "El propio modelo, mediante notificaciones",
                        "La vista, consultando la base de datos", "Un temporizador que refresca cada segundo"),
                'B', EstadoPregunta.APROBADA,
                Competencia.INGLES, "Patrones de diseño", Dificultad.INTERMEDIO));
        agregar(new Question("P-006", "Inversión de dependencias",
                "Según DIP, ¿de qué deben depender los módulos de alto nivel?",
                new QuestionDistractors("De módulos de bajo nivel concretos", "De abstracciones",
                        "De frameworks específicos", "De la base de datos directamente"),
                'B', EstadoPregunta.RECHAZADA,
                Competencia.LECTURA_CRITICA, "Principios SOLID", Dificultad.AVANZADO));
        agregar(new Question("P-007", "Pruebas de software Saber Pro",
                "¿Qué tipo de prueba aísla una unidad de código de sus dependencias?",
                new QuestionDistractors("Prueba de integración", "Prueba de aceptación",
                        "Prueba unitaria", "Prueba de carga"),
                'C', EstadoPregunta.PUBLICADA,
                Competencia.RAZONAMIENTO_CUANTITATIVO, "Pruebas de software", Dificultad.BASICO));
        agregar(new Question("P-008", "Patrón Factory",
                "¿Qué problema resuelve principalmente el patrón Factory?",
                new QuestionDistractors("Centraliza la creación de objetos sin exponer su clase concreta",
                        "Permite herencia múltiple en Java", "Evita el uso de interfaces",
                        "Reemplaza al patrón Observer"),
                'A', EstadoPregunta.ARCHIVADA,
                Competencia.COMPETENCIAS_CIUDADANAS, "Patrones de diseño", Dificultad.AVANZADO));

        // Preguntas ya publicadas adicionales: sin estas, el Docente no
        // tendria suficiente variedad (distintas competencias/dificultades)
        // para poder generar un simulacro de prueba (HU-12, RF-21/RF-22),
        // que solo puede usar preguntas en estado PUBLICADA.
        agregar(new Question("P-009", "Herencia y polimorfismo",
                "¿Qué permite el polimorfismo en la programación orientada a objetos?",
                new QuestionDistractors("Que una clase tenga un único metodo", "Invocar el mismo metodo con comportamientos distintos segun el objeto",
                        "Ocultar todos los atributos de una clase", "Evitar el uso de interfaces"),
                'B', EstadoPregunta.PUBLICADA,
                Competencia.LECTURA_CRITICA, "Herencia y polimorfismo", Dificultad.BASICO));
        agregar(new Question("P-010", "Principio de Segregación de Interfaces",
                "Según ISP, ¿qué deben evitar los clientes de una interfaz?",
                new QuestionDistractors("Depender de metodos que no usan", "Implementar mas de una interfaz",
                        "Usar clases abstractas", "Definir metodos estaticos"),
                'A', EstadoPregunta.PUBLICADA,
                Competencia.RAZONAMIENTO_CUANTITATIVO, "Principios SOLID", Dificultad.INTERMEDIO));
        agregar(new Question("P-011", "Patrón Strategy",
                "¿Qué problema resuelve el patrón Strategy?",
                new QuestionDistractors("Permite intercambiar algoritmos en tiempo de ejecucion",
                        "Centraliza la creacion de objetos", "Sincroniza hilos de ejecucion",
                        "Convierte una interfaz en otra"),
                'A', EstadoPregunta.PUBLICADA,
                Competencia.COMPETENCIAS_CIUDADANAS, "Patrones de diseño", Dificultad.AVANZADO));
        agregar(new Question("P-012", "Persistencia con SQLite",
                "¿Qué ventaja ofrece usar SQLite en una aplicacion de escritorio pequeña?",
                new QuestionDistractors("No requiere instalar un servidor de base de datos aparte",
                        "Es exclusivo de aplicaciones web", "Reemplaza la necesidad de un ORM",
                        "Solo funciona en la nube"),
                'A', EstadoPregunta.PUBLICADA,
                Competencia.COMUNICACION_ESCRITA, "Persistencia de datos", Dificultad.BASICO));
    }
}
