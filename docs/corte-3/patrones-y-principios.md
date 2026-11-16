# Patrones y principios del tercer corte

Los nueve patrones de diseño del curso ya están descritos, con sus clases y pruebas, en [patrones del segundo corte](../corte-2/05-patrones.md). Los docentes
confirmaron que en el tercer corte no se piden patrones GoF adicionales. Esta página dice qué cambió en este corte y qué patrones de arquitectura y de
dominio se suman, que son los que pide la arquitectura hexagonal con DDD.

## Los nueve patrones GoF, dónde siguen

| Tipo | Patrón | Servicio |
|---|---|---|
| Creacional | Builder | `question-service` |
| Creacional | Factory Method | `notification-service` |
| Estructural | Adapter | Todos los servicios, ahora más explícito en `evaluation-service` |
| Estructural | Facade | `api-gateway` |
| Estructural | Decorator | `email-service` |
| Comportamiento | Strategy | `evaluation-service` (`PoliticaDeDecision`) y `simulation-service` (`SelectorDePreguntas`, con `SeleccionAleatoria`) |
| Comportamiento | State | `lifecycle-service` |
| Comportamiento | Template Method | `validation-service` |
| Comportamiento | Observer | Eventos por RabbitMQ en todos los servicios |

El Strategy de los simulacros es de este corte: el estudiante pide preguntas al azar, y si mañana se quisiera otra manera de escoger, como priorizar las
que menos acierta, es una clase nueva que implementa `SelectorDePreguntas`.

## Patrones de arquitectura y de dominio

| Patrón | Qué resuelve | Dónde se ve |
|---|---|---|
| Puertos y adaptadores (hexagonal) | Que el negocio no dependa de la web, de la base de datos ni del broker | `evaluation-service`: `application.port.in`, `application.port.out` y `adapter` |
| Agregado y raíz del agregado | Que todas las reglas de un grupo de objetos pasen por un solo punto | `CasoDeEvaluacion` |
| Objeto de valor | Valores inmutables que validan al crearse y se comparan por sus datos | `Observacion`, `Asignacion`, `Evaluacion` |
| Servicio de dominio | Una regla que no pertenece a una sola entidad | `PoliticaDeDecision` y `Unanimidad` |
| Repositorio | Esconder cómo se guarda un agregado | `EvaluationRepository`, que implementa `JpaEvaluationRepository` |
| Evento de dominio | Avisar a otros servicios sin conocerlos | `EvaluationSubmitted` y `ReviewCompleted` |
| Outbox transaccional | Que el evento y el cambio se guarden juntos | `lifecycle-service` y `simulation-service` |
| Consumidor idempotente | Que un evento repetido no se procese dos veces | `ProcessedEvents` en los servicios que escuchan |
| API Gateway | Una sola entrada con seguridad, circuit breaker y reintentos | `api-gateway` |

## Principios SOLID, en lo que cambió

| Principio | Cómo se aplica en este corte |
|---|---|
| Responsabilidad única | `simulation-service` arma y califica simulacros; `tracking-service` solo acumula resultados y calcula estadísticas |
| Abierto y cerrado | Cambiar la política de decisión o la manera de escoger preguntas es una clase nueva, sin tocar las existentes |
| Sustitución de Liskov | `Unanimidad` cumple el contrato de `PoliticaDeDecision`; `SeleccionAleatoria` el de `SelectorDePreguntas` |
| Segregación de interfaces | `evaluation-service` tiene tres puertos de entrada pequeños (`EvaluarPregunta`, `ConsultarRevision` y `ReaccionarAEventosDeRevision`) en lugar de una interfaz grande |
| Inversión de dependencias | El dominio y la aplicación dependen de puertos; los adaptadores los implementan. `ArquitecturaTest` lo comprueba |

El detalle de cada decisión de diseño está en [decisiones y SOLID](../corte-2/10-decisiones-y-solid.md) y la forma del hexágono en [hexagonal y DDD](hexagonal-y-ddd.md).
