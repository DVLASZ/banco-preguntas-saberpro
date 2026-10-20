# JPA y DTO en el proyecto

Verificación hecha el 20 de octubre de 2026 a raíz del requerimiento de los docentes de usar **JPA** y **DTO**. Resumen: los
13 servicios con datos ya persisten con JPA y todas sus APIs hablan con DTO; el monolito del primer corte (congelado en la rama
`corte-1`) solo los usa en `modulo-api-rest`.

## 1. Cómo se aplican en los microservicios

Cada servicio separa tres modelos y los convierte en la frontera de cada capa:

```
JSON  <->  DTO (api/dto)  <->  dominio (domain)  <->  entidad JPA (infrastructure/persistence)  <->  PostgreSQL
              record                 clases puras             @Entity + Spring Data
```

| Pieza | Dónde | Regla |
|---|---|---|
| **DTO de entrada** (`*Request`) | `api/dto`, como `record` | Lleva la validación de formato con Bean Validation (`@NotBlank`, `@Size`, `@Pattern`, `@NotNull`) y el controlador la activa con `@Valid`. Convierte a objetos del dominio con un método `a…()` |
| **DTO de salida** (`*Response`, `PageResponse`) | `api/dto`, como `record` | Se construye desde el dominio con un método estático `de(...)`. Nunca se devuelve una entidad ni una clase del dominio. Oculta lo que el cliente no debe ver (por ejemplo, la respuesta correcta de un simulacro en curso) |
| **Entidad JPA** (`*Entity`) | `infrastructure/persistence` | Anotada con `@Entity`; solo la conocen los adaptadores de persistencia. El esquema lo crea Flyway y Hibernate solo lo **valida** (`ddl-auto: validate`) |
| **Repositorio Spring Data** (`*JpaRepository`) | `infrastructure/persistence` | Extiende `JpaRepository`; las consultas con filtros opcionales se construyen dinámicamente (`QuestionJpaRepositoryCustomImpl`) para poder usar índices |
| **Adaptador** (`Jpa*Repository`, `*Mapper`) | `infrastructure/persistence` | Implementa el puerto del dominio (por ejemplo `QuestionRepository`) y convierte entre entidad y dominio. El dominio no sabe que existe JPA |

Reglas que **hace cumplir ArchUnit** (`ArquitecturaTest` en cada servicio, falla la compilación si se rompen): el dominio no
depende de Spring, JPA, Jackson ni AMQP; la aplicación no depende de los adaptadores ni de la persistencia; la infraestructura no
depende de la API. Además, `spring.jpa.open-in-view=false` en todos los servicios: las entidades nunca llegan al controlador.

## 2. Inventario por servicio

| Servicio | Entidades JPA | DTO de la API | Migraciones Flyway |
|---|---|---|---|
| `question-service` | `QuestionEntity` | `QuestionRequest`, `QuestionResponse`, `QuestionPageResponse`, `CambioEstadoResponse` | V1 a V5 |
| `validation-service` | (sin base de datos) | `ValidationRequest`, `ValidationResponse` | — |
| `lifecycle-service` | `LifecycleEntity`, `StateHistoryEntity`, `ProcessedEventEntity` | `RegistroRequest`, `EnvioRequest`, `PublicacionRequest`, `LifecycleResponse`, `CambioEstadoResponse` | V1 |
| `assignment-service` | `CaseEntity`, `RoundEntity`, `AssignmentEntity`, `ProcessedEventEntity` | `AssignReviewersRequest`, `AssignmentResponse`, `CaseAssignmentsResponse`, `PendingCaseResponse`, `ReviewConfigResponse`, `ReviewPageResponse` | V1 |
| `evaluation-service` | `ReviewCaseEntity`, `ReviewRoundEntity`, `AssignmentEntity`, `EvaluationEntity`, `ProcessedEventEntity` | `EvaluationRequest`, `EvaluationResultResponse`, `ReviewResponse` | V1 |
| `notification-service` | `NotificationEntity`, `QuestionInfoEntity`, `ProcessedEventEntity` | `NotificationResponse`, `PageResponse`, `UnreadCountResponse`, `MarkedReadResponse` | V1 a V3 |
| `email-service` | `SentEmailEntity`, `ProcessedEventEntity` | `EmailResponse`, `PageResponse` | V1 |
| `history-service` | `HistoryEntryEntity`, `QuestionInfoEntity`, `QuestionReviewerEntity`, `ProcessedEventEntity` | `EntryResponse`, `PageResponse` | V1 y V2 |
| `audit-service` | `AuditEntryEntity`, `ProcessedEventEntity` | `EntryResponse`, `PageResponse` | V1 |
| `catalog-service` | `CatalogQuestionEntity`, `ProcessedEventEntity` | `SelectionRequest`, `SelectedQuestionResponse`, `CatalogSummaryResponse` | V1 |
| `simulation-service` | `SimulationEntity`, `SimulationItemEntity` | `CreateSimulationRequest`, `AnswerRequest`, `SimulationResponse`, `SimulationItemResponse`, `SimulationSummaryResponse`, `ResultResponse`, `PageResponse` | V1 y V2 |
| `statistics-service` | `ResultEntryEntity`, `ProcessedEventEntity` | `StatsResponse`, `StrengthsResponse`, `CompetencyResponse`, `HistoryEntryResponse`, `PageResponse` | V1 y V2 |
| `report-service` | `StudentSummaryEntity`, `ProcessedEventEntity` | `GroupReportResponse`, `CompetencyResponse` | V1 |
| `user-service` | `UserEntity`, `SessionCursorEntity` | `UserResponse`, `MeResponse`, `SyncResponse`, `PageResponse` | V1 y V2 |

Los servicios con Outbox (publican eventos) comparten además la tabla `outbox_event` de `bp-platform`.

## 3. Hallazgos de la verificación

- **Todos los endpoints usan DTO.** Se revisaron los 14 controladores: ningún método devuelve una entidad ni una clase del dominio y
  todo cuerpo de entrada es un DTO.
- **Corregido:** `notification-service` devolvía dos respuestas como `Map` (`/unread-count` y `/read-all`). Ahora son
  `UnreadCountResponse` y `MarkedReadResponse`; el JSON no cambia (`{"noLeidas": n}` y `{"marcadas": n}`), pero el contrato
  OpenAPI queda tipado.
- **Excepción deliberada:** `ValidationRequest` no usa `@Valid`. Su propósito es recibir contenido incompleto y devolver **todas**
  las violaciones en una sola respuesta, así que sus campos son opcionales y el dominio hace la validación.
- **Sin base de datos:** `validation-service` no tiene entidades porque solo evalúa reglas sobre lo que recibe.
- **Repetición aceptada:** `PageResponse` existe en varios servicios con el mismo formato. Se mantiene por servicio para no
  compartir clases de la API (la biblioteca `bp-platform` solo contiene infraestructura, nunca modelo).

## 4. El monolito del primer corte

| Parte | Persistencia | DTO |
|---|---|---|
| `modulo-api-rest` (Taller 6) | JPA: `QuestionEntity` y `QuestionJpaRepository` (Spring Data) sobre H2 | `QuestionRequest`, `QuestionResponse`, con `QuestionMapper` |
| `modulo-preguntas`, `modulo-usuarios`, `modulo-revision`, `modulo-simulacros` (aplicación de escritorio) | JDBC directo (`QuestionImplRepository`), sin JPA | No aplica: no hay API, las ventanas usan el dominio |

El monolito está **congelado** en la rama `corte-1` y el proyecto vigente es el de microservicios, donde JPA y DTO ya están
completos; migrar la aplicación de escritorio a JPA no aporta al corte 2 y cambiaría código ya entregado. Si los docentes lo
exigieran para el monolito, el camino es el de `modulo-api-rest`: una entidad por agregado en el módulo de acceso, un repositorio
Spring Data detrás del puerto existente (`QuestionRepository`) y un mapper entidad-dominio, sin tocar el dominio.

## 5. Cómo agregar una entidad o un DTO nuevo

1. Escribir la migración de Flyway en `src/main/resources/db/migration` (el esquema nunca lo crea Hibernate).
2. Crear la `@Entity` y el `*JpaRepository` en `infrastructure/persistence`.
3. Implementar el puerto del dominio en un `Jpa*Repository` con su mapper.
4. Crear el `record` de entrada con validaciones y el de salida con `de(...)` en `api/dto`, y usarlos en el controlador.
5. Correr `./mvnw -pl servicios/<servicio> -am test`: `ArquitecturaTest` confirma que no se rompieron las capas.
