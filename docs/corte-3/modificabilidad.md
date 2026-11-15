# Escenarios de modificabilidad

La modificabilidad es la facilidad para cambiar el sistema sin romper lo que ya funciona. Los requisitos RNF-10 a RNF-14 piden separar la lógica de negocio de
la interfaz y de la persistencia, bajo acoplamiento y poder incorporar nuevos tipos, criterios o módulos sin tocar mucho de lo existente.

Cada escenario sigue el formato del curso: fuente, estímulo, entorno, artefacto, respuesta, medida y resultado esperado. Los cambios que describen no se
hicieron, pero el diseño ya está preparado para ellos y cada escenario dice en qué clases se apoya.

## Escenario 1: cambiar la regla con la que se decide una revisión

| Elemento | Contenido |
|---|---|
| Fuente | Los docentes |
| Estímulo | Piden que una pregunta se apruebe por mayoría y no por unanimidad |
| Entorno | Sistema en operación, con revisiones en curso |
| Artefacto | `evaluation-service` |
| Respuesta | Se escribe una clase nueva, `Mayoria`, que implementa `PoliticaDeDecision`, y se cambia el objeto que arma `ApplicationConfig` |
| Medida | Clases nuevas, clases existentes modificadas y líneas cambiadas |
| Resultado esperado | Una clase nueva, una línea cambiada en la configuración y ninguna modificación en `CasoDeEvaluacion`, en los puertos ni en los adaptadores |

La razón es que el caso de evaluación recibe la política como parámetro (patrón Strategy) y no sabe cuál es. `UnanimidadTest` sirve de molde para la prueba de la nueva clase.

## Escenario 2: agregar un tipo de aviso

| Elemento | Contenido |
|---|---|
| Fuente | El administrador |
| Estímulo | Quiere un aviso nuevo, por ejemplo cuando una pregunta se archiva |
| Entorno | Desarrollo |
| Artefacto | `notification-service` |
| Respuesta | Una subclase de `CreadorDeAviso` (Factory Method), un valor en `TipoNotificacion` y su registro en `CreadoresDeAviso` |
| Medida | Clases existentes que cambian |
| Resultado esperado | El servicio de aplicación no cambia. `CreadoresDeAvisoTest` falla si un tipo queda sin creador, así que no se puede olvidar el registro |

## Escenario 3: agregar una regla de calidad a la validación

| Elemento | Contenido |
|---|---|
| Fuente | El equipo de calidad del banco |
| Estímulo | Quieren rechazar, por ejemplo, opciones demasiado parecidas entre sí |
| Entorno | Desarrollo |
| Artefacto | `validation-service` |
| Respuesta | Una clase nueva que extiende `ReglaPorOpcion` (Template Method) o implementa la regla de contenido, y su registro en la lista de reglas |
| Medida | Servicios que cambian |
| Resultado esperado | Solo cambia `validation-service`. `question-service` sigue llamándolo igual y recibe todas las violaciones juntas |

## Escenario 4: pasar del correo simulado al correo real

| Elemento | Contenido |
|---|---|
| Fuente | El docente |
| Estímulo | Pide enviar correos reales |
| Entorno | Despliegue |
| Artefacto | `email-service` |
| Respuesta | Una implementación nueva del puerto `EmailSender` que use SMTP. Los decoradores `EmailConReintentos` y `EmailConRegistro` siguen envolviéndola |
| Medida | Casos de uso modificados |
| Resultado esperado | Cero. Cambia el adaptador y la configuración del servicio. Los reintentos y el registro se conservan |

## Escenario 5: agregar un servicio que reaccione a lo que pasa en las revisiones

| Elemento | Contenido |
|---|---|
| Fuente | El equipo de desarrollo |
| Estímulo | Se quiere un servicio de estadísticas de revisión |
| Entorno | Desarrollo |
| Artefacto | RabbitMQ y los servicios existentes |
| Respuesta | El servicio nuevo declara su cola y se suscribe a los eventos `review.completed` y `question.state-changed` |
| Medida | Servicios existentes que hay que modificar |
| Resultado esperado | Ninguno. Así se sumaron `history-service` y `audit-service` sin tocar a quienes publican |

## Escenario 6: cambiar la base de datos de un servicio

| Elemento | Contenido |
|---|---|
| Fuente | El equipo de infraestructura |
| Estímulo | Un servicio debe pasar de PostgreSQL a otra base |
| Entorno | Desarrollo |
| Artefacto | Cualquier servicio con persistencia, por ejemplo `evaluation-service` |
| Respuesta | Se escribe otro adaptador de salida que implemente `EvaluationRepository` |
| Medida | Paquetes modificados |
| Resultado esperado | Solo cambia `adapter.out.persistence`. El dominio y la aplicación no se tocan. `ArquitecturaTest` falla si alguien rompe esa frontera |

## Cómo se protege

- Cada servicio tiene su propia base de datos y solo se comunica con otros por eventos o por una llamada REST puntual, así que un cambio no se propaga.
- ArchUnit comprueba en cada servicio que el dominio no dependa de Spring, de JPA ni de la mensajería. En `evaluation-service` comprueba además el hexágono completo.
- Las pruebas de cada clase del dominio avisan si un cambio rompe una regla. La cobertura está en [cobertura de pruebas](cobertura-de-pruebas.md).
- Los patrones que hacen posibles estos cambios están en [patrones y principios](patrones-y-principios.md).
