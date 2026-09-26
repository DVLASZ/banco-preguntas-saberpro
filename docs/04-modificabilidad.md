# Atributos de calidad y escenario de modificabilidad

## 1. Atributos de calidad relevantes para esta iteración

| Atributo | Por qué importa en el banco de preguntas | Cómo se atiende |
|---|---|---|
| **Modificabilidad** (principal) | Las reglas de validación, los estados de una pregunta y las formas de notificar cambian con el uso; además el proyecto migrará a microservicios | Módulos Maven con dependencias en una sola dirección; interfaces (puertos) entre capas; una regla de validación por clase; inyección de dependencias en un único punto (`MainApp`) |
| **Testabilidad** | La rúbrica exige pruebas unitarias a las clases del dominio y las vistas Swing son difíciles de probar | El dominio no depende de Swing; los controladores hablan con la vista por una interfaz y se prueban con una vista falsa; los repositorios y notificadores se reemplazan por dobles de prueba |
| **Usabilidad** | Los autores redactan preguntas largas y no deben perder trabajo ni equivocarse de estado | Estados con color, campos en rojo, confirmaciones antes de enviar o descartar, formulario de solo lectura fuera de Borrador |

## 2. Escenario de calidad de modificabilidad

**Escenario principal: agregar una regla de validación estructural.**

| Elemento | Descripción |
|---|---|
| **Fuente del estímulo** | El coordinador del banco de preguntas (cliente) |
| **Estímulo** | Solicita una regla nueva de validación estructural: "la pregunta directa no debe superar 300 caracteres" |
| **Artefacto** | El módulo de validación estructural (`modulo-preguntas`, paquete `domain.validation`) |
| **Entorno** | Tiempo de desarrollo; el sistema ya está funcionando y las reglas RF-08 a RF-13 están implementadas |
| **Respuesta** | El desarrollador crea una clase que implementa `ValidationRule` con la nueva regla y la agrega a la lista de `QuestionValidator.porDefecto()`. La regla se aplica automáticamente al guardar y al enviar a revisión, tanto en la aplicación de escritorio como en el servicio REST |
| **Medida de la respuesta** | Se crea **1 clase** (unas 15 líneas) y se modifica **1 línea**; se modifican **0 líneas** de `QuestionService`, de los controladores y de las vistas; el cambio y su prueba toman **menos de una hora**; las pruebas existentes siguen en verde |
| **Resultado esperado** | La regla nueva funciona en todos los puntos de entrada sin tocar la lógica de negocio ni la interfaz, y el resto del sistema no se ve afectado |

**Por qué se puede cumplir.** `QuestionValidator` recibe una lista de reglas y las
ejecuta todas; no conoce ninguna regla concreta. `QuestionService` solo llama al
validador, y la interfaz solo muestra las violaciones que este devuelve. Las siete
reglas actuales ya se construyeron así, cada una como una clase independiente, sin
modificar el servicio.

```java
// Esbozo de la regla nueva: 1 clase...
public class LongitudMaximaEnunciadoRule implements ValidationRule {
    @Override
    public List<Violacion> validar(ContenidoPregunta c) {
        if (c.enunciado() != null && c.enunciado().length() > 300) {
            return List.of(new Violacion("enunciado", "La pregunta directa no puede superar 300 caracteres"));
        }
        return List.of();
    }
}
// ...y 1 línea en QuestionValidator.porDefecto():  new LongitudMaximaEnunciadoRule()
```

## 3. Otros escenarios que la arquitectura también soporta

| Cambio solicitado | Qué se toca | Qué NO se toca | Evidencia |
|---|---|---|---|
| Guardar las preguntas en una base de datos en vez de en memoria | Una clase nueva que implemente `QuestionRepository` y la línea donde se arma en `MainApp` | `QuestionService`, `Question`, los controladores y las vistas | Ya ocurrió en el Taller 6: `QuestionJpaAdapter` (JPA + H2) implementa el mismo puerto `QuestionRepository` y reutiliza el dominio sin cambiarlo |
| Enviar el correo de asignación de revisores por SMTP real en vez del simulado | Una clase nueva que implemente `NotificadorAsignacion` y una línea en `MainApp` | `AsignacionRevisionService` | `AsignacionRevisionService` depende de la interfaz `NotificadorAsignacion`, no de `NotificadorCorreoSimulado` |
| Que el Revisor vea solo las preguntas que le asignaron | Cambiar la fuente de preguntas que se le entrega a `GUIRevisor` | La ventana ni el controlador del Revisor | `FuenteDePreguntasParaRevisar` (Strategy) permite cambiar la fuente sin tocar `RevisionController` |
| Agregar un tipo de pregunta nuevo (por ejemplo, con imagen) | Un plugin nuevo y una línea en `plugins.properties` | El núcleo del microkernel | Arquitectura Microkernel del Taller 5: los plugins se cargan por reflexión |

## 4. Tácticas de modificabilidad aplicadas

| Táctica | Dónde se aplica |
|---|---|
| Aumentar la cohesión (una responsabilidad por clase) | Una regla de validación por clase; un controlador por ventana; `EstadoPregunta` concentra las transiciones válidas |
| Reducir el acoplamiento con interfaces | `QuestionRepository`, `SimulacroRepository`, `AsignacionRevisionRepository`, `NotificadorAsignacion`, `DirectorioRevisores`, `RedaccionPreguntaVista`, `RevisionVista` |
| Restringir las dependencias | Módulos Maven: `modulo-preguntas` y `modulo-usuarios` no dependen de nadie; el grafo no tiene ciclos y `app` es el único que conoce a todos |
| Diferir el enlace | Composition root (`MainApp`) que arma las dependencias; plugins cargados por reflexión desde un archivo de propiedades |
