# 10 · Decisiones de arquitectura y SOLID

Este documento responde al entregable «decisiones de arquitectura y su justificación» (tipo de aplicación, estilo y
tecnologías) y a la rúbrica de SOLID y buenas prácticas. Cada decisión dice **qué se decidió, por qué, qué alternativas se
descartaron y qué se paga por ello**. Los nombres de clases son los del código.

## 1. Tipo de aplicación

**Decisión:** aplicación web de una sola página (SPA, Angular) que consume una API REST; el backend y el frontend son
proyectos separados que solo se comunican por HTTP/JSON.

| | |
|---|---|
| Por qué | El enunciado del segundo corte exige frontend y backend separados y comunicados por API REST. Una web evita instalar nada en el equipo de los autores, los revisores y el administrador, y es lo que permite escalar el backend sin tocar el cliente |
| Alternativas | La aplicación de escritorio Swing del primer corte (el docente Libardo aceptó también Swing o JavaFX, pero el foco de la iteración es el backend y una aplicación de escritorio no se despliega en contenedores); React en lugar de Angular |
| Por qué Angular | Estructura por defecto (rutas, formularios, inyección de dependencias, pruebas) que acelera un proyecto de equipo; TypeScript detecta errores de contrato con la API en compilación |
| Costo | Dos bases de código y dos conjuntos de pruebas (183 más de frontend); hay que mantener los modelos de TypeScript alineados con los DTO del backend, lo que se cubre con las colecciones de Postman y el recorrido funcional |

## 2. Estilo arquitectónico

**Decisión:** microservicios orientados a eventos, con una base de datos por servicio, un API Gateway como entrada única y
RabbitMQ como bus.

| | |
|---|---|
| Por qué | El corte pide una solución distribuida y escalable. Cada contexto delimitado (usuarios, preguntas, validación, ciclo de vida, asignación, evaluación, notificaciones, correo, historial, auditoría, catálogo, simulacros, estadísticas y reportes) evoluciona y escala por separado; la comunicación por eventos desacopla productores y consumidores (notificaciones e historial se agregan sin tocar a quien produce los eventos) |
| Alternativas | Monolito modular (más simple, pero no cumple el corte ni escala por partes); microservicios que se llaman siempre por REST (acopla disponibilidad: si revisión cae, preguntas no puede enviar; por eso las llamadas síncronas se limitan a tres, ver abajo); Saga, CQRS y Event Sourcing (los docentes los dejaron fuera de alcance) |
| Costo | Consistencia eventual (la pregunta pasa a «En revisión» unos milisegundos después de asignar), más piezas que operar y pruebas más complejas |
| Cómo se mitiga | Patrón Outbox (no se pierden eventos), consumidores idempotentes (un evento repetido no duplica efectos), reintentos con cola de mensajes muertos, circuit breaker en el gateway; todo documentado en [08](08-implementacion-seguridad-y-resiliencia.md) |

### Cuántos servicios y por qué tan pequeños

**Decisión (AD-03):** 14 servicios, cada uno con una sola responsabilidad y sus propios datos. Al principio eran siete; se dividió
cada servicio que mezclaba responsabilidades con motivos de cambio o de escala distintos:

| Antes | Ahora | Qué se separó |
|---|---|---|
| `question-service` | `question-service`, `validation-service`, `lifecycle-service` | El contenido, las reglas de calidad (cambian cuando cambian los criterios) y el ciclo de vida (cambia cuando cambian los estados) |
| `review-service` | `assignment-service`, `evaluation-service` | Repartir la revisión (HU-04) y decidirla (HU-05) |
| `notification-service` | `notification-service`, `email-service` | El aviso en la bandeja y el canal de correo (que hoy es simulado y mañana puede ser real) |
| `history-service` | `history-service`, `audit-service` | La línea de tiempo de cada pregunta (requisito) y la auditoría de acciones e inicios de sesión (valor agregado) |
| `simulation-service` | `catalog-service`, `simulation-service` | Las preguntas disponibles y el simulacro que las usa |
| `tracking-service` | `statistics-service`, `report-service` | El seguimiento de cada estudiante y el reporte del grupo |

| | |
|---|---|
| Por qué | Cada servicio queda lo bastante pequeño para entenderse entero, probarse y reemplazarse; los servicios con carga distinta (consultas del estudiante frente a reportes del docente, bandeja frente a correo) escalan por separado y una caída del correo, de la auditoría o del reporte no afecta al flujo principal |
| Alternativas | Mantener los siete servicios (menos piezas, pero con responsabilidades mezcladas); dividir todavía más (por ejemplo, un servicio por regla de validación): el costo de operación superaría la ganancia |
| Costo | Más contenedores y bases de datos (13 PostgreSQL), más eventos y pruebas de integración entre servicios; consistencia eventual entre la copia del estado de `question-service` y el ciclo de vida |
| Cómo se mitiga | Eventos con Outbox e idempotencia; números de secuencia para ignorar avisos atrasados; solo tres llamadas síncronas, con la identidad propagada; la plantilla de cada servicio es la misma, así que operar 14 se parece a operar uno |

**Decisión (AD-04): las únicas llamadas síncronas entre servicios son tres.** Validación y ciclo de vida se llaman por REST desde
`question-service`, y el catálogo desde `simulation-service`. Es lo contrario de la decisión inicial de dejarlos como módulos internos
(antes se descartó separarlos para no añadir una llamada de red por operación): al medir la complejidad de un servicio con todo
adentro, pesó más la claridad de las responsabilidades y el escalado independiente que esa llamada, y las tres se justifican porque el
usuario espera la respuesta (ver [02](02-contextos-y-eventos.md)). Se acotan así: la identidad de quien hizo la petición viaja en la
llamada y el servicio llamado vuelve a exigir el rol; si el otro servicio no responde, la operación falla con 503 y no deja
nada a medias (el servicio que llama no guarda hasta que el otro confirma); y el gateway no expone ninguno de los tres puntos
internos, de modo que las respuestas correctas del catálogo no salen hacia el navegador.

## 3. Decisiones de tecnología

| Tecnología | Para qué | Por qué se eligió | Alternativas y desventajas |
|---|---|---|---|
| **Java 21 y Spring Boot 3.5** | Todos los servicios | Obligatorio según los docentes («Java con Spring Boot»); ecosistema maduro para REST, JPA, AMQP y seguridad; el dominio ya estaba escrito en Java en el primer corte | Quarkus o Micronaut arrancan más rápido, pero el equipo no los conoce. Desventaja: arranque de unos segundos y consumo de memoria mayor |
| **Spring Data JPA y Flyway** | Persistencia y migraciones | JPA reduce el código repetitivo; Flyway versiona el esquema (`db/migration`) y asegura que cada entorno tenga el mismo | JDBC puro (más control, más código). Desventaja: JPA oculta consultas; por eso hay pruebas de persistencia sobre PostgreSQL real |
| **PostgreSQL 16, una por servicio** | Datos | Confirmado por los docentes. Una base por servicio garantiza que ningún servicio lea las tablas de otro (bajo acoplamiento) | Una base compartida (más simple, pero acopla los servicios por el esquema). Desventaja: no hay joins entre servicios; se resuelve guardando solo identificadores y copiando lo mínimo que cada servicio necesita (`question_info`) |
| **RabbitMQ 3.13** | Bus de eventos | Confirmado por los docentes; exchange *topic* con una cola por consumidor, entrega persistente, confirmaciones, colas de mensajes muertos y panel de administración | Kafka (más potente, mucho más pesado para este volumen). Desventaja: el orden estricto solo se garantiza por cola, no entre servicios |
| **Keycloak 26** | Identidad | Autenticación y roles son un problema resuelto: OAuth2/OpenID Connect con PKCE, JWT firmado, administración de usuarios y registro de eventos de acceso (que alimentan la auditoría). Evita guardar contraseñas en nuestros servicios (RNF-08) | Implementar el login en `user-service` con BCrypt (menos piezas, pero más código de seguridad propio y más riesgo). Desventaja: un contenedor más y configuración del realm |
| **JWT validado dos veces** | Seguridad | El gateway valida y autoriza por rol, y cada servicio vuelve a validar firma y emisor y exige el rol: un error en una barrera no abre la operación (autorización redundante) | Confiar solo en el gateway (más simple, pero cualquiera que llegue a un servicio por la red interna se salta la seguridad) |
| **Spring Cloud Gateway, Eureka y nginx (`edge`)** | Entrada única, descubrimiento y balanceo | El gateway centraliza token, roles, CORS y resiliencia; Eureka permite agregar réplicas sin cambiar configuración; `edge` reparte entre réplicas del gateway | Direcciones fijas en el gateway (más simple, no escala). Desventaja: más contenedores |
| **Resilience4J** | Resiliencia | Circuit breaker, reintentos, límite de tiempo y bulkhead por servicio, integrados con el gateway | Sin protección: un servicio lento acumularía peticiones. Se ajustaron los valores con pruebas (el bulkhead por defecto rechazaba llamadas sanas bajo carga) |
| **Patrón Outbox** | Mensajería confiable | Publicar tras el commit puede fallar si RabbitMQ está caído; el Outbox guarda el evento en la misma transacción y un relevo lo envía | Publicar directo (pierde eventos). Desventaja: una tabla y un proceso periódico más; entrega «al menos una vez», que se compensa con consumidores idempotentes |
| **Angular, TypeScript y nginx** | Frontend | Ver sección 1 | React |
| **Docker y Docker Compose** | Despliegue | El corte 3 pide empaquetar todo para levantarse con un comando; permite además probar réplicas (`--scale`) | Kubernetes (fuera de alcance según los docentes) |
| **JUnit 5, MockMvc, ArchUnit, PostgreSQL embebido** | Pruebas del backend | Las pruebas corren sin Docker: dominio y aplicación con dobles en memoria, persistencia sobre PostgreSQL real embebido, API con MockMvc, seguridad con tokens firmados en la prueba y arquitectura con ArchUnit | Testcontainers (exige Docker abierto para pasar la integración continua) |
| **Vitest** | Pruebas del frontend | Integrado con Angular, rápido | Karma/Jasmine |
| **k6, Postman y newman** | Carga y contrato | k6 mide escalabilidad (RNF-17 y RNF-18); las colecciones de Postman (pedidas por los docentes por módulo) se ejecutan automáticamente con newman | JMeter |
| **springdoc OpenAPI** | Documentación de la API | Cada servicio genera su contrato y su Swagger a partir del código, y el gateway los agrega: la documentación no se desactualiza | Escribir los contratos a mano (se desincronizan, como pasó con el diseño inicial) |
| **Mermaid y modelo C4** | Diagramas | Texto versionado en git que GitHub muestra directamente, exportable a imagen | Herramientas gráficas (no se pueden revisar en un diff) |

## 4. Otras decisiones de diseño

| # | Decisión | Por qué | Alternativa descartada |
|---|---|---|---|
| D1 | **Arquitectura por capas con puertos y adaptadores** en todos los servicios (`api`, `application`, `domain`, `infrastructure`) | El dominio no depende de Spring ni de JPA; los casos de uso dependen de interfaces (puertos) que implementa la infraestructura. Facilita probar y cambiar tecnología (por ejemplo, el correo simulado por uno real) | Capas clásicas con el dominio acoplado a JPA |
| D2 | **Biblioteca común `bp-platform`** (seguridad JWT, sobre de eventos, Outbox, manejo de errores, OpenAPI) | Evita repetir lo transversal en 14 servicios. Incluye la propagación de la identidad en las llamadas entre servicios. Solo contiene infraestructura, **nunca modelo de dominio**, para no acoplar los contextos | Copiar el código en cada servicio |
| D3 | **Un solo rechazo cierra la ronda** y la pregunta queda Rechazada | Lo confirmó el docente (consulta 29: «con un rechazo la pregunta se devuelve»). Las observaciones que lleguen después se guardan marcadas como tardías | Esperar a que todos evalúen |
| D4 | **Rondas por versión de la pregunta** (`review_round`) | Al corregir y reenviar, la pregunta nueva se revisa en una ronda nueva con los mismos revisores; el historial de rondas anteriores se conserva | Sobrescribir la revisión |
| D5 | **Correo simulado** tras el puerto `EmailSender` (en `email-service`) | Lo rectificó el docente Libardo: enviar correo real en pruebas bloquea cuentas. Cambiar a SMTP real solo requiere otro adaptador | SMTP real con una cuenta de prueba |
| D6 | **Historial (`history-service`) y auditoría (`audit-service`)**, alimentados solo por eventos | Es un consumidor puro: no afecta al flujo principal y se puede reconstruir | Que cada servicio guarde su propio historial |
| D7 | **Máximo de revisores configurable** (`ASSIGNMENT_MAX_REVIEWERS`, por defecto 3) | Los docentes piden 1 a 3 «y si queda abierto a más no hay problema» | Valor fijo en el código |
| D8 | **Identificadores de usuario = nombre de usuario de Keycloak** | Los servicios se refieren a las personas solo por identificador; no se necesita una tabla de usuarios en cada uno | UUID propio sincronizado en cada servicio |
| D9 | **Borrado lógico** (nada se elimina físicamente) | RNF-16; mantiene la trazabilidad | Borrado físico |
| D10 | **Catálogo propio (`catalog-service`) alimentado por `question.published` con el contenido de la pregunta** (*event-carried state transfer*) | El catálogo no depende de que `question-service` esté en pie, y los dos servicios no se acoplan en tiempo de ejecución | Que se consulte a `question-service` por REST en cada simulacro |
| D11 | **El simulacro guarda una copia de sus preguntas** | Lo que vio el estudiante no cambia si la pregunta se archiva o se actualiza; la calificación es reproducible | Referenciar el catálogo |
| D12 | **Estadísticas calculadas al consultar, sin agregados guardados; el reporte del grupo, un modelo de lectura propio** | En `statistics-service` el historial es la única fuente y los cálculos son funciones puras fáciles de probar; `report-service` guarda solo el último resumen de cada estudiante (que publica `statistics.student-updated`) y no consulta a nadie | Precalcular agregados por evento; que el reporte consulte a las estadísticas |
| D13 | **Roles Estudiante y Docente separados de Autor y Revisor** | Cada uno ve solo lo suyo: el estudiante no ve preguntas en elaboración ni revisiones | Un solo rol «usuario» con permisos finos |
| D14 | **`question-service` guarda una copia del estado con su secuencia, y `lifecycle-service` es el dueño del estado** | Se puede listar y filtrar por estado sin llamar a otro servicio; el número de secuencia hace que un aviso repetido o atrasado no haga retroceder la copia | Consultar el estado en cada listado; compartir la base |
| D15 | **Los eventos hacia afuera no cambiaron al dividir los servicios**: solo se les agregaron campos | Los consumidores existentes siguen funcionando; quien publica un evento puede cambiar de servicio sin que los demás se enteren | Renombrar los eventos por servicio |

## 5. SOLID en el código

### S · Responsabilidad única

Cada clase tiene una razón para cambiar.

| Ejemplo | Responsabilidad |
|---|---|
| `QuestionValidator` (en `validation-service`) y cada `*Rule` (`CamposObligatoriosRule`, `ContextoObligatorioRule`, `PreguntaDirectaUnicaRule`, `CuatroOpcionesRule`, `RespuestaCorrectaUnicaRule`, `ExpresionesProhibidasRule`, `LongitudYEstructuraOpcionesRule`) | El validador reúne las violaciones; cada regla comprueba una sola cosa de RF-08 a RF-13 |
| `EstadoPregunta` y las clases `Fase…` (en `lifecycle-service`) | `EstadoPregunta` identifica el estado; cada fase (patrón State) conoce solo lo que se puede hacer en ese estado |
| `OutboxRelay` y `OutboxEventPublisher` | El primero reenvía los pendientes; el segundo solo guarda el evento en la transacción |
| `QuestionApplicationService`, `AssignmentApplicationService`, `EvaluationApplicationService`, `LifecycleApplicationService` | Orquestan casos de uso; no tocan HTTP, SQL ni RabbitMQ |
| `QuestionController` / `AssignmentController` / `EvaluationController` | Traducen HTTP a casos de uso y viceversa |

### O · Abierto/cerrado

Se agrega comportamiento nuevo sin modificar el código existente.

| Para… | Basta con… | Sin tocar… |
|---|---|---|
| Una regla de validación nueva | Una clase que implemente `ValidationRule` y agregarla en `QuestionValidator.porDefecto()` | Las demás reglas ni el servicio (`question-service` ni siquiera se entera) |
| Un tipo de aviso nuevo | Una subclase de `CreadorDeAviso` (Factory Method), un valor en `TipoNotificacion` y su registro en `CreadoresDeAviso` | `NotificationApplicationService` |
| Otra forma de decidir una ronda (mayoría) | Una implementación de `PoliticaDeDecision` (Strategy) elegida en `ApplicationConfig` | `CasoDeEvaluacion` |
| Un estado nuevo en el ciclo de vida de la pregunta | Una clase que implemente `FaseDePregunta` (State) y una línea en `FaseDePregunta.de` | Los casos de uso |
| Una regla de validación que examine cada opción | Una subclase de `ReglaPorOpcion` (Template Method) con solo el paso `evaluar` | El recorrido de las opciones ni las demás reglas |
| Reintentos o registro en el envío de correo | Decoradores del puerto `EmailSender` apilados en `ApplicationConfig` de `email-service` | El caso de uso y el adaptador |
| Otra forma de escoger las preguntas de un simulacro | Una implementación de `SelectorDePreguntas` elegida en `ApplicationConfig` | `CatalogApplicationService` |
| Otra ruta protegida en el gateway | Una línea en la tabla `RoleRules` | La configuración de seguridad |

### L · Sustitución de Liskov

Toda implementación puede usarse donde se espera su tipo base sin que el cliente note la diferencia.

- `Unanimidad` cumple el contrato de `PoliticaDeDecision` (decide a partir de revisores y evaluaciones), por lo que `CasoDeEvaluacion` funciona igual con cualquier otra.
- Los creadores de aviso son intercambiables: `CreadoresDeAvisoTest` crea cada tipo a través de la clase base y comprueba el mismo contrato (una notificación del tipo pedido, sin leer, con asunto y mensaje).
- `SimulatedEmailSender` y el doble `RecordingEmailSender` de las pruebas sustituyen a cualquier `EmailSender`; los casos de uso se comportan igual.

### I · Segregación de interfaces

Los puertos son pequeños y cada cliente depende solo de lo que usa.

| Puerto | Qué expone |
|---|---|
| `EmailSender` | Un único método, `enviar` |
| `ProcessedEvents` | Solo `registrarSiEsNuevo` (idempotencia) |
| `QuestionInfoRepository` y `NotificationRepository` | Separados: lo que se recuerda de una pregunta no se mezcla con la bandeja |
| `DomainEventPublisher` | Publicar eventos de dominio, sin saber cómo viajan |

### D · Inversión de dependencias

Los módulos de alto nivel (casos de uso) no dependen de los de bajo nivel (JPA, RabbitMQ); ambos dependen de abstracciones.

- Los servicios de aplicación reciben sus puertos, el `Clock` y el generador de identificadores **por constructor**; `ApplicationConfig` de cada servicio los ensambla con los adaptadores (`JpaQuestionRepository`, `QuestionEventPublisher`…). Por eso las pruebas usan dobles en memoria y reloj manual.
- `ArquitecturaTest` (ArchUnit) **hace fallar la compilación** si el dominio depende de Spring, de JPA/Jackson/AMQP o de las capas de aplicación, infraestructura o API. La regla está en los 14 servicios.

## 6. Buenas prácticas aplicadas

| Práctica | Dónde |
|---|---|
| Inmutabilidad | `Question` y los objetos de valor son inmutables; un cambio devuelve una instancia nueva (`conEstado`, `reabrir`) |
| Fallar rápido con mensajes claros | Los errores se traducen a respuestas en español con el motivo (`ApiError` y `PlatformExceptionHandler` de `bp-platform`, más un manejador por servicio) |
| Sin números ni textos mágicos | Límites, mensajes y rutas en constantes y configuración (`ASSIGNMENT_MAX_REVIEWERS`, `RoleRules`) |
| Configuración fuera del código | Variables de entorno (`.env.example`); el `.env` real no se versiona |
| Pruebas por capa | Dominio, aplicación con dobles, API, persistencia sobre PostgreSQL real, mensajería, seguridad, arquitectura y flujo de extremo a extremo (ver el [plan de pruebas](anexos/plan-de-pruebas.md)) |
| Idempotencia | `processed_event` en cada consumidor |
| Documentación viva | OpenAPI exportado de los servicios, diagramas versionados en git y trazabilidad de requisitos ([09](09-trazabilidad-de-requisitos.md)) |

## 7. Escenarios de calidad

- **Escalabilidad:** [04](04-escalabilidad.md) (600 usuarios concurrentes, 0 % de error, p95 de 18 ms a ritmo humano y 692 ms bajo estrés).
- **Modificabilidad:** agregar un tipo de notificación, una regla de validación o una política de decisión no toca el
  código existente (sección 5, O). El escenario formal de modificabilidad (con estímulo, respuesta y medida) del tercer
  corte se redacta con el documento final.
