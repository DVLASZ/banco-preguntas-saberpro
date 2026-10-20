# 05 · Patrones de diseño

La rúbrica pide al menos 6 patrones GoF: 2 creacionales, 2 estructurales y 2 de comportamiento. Según los docentes
(consulta 26) los patrones se acumulan entre las iteraciones y pueden aplicarse en cualquier microservicio o en el
frontend. Esta tabla recoge los **nueve** que están implementados en el código de los microservicios (seis cubren la
rúbrica y tres más completan el catálogo); cada fila indica las clases reales y las pruebas que lo respaldan.

| Tipo | Patrón | Dónde (clases reales) | Servicio |
|---|---|---|---|
| Creacional | **Builder** | `Question.Builder` | `question-service` |
| Creacional | **Factory Method** | `CreadorDeAviso` (creador) y sus ocho subclases (`AvisoRevisoresAsignados`, `AvisoPreguntaEnRevision`, `AvisoEvaluacionRecibida`, `AvisoRevisionAprobada`, `AvisoRevisionRechazada`, `AvisoResultadoDeRevision`, `AvisoObservacionTardia`, `AvisoPreguntaPublicada`); registro `CreadoresDeAviso` | `notification-service` |
| Estructural | **Adapter** | Puertos y adaptadores: `JpaQuestionRepository`, `JpaLifecycleRepository`, `JpaAssignmentRepository`, `JpaEvaluationRepository`, `JpaNotificationRepository`; mensajería (`LifecycleEventPublisher`, `AssignmentEventPublisher`, `EvaluationEventPublisher`, `UserEventPublisher`); clientes REST de otros servicios (`ValidadorRemoto`, `CicloDeVidaRemoto`, `CatalogoRemoto`); correo (`SimulatedEmailSender` detrás del puerto `EmailSender`) | todos |
| Estructural | **Facade** | `api-gateway` (Spring Cloud Gateway): una sola entrada que oculta la topología interna y centraliza token, autorización, circuit breaker y CORS | `api-gateway` |
| Comportamiento | **Strategy** | `PoliticaDeDecision` con la estrategia `Unanimidad` (`evaluation-service`) y `SelectorDePreguntas` con `SeleccionAleatoria` (`catalog-service`); cada servicio la recibe por constructor (`ApplicationConfig`) | `evaluation-service`, `catalog-service` |
| Comportamiento | **State** | `FaseDePregunta` y sus siete fases (`FaseBorrador`, `FasePendienteDeRevision`, `FaseEnRevision`, `FaseAprobada`, `FaseRechazada`, `FasePublicada`, `FaseArchivada`) | `lifecycle-service` |
| Comportamiento | **Template Method** | `ReglaPorOpcion` con las reglas `ExpresionesProhibidasRule` y `LongitudYEstructuraOpcionesRule` | `validation-service` |
| Estructural | **Decorator** | `EmailConReintentos` y `EmailConRegistro` decoran el puerto `EmailSender` | `email-service` |
| Comportamiento | **Observer** | Publicación y suscripción de eventos por RabbitMQ: los servicios publican (`AmqpEventPublisher`, `OutboxEventPublisher`) y los demás se suscriben con `@RabbitListener` (`EventsListener`) | todos |

## Detalle de cada patrón

### Builder · `Question.Builder`

- **Problema:** una pregunta tiene más de quince campos (contexto, enunciado, cuatro opciones, justificación,
  competencia, tema, estado, versión, fechas…). Un constructor con tantos parámetros es ilegible y propenso a errores.
- **Solución:** `Question.builder()` arma el objeto campo por campo y valida al construir. `Question` es inmutable:
  cada cambio de estado (`conEstado`, `reabrir`) devuelve una instancia nueva.
- **Prueba:** `QuestionTransicionTest`, `QuestionTest`.

### Factory Method · `CreadorDeAviso`

- **Problema:** cada evento genera un aviso distinto (asignación a un revisor, aprobación, rechazo, observación
  tardía, publicación) con su propio asunto y cuerpo. Un `if` por tipo dentro del servicio de aplicación lo obliga a
  conocer todos los textos y cambiar cada vez que se agrega uno.
- **Solución:** `CreadorDeAviso` es la clase creadora. Define la operación `crear(...)`, que arma la `Notificacion`,
  y declara el **método fábrica** `crearMensaje(ContextoDeAviso)`, que cada subclase implementa para fabricar su
  `Mensaje`. `CreadoresDeAviso.para(tipo)` entrega el creador que corresponde a cada `TipoNotificacion`.
  `NotificationApplicationService` no conoce ninguna clase concreta.
- **Para agregar un tipo de aviso nuevo:** una subclase, un valor en `TipoNotificacion` y su registro; el servicio
  de aplicación no cambia (principio abierto/cerrado).
- **Prueba:** `CreadoresDeAvisoTest` (17 pruebas, entre ellas una por cada tipo de notificación, que falla si falta
  un creador).

### Adapter · puertos y adaptadores

- **Problema:** el dominio y los casos de uso no deben depender de PostgreSQL, RabbitMQ ni del servidor de correo.
- **Solución:** cada servicio declara puertos en `application/port` (`QuestionRepository`, `NotificationRepository`,
  `EmailSender`, `DomainEventPublisher`…) y los adapta en `infrastructure`: JPA, RabbitMQ y el envío de correo. El correo es
  simulado porque así lo decidió el docente (consulta 16); si se pidiera correo real solo cambia el adaptador.
- **Prueba:** pruebas de persistencia sobre PostgreSQL real, de mensajería y `ArquitecturaTest` (ArchUnit), que
  falla si el dominio depende de Spring, de JPA o de las capas externas.

### Facade · `api-gateway`

- **Problema:** el frontend no debe conocer catorce servicios, sus direcciones ni sus réplicas.
- **Solución:** el gateway expone una sola interfaz bajo `/api`, resuelve los servicios por Eureka, valida el JWT,
  aplica la tabla de roles y protege con circuit breaker, reintentos y bulkhead (documento 08).
- **Prueba:** las 22 pruebas de `api-gateway`.

### Strategy · `PoliticaDeDecision`

- **Problema:** hoy una pregunta se aprueba por unanimidad; mañana podría pedirse mayoría u otra regla.
- **Solución:** `PoliticaDeDecision.decidir(revisores, evaluaciones)` es la interfaz y `Unanimidad` la estrategia
  vigente (con un rechazo se cierra la ronda). `CasoDeEvaluacion` usa la interfaz, no la clase.
- **Prueba:** `UnanimidadTest` y las pruebas de `CasoDeEvaluacion`.

Una segunda estrategia, `SelectorDePreguntas`, decide cómo se escogen las preguntas de un simulacro (hoy, al azar:
`SeleccionAleatoria`); cambiarla por una que equilibre por competencia no toca `SimulationApplicationService`
(`SeleccionAleatoriaTest`).

### State · `FaseDePregunta`

- **Problema:** lo que se puede hacer con una pregunta depende de su estado: solo el borrador se modifica y se envía,
  solo la aprobada se publica, solo la rechazada se reabre. Esas reglas estaban repartidas en condicionales
  (`if (estado != BORRADOR)`) por el servicio de aplicación.
- **Solución:** cada estado es una clase (`FaseBorrador`, `FaseEnRevision`…) que implementa solo las operaciones que admite
  (`enviarARevision`, `iniciarRevision`, `aprobar`, `rechazar`, `reabrir`, `publicar`, `archivar`) y devuelve el estado
  siguiente; las demás lanzan `OperacionNoPermitidaException` con el mensaje de siempre. `Question` delega en su fase
  (`esEditable`, `enviarARevision`, `reabrir`…) y `EstadoPregunta.puedePasarA` también. `EstadoPregunta` sigue siendo el
  identificador que se guarda y viaja en los eventos.
- **Para agregar un estado nuevo:** una clase más y una línea en `FaseDePregunta.de`.
- **Prueba:** `FaseDePreguntaTest` (cada operación solo desde su estado, coincidencia con la tabla de RF-15, la pregunta
  delega en su fase) y `EstadoPreguntaTest`.

### Template Method · `ReglaPorOpcion`

- **Problema:** dos de las reglas de validación recorrían las cuatro opciones con el mismo esqueleto (obtener las opciones,
  saltar las vacías, armar el mensaje con la letra) y solo cambiaba lo que se comprueba en cada una.
- **Solución:** `ReglaPorOpcion.validar` es el método plantilla (`final`): recorre A a D, omite las opciones que descarta
  el gancho `omitir` y llama al paso abstracto `evaluar`, que implementa cada regla. Las subclases no repiten el recorrido.
- **Prueba:** `ReglaPorOpcionTest` (el esqueleto con una regla espía y el gancho cambiado) y `ReglasDeValidacionTest`.

### Decorator · `EmailConReintentos` y `EmailConRegistro`

- **Problema:** un servidor de correo real falla de vez en cuando por causas pasajeras y conviene dejar constancia de cada
  envío, pero eso no es responsabilidad del caso de uso ni del adaptador que habla con el servidor.
- **Solución:** ambos decoradores implementan el puerto `EmailSender` y envuelven a otro `EmailSender`. `EmailConReintentos`
  repite el envío hasta `notification.email-attempts` veces (3 por defecto); `EmailConRegistro` registra y cuenta los envíos
  correctos y fallidos. Se apilan en `ApplicationConfig`: el registro envuelve a los reintentos y estos al envío simulado.
- **Prueba:** `DecoradoresDeCorreoTest` (reintento hasta que pasa, última excepción si no, conteo y apilado).

### Observer · eventos por RabbitMQ

- **Problema:** `lifecycle-service` no debe llamar directamente a revisión, notificaciones, historial y catálogo cada vez
  que cambia una pregunta.
- **Solución:** el servicio publica un evento (sujeto) en el exchange `bancopreguntas.events`; cada interesado tiene
  su cola y su `@RabbitListener` (observador). Agregar un observador nuevo, como `audit-service`, no cambia al
  publicador. Los consumidores son idempotentes y el publicador usa el patrón Outbox.
- **Prueba:** las pruebas de los escuchas de eventos (`EventsListenerTest`, `ReviewEventsListenerTest` y equivalentes) y las de Outbox de `bp-platform`.

## Cobertura de la rúbrica

| Categoría | Mínimo | Implementados |
|---|---|---|
| Creacionales | 2 | Builder, Factory Method |
| Estructurales | 2 | Adapter, Facade, Decorator |
| Comportamiento | 2 | Strategy, Observer, State, Template Method |

Además, el Taller 8 del laboratorio implementa Builder, Adapter, Facade, State, Template Method y Decorator sobre un modelo
propio de pregunta; los docentes aceptan que los patrones de los laboratorios cuenten.

## Otros elementos de diseño

Esto **no es** un patrón GoF; se documenta porque explica una decisión del código.

| Elemento | Dónde | Observación |
|---|---|---|
| Lista de reglas de validación | `QuestionValidator` (en `validation-service`) y las siete `…Rule` que implementan `ValidationRule` | El validador ejecuta todas las reglas y reúne las violaciones para mostrar al autor todo lo que debe corregir de una vez; no es una cadena de responsabilidad (ninguna regla decide si pasa el turno a la siguiente) |

## Patrones de arquitectura de microservicios

| Patrón | Dónde |
|---|---|
| API Gateway | `api-gateway` (Spring Cloud Gateway): entrada única, autorización, CORS |
| Service Registry y Discovery | Eureka (`discovery-server`); las réplicas se registran solas |
| Base de datos por servicio | Cinco instancias de PostgreSQL |
| Arquitectura orientada a eventos | RabbitMQ, exchange *topic* `bancopreguntas.events` |
| Consumidor idempotente | Tabla `processed_event` en cada consumidor |
| Transactional Outbox | question, review y user: el evento se guarda con el cambio y un relevo lo publica |
| Circuit Breaker, Retry y Bulkhead | Gateway (Resilience4J) |
| Dead Letter Queue | `bancopreguntas.dead-letter`, por política de RabbitMQ |
| Seguridad con JWT | Keycloak, validado en el gateway y en cada servicio |
| Balanceo de carga | `edge` (nginx) delante de las réplicas del gateway |
