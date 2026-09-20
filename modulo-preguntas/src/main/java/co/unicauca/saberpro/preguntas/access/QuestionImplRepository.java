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

    private static final String BIBLIOGRAFIA_GAMMA =
            "Gamma, E., Helm, R., Johnson, R. y Vlissides, J. (1994). Design Patterns: Elements of "
                    + "Reusable Object-Oriented Software. Addison-Wesley.";
    private static final String BIBLIOGRAFIA_MARTIN =
            "Martin, R. C. (2017). Clean Architecture: A Craftsman's Guide to Software Structure and "
                    + "Design. Prentice Hall.";

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

    private void agregar(String id, String nombre, String contexto, String enunciado,
                          String opcionA, String opcionB, String opcionC, String opcionD,
                          char respuestaCorrecta, String justificacion, String bibliografia,
                          EstadoPregunta estado, Competencia competencia, String tema, String subtema,
                          Dificultad dificultad, String autor) {
        preguntas.put(id, Question.builder()
                .id(id)
                .nombre(nombre)
                .contexto(contexto)
                .enunciado(enunciado)
                .opciones(new QuestionDistractors(opcionA, opcionB, opcionC, opcionD))
                .respuestaCorrecta(respuestaCorrecta)
                .justificacion(justificacion)
                .bibliografia(bibliografia)
                .estado(estado)
                .competencia(competencia)
                .tema(tema)
                .subtema(subtema)
                .dificultad(dificultad)
                .autor(autor)
                .build());
    }

    /**
     * Preguntas de ejemplo con el contenido completo que exige la validación
     * estructural. Las del primer bloque (P-001 a P-006) son del Autor
     * {@code autor1}, en distintos estados; las del segundo (P-007 a P-012)
     * son de {@code autor2} y las publicadas alimentan los simulacros (HU-12).
     */
    private void cargarDatosDeEjemplo() {
        agregar("P-001", "Pregunta sobre DDD",
                "Un equipo de desarrollo debe construir el sistema de un hospital con reglas de negocio "
                        + "complejas y cambiantes, y necesita organizar el código para que refleje el lenguaje "
                        + "de los expertos del negocio.",
                "¿Cuál es el objetivo principal de DDD?",
                "Diseñar bases de datos", "Modelar el dominio del negocio", "Eliminar UML",
                "Crear interfaces gráficas", 'B',
                "DDD propone que el software modele el dominio del negocio con el mismo lenguaje de los "
                        + "expertos; por eso la opción B es la correcta y las demás describen tareas que no "
                        + "son su objetivo.",
                "Evans, E. (2003). Domain-Driven Design: Tackling Complexity in the Heart of Software. "
                        + "Addison-Wesley.",
                EstadoPregunta.BORRADOR, Competencia.LECTURA_CRITICA, "Diseño de software",
                "Diseño guiado por el dominio", Dificultad.BASICO, "autor1");
        agregar("P-002", "Principio de Responsabilidad Única",
                "En una revisión de código se encuentra una clase que valida datos, calcula totales y además "
                        + "guarda información en la base de datos, y cada cambio en cualquiera de esas tareas "
                        + "obliga a modificarla.",
                "¿Qué establece el principio SRP de SOLID?",
                "Una clase debe tener una única razón para cambiar",
                "Una clase debe implementar múltiples interfaces",
                "Una clase no debe tener atributos privados",
                "Una clase debe heredar de una sola clase abstracta", 'A',
                "SRP indica que una clase debe tener una única razón para cambiar; la clase del contexto "
                        + "mezcla varias responsabilidades, y por eso el principio se cumple con la opción A.",
                BIBLIOGRAFIA_MARTIN,
                EstadoPregunta.BORRADOR, Competencia.RAZONAMIENTO_CUANTITATIVO, "Principios SOLID",
                "Responsabilidad única", Dificultad.BASICO, "autor1");
        agregar("P-003", "Patrón Observer",
                "Una aplicación de escritorio tiene una ventana de estadísticas y una gráfica que deben "
                        + "actualizarse cada vez que cambia el estado de una pregunta, sin que el servicio "
                        + "conozca los detalles de cada ventana.",
                "¿Qué relación existe entre un Subject y sus Observers?",
                "El Subject hereda de cada Observer",
                "El Subject notifica a los Observers suscritos cuando cambia su estado",
                "Los Observers modifican directamente los atributos del Subject",
                "Solo puede existir un Observer por Subject", 'B',
                "En el patrón Observer el Subject mantiene una lista de observadores suscritos y los notifica "
                        + "cuando cambia su estado, lo que corresponde a la opción B.",
                BIBLIOGRAFIA_GAMMA,
                EstadoPregunta.PENDIENTE_REVISION, Competencia.COMPETENCIAS_CIUDADANAS, "Patrones de diseño",
                "Observer", Dificultad.INTERMEDIO, "autor1");
        agregar("P-004", "Arquitectura en capas",
                "Un sistema separa su código en presentación, dominio y acceso a datos para que cada parte "
                        + "pueda cambiarse sin afectar a las demás.",
                "¿Cuál es la responsabilidad de la capa de acceso a datos?",
                "Interactuar con el usuario", "Contener las reglas de negocio",
                "Persistir y recuperar información", "Definir la interfaz gráfica", 'C',
                "La capa de acceso a datos se encarga de guardar y recuperar la información, sin reglas de "
                        + "negocio ni interfaz, por lo que la respuesta es la opción C.",
                "Bass, L., Clements, P. y Kazman, R. (2021). Software Architecture in Practice (4.ª ed.). "
                        + "Addison-Wesley.",
                EstadoPregunta.EN_REVISION, Competencia.COMUNICACION_ESCRITA, "Arquitectura en capas",
                "Capa de acceso a datos", Dificultad.INTERMEDIO, "autor1");
        agregar("P-005", "Micropatrón MVC",
                "En una aplicación con arquitectura MVC, la vista debe mostrar siempre los datos actuales del "
                        + "modelo sin consultarlo de forma permanente.",
                "¿Qué componente del patrón MVC actualiza la vista cuando cambia el modelo?",
                "El controlador, directamente", "El propio modelo, mediante notificaciones",
                "La vista, consultando la base de datos", "Un temporizador que refresca cada segundo", 'B',
                "En MVC el modelo notifica sus cambios y la vista se actualiza a partir de esas "
                        + "notificaciones, por eso la opción correcta es la B.",
                "Fowler, M. (2002). Patterns of Enterprise Application Architecture. Addison-Wesley.",
                EstadoPregunta.APROBADA, Competencia.INGLES, "Patrones de diseño",
                "Modelo-Vista-Controlador", Dificultad.INTERMEDIO, "autor1");
        agregar("P-006", "Inversión de dependencias",
                "Un módulo de reportes crea directamente una conexión a una base de datos concreta, y cambiar "
                        + "de motor obliga a reescribir el módulo.",
                "Según DIP, ¿de qué deben depender los módulos de alto nivel?",
                "De módulos de bajo nivel concretos", "De abstracciones", "De frameworks específicos",
                "De la base de datos directamente", 'B',
                "El principio de inversión de dependencias indica que los módulos de alto nivel deben "
                        + "depender de abstracciones y no de implementaciones concretas, como dice la opción B.",
                BIBLIOGRAFIA_MARTIN,
                EstadoPregunta.RECHAZADA, Competencia.LECTURA_CRITICA, "Principios SOLID",
                "Inversión de dependencias", Dificultad.AVANZADO, "autor1");

        agregar("P-007", "Pruebas de software Saber Pro",
                "Un equipo quiere verificar una clase de cálculo de notas sin depender de la base de datos ni "
                        + "de la interfaz gráfica.",
                "¿Qué tipo de prueba aísla una unidad de código de sus dependencias?",
                "Prueba de integración", "Prueba de aceptación", "Prueba unitaria", "Prueba de carga", 'C',
                "Una prueba unitaria aísla una unidad de código de sus dependencias para verificar su "
                        + "comportamiento, que es lo que describe la opción C.",
                "Beck, K. (2002). Test Driven Development: By Example. Addison-Wesley.",
                EstadoPregunta.PUBLICADA, Competencia.RAZONAMIENTO_CUANTITATIVO, "Pruebas de software",
                "Pruebas unitarias", Dificultad.BASICO, "autor2");
        agregar("P-008", "Patrón Factory",
                "Un programa necesita crear distintos tipos de objetos sin que el código que los usa conozca "
                        + "su clase concreta.",
                "¿Qué problema resuelve principalmente el patrón Factory?",
                "Centraliza la creación de objetos sin exponer su clase concreta",
                "Permite herencia múltiple en Java", "Evita el uso de interfaces",
                "Reemplaza al patrón Observer", 'A',
                "El patrón Factory centraliza la creación de objetos y oculta su clase concreta, como indica "
                        + "la opción A.",
                BIBLIOGRAFIA_GAMMA,
                EstadoPregunta.ARCHIVADA, Competencia.COMPETENCIAS_CIUDADANAS, "Patrones de diseño",
                "Factory", Dificultad.AVANZADO, "autor2");

        // Preguntas ya publicadas adicionales: sin estas, el Docente no
        // tendria suficiente variedad (distintas competencias/dificultades)
        // para poder generar un simulacro de prueba (HU-12, RF-21/RF-22),
        // que solo puede usar preguntas en estado PUBLICADA.
        agregar("P-009", "Herencia y polimorfismo",
                "Un programa de dibujo tiene una jerarquía de figuras y recorre una lista de ellas llamando "
                        + "siempre al mismo método, y obtiene un dibujo distinto según la figura.",
                "¿Qué permite el polimorfismo en la programación orientada a objetos?",
                "Que una clase tenga un único metodo",
                "Invocar el mismo metodo con comportamientos distintos segun el objeto",
                "Ocultar todos los atributos de una clase", "Evitar el uso de interfaces", 'B',
                "El polimorfismo permite invocar el mismo método con comportamientos distintos según el "
                        + "objeto, como describe la opción B.",
                "Deitel, P. y Deitel, H. (2016). Java: cómo programar (10.ª ed.). Pearson.",
                EstadoPregunta.PUBLICADA, Competencia.LECTURA_CRITICA, "Herencia y polimorfismo",
                "Polimorfismo", Dificultad.BASICO, "autor2");
        agregar("P-010", "Principio de Segregación de Interfaces",
                "Una interfaz con muchos métodos obliga a sus clientes a implementar operaciones que nunca "
                        + "usan.",
                "Según ISP, ¿qué deben evitar los clientes de una interfaz?",
                "Depender de metodos que no usan", "Implementar mas de una interfaz",
                "Usar clases abstractas", "Definir metodos estaticos", 'A',
                "ISP establece que los clientes no deben depender de métodos que no usan, lo que corresponde "
                        + "a la opción A.",
                BIBLIOGRAFIA_MARTIN,
                EstadoPregunta.PUBLICADA, Competencia.RAZONAMIENTO_CUANTITATIVO, "Principios SOLID",
                "Segregación de interfaces", Dificultad.INTERMEDIO, "autor2");
        agregar("P-011", "Patrón Strategy",
                "Un sistema de envíos debe calcular el costo de forma distinta según el país, y se quiere "
                        + "poder agregar nuevas formas de cálculo sin modificar el código existente.",
                "¿Qué problema resuelve el patrón Strategy?",
                "Permite intercambiar algoritmos en tiempo de ejecucion", "Centraliza la creacion de objetos",
                "Sincroniza hilos de ejecucion", "Convierte una interfaz en otra", 'A',
                "Strategy encapsula algoritmos intercambiables y permite elegirlos en tiempo de ejecución, "
                        + "por lo que la respuesta es la opción A.",
                BIBLIOGRAFIA_GAMMA,
                EstadoPregunta.PUBLICADA, Competencia.COMPETENCIAS_CIUDADANAS, "Patrones de diseño",
                "Strategy", Dificultad.AVANZADO, "autor2");
        agregar("P-012", "Persistencia con SQLite",
                "Un equipo desarrolla una aplicación de escritorio pequeña y no quiere que sus usuarios "
                        + "instalen ni administren un servidor de base de datos.",
                "¿Qué ventaja ofrece usar SQLite en una aplicacion de escritorio pequeña?",
                "No requiere instalar un servidor de base de datos aparte", "Es exclusivo de aplicaciones web",
                "Reemplaza la necesidad de un ORM", "Solo funciona en la nube", 'A',
                "SQLite es una base de datos embebida que no requiere un servidor aparte, como señala la "
                        + "opción A.",
                "Owens, M. y Allen, G. (2010). The Definitive Guide to SQLite (2.ª ed.). Apress.",
                EstadoPregunta.PUBLICADA, Competencia.COMUNICACION_ESCRITA, "Persistencia de datos",
                "Bases de datos embebidas", Dificultad.BASICO, "autor2");
    }
}
