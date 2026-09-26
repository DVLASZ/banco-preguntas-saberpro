# Módulo Microkernel (Taller 5)

Arquitectura Microkernel (Plug-in) combinada con el patrón Tuberías y
Filtros para poder incorporar nuevos tipos de pregunta al banco de
preguntas sin modificar el núcleo del sistema.

## Patrón Microkernel

`QuestionMicrokernel` administra el banco de preguntas y carga los
plugins dinámicamente **por Reflexión**
(`Class.forName(...).getDeclaredConstructor().newInstance()`), leyendo
sus nombres de clase desde `src/main/resources/plugins.properties` — el
mismo mecanismo del ejemplo visto en clase sobre el envío de paquetes a
distintos países (`DeliveryPluginManager`).

A diferencia del ejemplo del taller (que usa una entidad `Question` de
juguete), este módulo reutiliza la **`Question` real** de
`modulo-preguntas`: cada pregunta generada se registra a través de
`QuestionService.registrarPreguntaGenerada(...)`, así queda en el mismo
banco que usan el Autor, el Revisor y el Docente, y el patrón Observer
del proyecto (vistas de estadísticas/gráfica) sigue funcionando también
para las preguntas creadas por un plugin.

Contrato que deben cumplir todos los plugins:

```java
public interface QuestionPlugin {
    String getName();
    boolean supports(String type);
    Question generate(QuestionRequest request);
}
```

Plugins registrados (`plugins.properties`):

| Plugin | Tipo (`supports`) | Qué hace |
|---|---|---|
| `MultipleChoiceQuestionPlugin` | `MULTIPLE_CHOICE` | Selección múltiple estándar |
| `CaseQuestionPlugin` | `CASO` | Antepone "Caso de estudio:" al enunciado |
| `MultimediaQuestionPlugin` | `MULTIMEDIA` | Referencia un recurso (URL/ruta) en el enunciado |

Los tres extienden `BaseQuestionPlugin` (Template Method): esa clase
centraliza la ejecución del pipeline de validación y la construcción de
la `Question` real; cada subclase solo decide su `getName()`, qué `type`
soporta, y cómo redactar el enunciado final.

## Patrón Tuberías y Filtros

Antes de construir la pregunta, `BaseQuestionPlugin` ejecuta un
`QuestionPipeline` con cuatro filtros (`QuestionFilter`), en orden, que
se detiene en el primero que falla:

1. `ContentValidationFilter` — título y enunciado no vacíos.
2. `OptionsValidationFilter` — exactamente 4 opciones, todas con contenido.
3. `ClassificationFilter` — la competencia debe ser una de las reales del banco.
4. `CorrectAnswerValidationFilter` — la respuesta correcta debe estar entre las opciones.

## Interfaz gráfica

`GUIMicrokernel` (Swing) se abre junto a la ventana habitual del Autor
de Preguntas: permite elegir el tipo, diligenciar el formulario y
generar la pregunta a través del microkernel.

## Pruebas

39 pruebas (JUnit 5 + Mockito): los 4 filtros, el pipeline, la solicitud de
generación, la clase base de los plugins (Template Method), los 3 plugins y
el núcleo (incluida la carga real de los plugins por Reflexión).

```bash
mvn test -pl modulo-microkernel -am
```
