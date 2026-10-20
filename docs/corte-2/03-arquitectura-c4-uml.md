# 03 · Arquitectura (C4 y UML)

> **Estado del repositorio (octubre de 2026).** Esta documentación describe la arquitectura de **14 microservicios** que se está
> implementando. El código publicado hoy en el repositorio corresponde a la primera distribución, de **7 servicios**; la nueva
> distribución divide varios de ellos para que cada uno tenga una sola responsabilidad (ver la tabla del [README de los servicios](../../microservicios/servicios/README.md)). Ya está implementada y verificada de punta a punta en local y el código se irá publicando por
> partes; mientras tanto, lo que no está en el repositorio aparece en esta documentación como texto, sin enlace.

> Cada diagrama de este documento también está como imagen (SVG y PNG) en [`img/diagramas`](img/diagramas/), lista para el documento formal en Word o PDF.
> Los nombres de clases, tablas y rutas son los del código; si algo cambia en el código, este documento se actualiza con él.

## Estilo arquitectónico

Microservicios orientados a eventos con un API Gateway como único punto de entrada, una base de datos
por servicio y un broker de mensajes para la comunicación entre servicios. Son 14 servicios pequeños, cada uno con una sola
responsabilidad ([02 · Contextos](02-contextos-y-eventos.md) explica la división). Frontend y backend están
separados y se comunican solo por API REST. La identidad la gestiona un proveedor externo (Keycloak) y los
servicios validan el JWT que emite. La justificación de cada decisión está en
[10 · Decisiones de arquitectura y SOLID](10-decisiones-y-solid.md).

## C4 · Nivel 1: contexto del sistema

```mermaid
flowchart LR
  AUT["Autor de preguntas"]
  ADM["Administrador"]
  REV["Revisor"]
  EST["Estudiante"]
  DOC["Docente"]
  SYS["Sistema Banco de Preguntas<br/>Saber Pro"]
  KC["Keycloak<br/>proveedor de identidad"]
  AUT -- "crea, envía y consulta preguntas" --> SYS
  ADM -- "asigna revisores y publica" --> SYS
  REV -- "evalúa preguntas" --> SYS
  EST -- "presenta simulacros y ve su progreso" --> SYS
  DOC -- "consulta el progreso y los reportes" --> SYS
  SYS -- "autentica y obtiene usuarios" --> KC
```

El correo electrónico es simulado (decisión del docente): el sistema deja el aviso en la bandeja de cada usuario y
`email-service` registra el «envío»; por eso no aparece un servidor de correo como sistema externo.

## C4 · Nivel 2: contenedores

```mermaid
flowchart TB
  FE["Frontend web<br/>Angular + nginx"]
  EDGE["edge<br/>nginx (balanceador)"]
  KC["Keycloak<br/>identidad (JWT)"]
  EU["discovery-server<br/>Eureka"]
  GW["api-gateway<br/>Spring Cloud Gateway<br/>valida token y rol"]
  US["user-service"]
  QS["question-service"]
  VS["validation-service<br/>(sin base de datos)"]
  LS["lifecycle-service"]
  AS["assignment-service"]
  ES["evaluation-service"]
  NS["notification-service"]
  EM["email-service"]
  HS["history-service"]
  AU["audit-service"]
  CS["catalog-service"]
  SS["simulation-service"]
  ST["statistics-service"]
  RP["report-service"]
  MQ{{"RabbitMQ<br/>exchange bancopreguntas.events"}}
  DB[("13 bases PostgreSQL<br/>una por servicio")]
  FE -- "REST/JSON" --> EDGE
  FE -. "login PKCE" .-> KC
  EDGE --> GW
  GW -. "registro" .-> EU
  GW -. "claves públicas" .-> KC
  US -. "usuarios y eventos de acceso" .-> KC
  GW --> US & QS & AS & ES & NS & EM & HS & AU & CS & SS & ST & RP
  QS -- "REST interno" --> VS
  QS -- "REST interno" --> LS
  SS -- "REST interno" --> CS
  US & LS & AS & ES & NS & SS & ST <-. "eventos" .-> MQ
  QS & CS & EM & HS & AU & RP <-. "eventos" .-> MQ
  US & QS & LS & AS & ES & NS & EM & HS & AU & CS & SS & ST & RP --- DB
```

| Contenedor | Responsabilidad | Puerto |
|---|---|---|
| frontend | Aplicación Angular servida por nginx | 4200 |
| edge | Balanceador nginx delante de las réplicas del gateway | 8080 |
| api-gateway | Enrutamiento por Eureka, validación del token, autorización por rol, circuit breaker y reintentos | interno |
| discovery-server | Registro de servicios (Eureka) | 8761 |
| keycloak | Proveedor de identidad: inicio de sesión, roles y token JWT | 8180 |
| user-service | Sincroniza usuarios y roles de Keycloak; consulta sus eventos de acceso y los publica | 8081 (depuración) |
| question-service | Contenido de las preguntas: crear, modificar, consultar y filtrar | 8082 (depuración) |
| assignment-service | Rondas de revisión, asignación de revisores y bandeja del revisor | 8083 (depuración) |
| notification-service | Consume eventos y crea los avisos de la bandeja; publica `notification.created` | 8084 (depuración) |
| history-service | Línea de tiempo de cada pregunta | 8085 (depuración) |
| simulation-service | Simulacros con tiempo y calificación automática | 8086 (depuración) |
| statistics-service | Historial, estadísticas, fortalezas y debilidades de cada estudiante | 8087 (depuración) |
| email-service | Correo simulado de cada aviso, con reintentos y registro | 8090 (depuración) |
| audit-service | Auditoría de acciones e inicios de sesión | 8091 (depuración) |
| catalog-service | Copia de las preguntas publicadas, resumen y selección al azar | 8092 (depuración) |
| report-service | Reporte del grupo para docentes | 8093 (depuración) |
| lifecycle-service | Estados de la pregunta, transiciones, historial y sus eventos (interno) | 8094 (depuración) |
| validation-service | Reglas de calidad del contenido de una pregunta (interno, sin estado) | 8095 (depuración) |
| evaluation-service | Evaluación de los revisores y decisión por unanimidad | 8096 (depuración) |
| RabbitMQ | Broker de eventos | 5672, panel 15672 |
| PostgreSQL ×13 | Una base por servicio (todos menos validation) | 5433 a 5446 (depuración) |

## C4 · Nivel 3: componentes

Todos los servicios siguen la misma organización interna: `api`, `application`, `domain` e
`infrastructure`. La capa de aplicación declara **puertos** (interfaces) y la de infraestructura los implementa con
**adaptadores** (JPA, RabbitMQ, clientes REST): es una organización hexagonal. Una prueba de arquitectura
(`ArquitecturaTest`, con ArchUnit) hace fallar la compilación si el dominio depende de Spring, de JPA o de las otras
capas. Se muestran los servicios con más lógica; los demás (`user`, `history`, `audit`, `report`, `statistics`, `email`) siguen el
mismo esquema con un controlador, un servicio de aplicación, sus puertos y adaptadores.

### question-service

```mermaid
flowchart LR
  subgraph api
    QC["QuestionController"]
  end
  subgraph application
    QSV["QuestionApplicationService"]
    QR["QuestionRepository (puerto)"]
    VP["ValidadorDeContenido (puerto)"]
    CP["CicloDeVidaDePreguntas (puerto)"]
  end
  subgraph domain
    Q["Question (Builder)"]
    EST["EstadoPregunta"]
    FIL["FiltroPreguntas"]
  end
  subgraph infrastructure
    QJ["JpaQuestionRepository"]
    VR["ValidadorRemoto"]
    CR["CicloDeVidaRemoto"]
    QL["LifecycleEventsListener"]
  end
  QC --> QSV
  QSV --> Q
  QSV --> FIL
  Q --> EST
  QSV --> QR
  QSV --> VP
  QSV --> CP
  QJ -. implementa .-> QR
  VR -. implementa .-> VP
  CR -. implementa .-> CP
  QL --> QSV
```

### lifecycle-service

```mermaid
flowchart LR
  subgraph api
    LC["LifecycleController"]
  end
  subgraph application
    LAS["LifecycleApplicationService"]
    LR["LifecycleRepository (puerto)"]
    LP["DomainEventPublisher (puerto)"]
  end
  subgraph domain
    CV["CicloDeVida"]
    FD["FaseDePregunta (State)<br/>7 fases"]
    EV["QuestionSubmitted, StateChanged,<br/>Reopened, Published"]
  end
  subgraph infrastructure
    LJ["JpaLifecycleRepository"]
    LPUB["LifecycleEventPublisher"]
    RL["ReviewEventsListener"]
  end
  LC --> LAS
  LAS --> CV
  CV --> FD
  LAS --> EV
  LAS --> LR
  LAS --> LP
  LJ -. implementa .-> LR
  LPUB -. implementa .-> LP
  RL --> LAS
```

### assignment-service

```mermaid
flowchart LR
  subgraph api
    AC["AssignmentController"]
  end
  subgraph application
    AAS["AssignmentApplicationService"]
    AR["AssignmentRepository (puerto)"]
    AP["DomainEventPublisher (puerto)"]
  end
  subgraph domain
    CASE["CasoDeAsignacion"]
    RON["RondaDeAsignacion"]
    ASG["Asignacion"]
    EV["ReviewersAssigned"]
  end
  subgraph infrastructure
    JPA["JpaAssignmentRepository"]
    PUB["AssignmentEventPublisher"]
    QL["QuestionEventsListener"]
    RL["ReviewEventsListener"]
  end
  AC --> AAS
  AAS --> CASE
  CASE --> RON
  RON --> ASG
  AAS --> EV
  AAS --> AR
  AAS --> AP
  JPA -. implementa .-> AR
  PUB -. implementa .-> AP
  QL --> AAS
  RL --> AAS
```

### evaluation-service

```mermaid
flowchart LR
  subgraph api
    EC["EvaluationController"]
  end
  subgraph application
    EAS["EvaluationApplicationService"]
    ER["EvaluationRepository (puerto)"]
    EP["DomainEventPublisher (puerto)"]
  end
  subgraph domain
    CASE["CasoDeEvaluacion"]
    RON["RondaDeEvaluacion"]
    EVA["Evaluacion"]
    POL["PoliticaDeDecision<br/>(Unanimidad)"]
  end
  subgraph infrastructure
    JPA["JpaEvaluationRepository"]
    PUB["EvaluationEventPublisher"]
    QL["QuestionEventsListener"]
    AL["AssignmentEventsListener"]
  end
  EC --> EAS
  EAS --> CASE
  CASE --> RON
  RON --> EVA
  CASE --> POL
  EAS --> ER
  EAS --> EP
  JPA -. implementa .-> ER
  PUB -. implementa .-> EP
  QL --> EAS
  AL --> EAS
```

### notification-service y email-service

```mermaid
flowchart LR
  subgraph notification-service
    NC["NotificationController"]
    NAS["NotificationApplicationService"]
    CRE["CreadorDeAviso<br/>(Factory Method)<br/>8 creadores concretos"]
    NOT["Notificacion"]
    NPUB["NotificationEventPublisher"]
    EL["EventsListener"]
  end
  subgraph email-service
    EML["EventsListener"]
    EAS["EmailApplicationService"]
    DEC["EmailConRegistro(<br/>EmailConReintentos(<br/>SimulatedEmailSender))<br/>(Decorator)"]
    EC["EmailController"]
  end
  NC --> NAS
  EL --> NAS
  NAS --> CRE
  CRE --> NOT
  NAS --> NPUB
  NPUB -- "notification.created" --> EML
  EML --> EAS
  EAS --> DEC
  EC --> EAS
```

### catalog-service y simulation-service

```mermaid
flowchart LR
  subgraph catalog-service
    CC["CatalogController"]
    CAS["CatalogApplicationService"]
    CRP["CatalogRepository (puerto)"]
    SEL["SelectorDePreguntas<br/>(SeleccionAleatoria)"]
    CRI["CriteriosDeSeleccion"]
    PC["PreguntaDeCatalogo"]
    QL["QuestionEventsListener"]
  end
  subgraph simulation-service
    SC["SimulationController"]
    SAS["SimulationApplicationService"]
    CAT["CatalogoDePreguntas (puerto)"]
    REM["CatalogoRemoto"]
    SIM["Simulacro"]
    CAL["Calificador"]
    EXP["ExpirationScheduler"]
  end
  CC --> CAS
  QL --> CAS
  CAS --> CRI
  CAS --> SEL
  CAS --> CRP
  CRP --> PC
  SC --> SAS
  SAS --> CAT
  SAS --> SIM
  SIM --> CAL
  EXP --> SAS
  REM -. implementa .-> CAT
  REM -- "POST /api/catalog/selection" --> CC
```

### statistics-service y report-service

```mermaid
flowchart LR
  subgraph statistics-service
    TC["StatisticsController"]
    TAS["StatisticsApplicationService"]
    HR["HistorialRepository (puerto)"]
    RR["ResultadoRegistrado"]
    EI["EstadisticasIndividuales"]
    AC["AnalisisDeCompetencias"]
    SEL["SimulationEventsListener"]
    SPUB["StatisticsEventPublisher"]
  end
  subgraph report-service
    RL["StatisticsEventsListener"]
    RAS["ReportApplicationService"]
    SR["ResumenRepository (puerto)"]
    RG["ReporteDeGrupo"]
    RC["ReportController"]
  end
  TC --> TAS
  SEL --> TAS
  TAS --> HR
  TAS --> EI
  TAS --> AC
  EI --> RR
  TAS --> SPUB
  SPUB -- "statistics.student-updated" --> RL
  RL --> RAS
  RAS --> SR
  RAS --> RG
  RC --> RAS
```

`user-service` además habla con Keycloak a través del puerto `IdentityProvider`. `validation-service` solo tiene
`ValidationController`, `ValidationApplicationService` y las siete reglas `ValidationRule` que agrupa `QuestionValidator`.

## Máquina de estados de la pregunta

```mermaid
stateDiagram-v2
  [*] --> Borrador
  Borrador --> PendienteDeRevision: autor envía
  PendienteDeRevision --> EnRevision: se asignan revisores
  EnRevision --> Aprobada: todos aprueban
  EnRevision --> Rechazada: algún rechazo
  Rechazada --> Borrador: autor reabre y corrige
  Aprobada --> Publicada: administrador publica
  Borrador --> Archivada
  PendienteDeRevision --> Archivada
  EnRevision --> Archivada
  Aprobada --> Archivada
  Rechazada --> Archivada
  Publicada --> Archivada
```

Las transiciones válidas las decide `lifecycle-service`: cada estado es una clase (`FaseDePregunta`, patrón State) que sabe a
cuáles puede pasar, y las protegen pruebas. Cualquier estado puede pasar a Archivada porque la pregunta nunca se borra físicamente
(RNF-16); hoy ninguna operación de la API archiva, porque los docentes no definieron quién archiva ni cuándo (ver
[pendientes](README.md#pendiente)). `question-service` guarda una copia del estado para listar y filtrar.

## Secuencia · HU-04: asignar revisores

```mermaid
sequenceDiagram
  actor ADM as Administrador
  participant GW as api-gateway
  participant AS as assignment-service
  participant DB as assignment_db
  participant MQ as RabbitMQ
  participant LS as lifecycle-service
  participant ES as evaluation-service
  participant NS as notification-service
  ADM->>GW: POST /api/reviews/{id}/assignments (1 a 3 revisores)
  GW->>GW: valida token y rol ADMIN
  GW->>AS: reenvía la petición
  AS->>AS: valida 1 a 3, sin autor y sin repetidos
  AS->>DB: guarda asignaciones y el evento (Outbox)
  AS-->>ADM: 200 OK, rondas con sus revisores
  AS->>MQ: review.reviewers-assigned
  MQ->>LS: la pregunta pasa a En revisión
  MQ->>ES: registra a los revisores de la ronda
  MQ->>NS: aviso a cada revisor y al autor
```

## Secuencia · HU-05: evaluar una pregunta

```mermaid
sequenceDiagram
  actor REV as Revisor
  participant GW as api-gateway
  participant ES as evaluation-service
  participant DB as evaluation_db
  participant MQ as RabbitMQ
  participant LS as lifecycle-service
  participant AS as assignment-service
  participant NS as notification-service
  REV->>GW: POST /api/reviews/{id}/evaluations (decisión y observación)
  GW->>ES: valida rol REVIEWER y reenvía
  ES->>ES: verifica que esté asignado y no haya evaluado
  ES->>DB: guarda evaluación y eventos (Outbox)
  ES->>MQ: review.evaluation-submitted
  MQ->>AS: la bandeja lo marca como evaluado
  MQ->>NS: aviso al administrador
  alt todos aprobaron o hubo un rechazo
    ES->>ES: PoliticaDeDecision decide (unanimidad)
    ES->>MQ: review.completed
    MQ->>LS: Aprobada o Rechazada
    MQ->>AS: la ronda queda decidida
    MQ->>NS: aviso al autor, al administrador y a los revisores
  end
  ES-->>REV: 200 OK con el resultado de la ronda
```

## Secuencia · HU-06: corregir una pregunta rechazada

```mermaid
sequenceDiagram
  actor AUT as Autor
  participant GW as api-gateway
  participant QS as question-service
  participant VS as validation-service
  participant LS as lifecycle-service
  participant MQ as RabbitMQ
  participant AS as assignment-service
  AUT->>GW: GET /api/reviews/{id} (rondas y observaciones)
  GW-->>AUT: decisiones y observaciones de cada ronda (evaluation-service)
  AUT->>GW: POST /api/questions/{id}/reopen
  GW->>QS: reabrir
  QS->>LS: Rechazada a Borrador (versión +1)
  LS->>MQ: question.state-changed y question.reopened
  AUT->>GW: PUT /api/questions/{id}
  QS->>VS: valida el contenido corregido
  AUT->>GW: POST …/submit
  QS->>LS: Borrador a Pendiente de revisión
  LS->>MQ: question.submitted
  MQ->>AS: nueva ronda con los mismos revisores (asignación automática)
```

## Secuencia · simulacros y seguimiento (RF-21 a RF-28)

```mermaid
sequenceDiagram
  actor EST as Estudiante
  actor DOC as Docente
  participant GW as api-gateway
  participant SS as simulation-service
  participant CS as catalog-service
  participant MQ as RabbitMQ
  participant ST as statistics-service
  participant RP as report-service
  EST->>GW: POST /api/simulations (criterios, cantidad, duración)
  GW->>SS: valida token y rol STUDENT
  SS->>CS: POST /api/catalog/selection (con la identidad del estudiante)
  CS-->>SS: preguntas al azar con su respuesta correcta
  SS-->>EST: 201, simulacro en curso con su tiempo (sin respuestas correctas)
  EST->>GW: POST /api/simulations/{id}/answers (por pregunta)
  EST->>GW: POST /api/simulations/{id}/finish
  SS->>SS: califica automáticamente
  SS->>MQ: simulation.completed
  SS-->>EST: resultado y respuestas correctas
  MQ->>ST: simulation.completed
  ST->>ST: guarda el resultado en el historial
  ST->>MQ: statistics.student-updated
  MQ->>RP: guarda el resumen del estudiante
  EST->>GW: GET /api/statistics/me/stats (historial, fortalezas)
  DOC->>GW: GET /api/reports/group
  GW->>RP: valida token y rol TEACHER o ADMIN
  RP-->>DOC: reporte del grupo (JSON o CSV)
```

## Modelo de datos por servicio

Los servicios se refieren entre sí solo por identificador, sin claves foráneas entre bases. No hay borrado
físico (RNF-16): se usan estados y fechas. Las migraciones de Flyway están en `src/main/resources/db/migration`
de cada servicio. Todos los consumidores de eventos guardan `processed_event` (idempotencia) y los servicios que publican
(`lifecycle_db`, `assignment_db`, `evaluation_db`, `notification_db`, `simulation_db`, `statistics_db` y `user_db`) guardan además
`outbox_event` (patrón Outbox: `seq`, `event_id`, `routing_key`, `payload`, `created_at`, `sent_at`). `validation-service` no tiene base.

### user_db

`user-service` no guarda contraseñas: Keycloak es la fuente de la identidad y aquí solo se mantiene una copia de
los perfiles para listar revisores y asignar.

```mermaid
erDiagram
  USER_PROFILE {
    string username PK
    string keycloak_id
    string full_name
    string email
    string roles
    boolean active
    timestamp last_login_at
    timestamp synced_at
  }
  SESSION_CURSOR {
    int id PK
    timestamp last_event_at
  }
```

### question_db

Solo el contenido y una copia del estado; el historial de estados y los eventos son de `lifecycle_db`.

```mermaid
erDiagram
  QUESTION {
    string id PK
    string author_id
    string nombre
    text contexto
    text enunciado
    string opcion_a
    string opcion_b
    string opcion_c
    string opcion_d
    string respuesta_correcta
    text justificacion
    string bibliografia
    string competencia
    string tema
    string subtema
    string dificultad
    string estado "copia del estado"
    int estado_secuencia "último cambio aplicado"
    int question_version
    timestamp creada_en
    timestamp actualizada_en
  }
```

### lifecycle_db

```mermaid
erDiagram
  QUESTION_LIFECYCLE ||--o{ STATE_HISTORY : registra
  QUESTION_LIFECYCLE {
    string question_id PK
    string author_id
    string estado
    int question_version
    int sequence
    timestamp updated_at
  }
  STATE_HISTORY {
    long id PK
    string question_id FK
    string desde
    string hacia
    string cambiado_por
    timestamp fecha
  }
```

### assignment_db

Una **ronda** es una vuelta de revisión de una versión de la pregunta; si el autor corrige y reenvía, se abre otra.

```mermaid
erDiagram
  REVIEW_CASE ||--|{ REVIEW_ROUND : tiene
  REVIEW_ROUND ||--|{ ROUND_ASSIGNMENT : asigna
  REVIEW_CASE {
    string id PK
    string question_id
    string author_id
    string title
    string competency
    timestamp created_at
    timestamp updated_at
  }
  REVIEW_ROUND {
    string id PK
    string case_id FK
    int number
    int question_version
    string state
    timestamp started_at
    timestamp decided_at
    timestamp closed_at
  }
  ROUND_ASSIGNMENT {
    long id PK
    string round_id FK
    string reviewer_id
    string assigned_by
    timestamp assigned_at
    boolean automatic
    string decision "la informa evaluation-service"
  }
```

### evaluation_db

Las mismas rondas, vistas desde la evaluación: los revisores los informa `assignment-service` y aquí se guardan las evaluaciones.

```mermaid
erDiagram
  REVIEW_CASE ||--|{ REVIEW_ROUND : tiene
  REVIEW_ROUND ||--|{ ROUND_ASSIGNMENT : "revisores (copia)"
  REVIEW_ROUND ||--o{ EVALUATION : recibe
  REVIEW_CASE {
    string id PK
    string question_id
    string author_id
    string title
    string competency
  }
  REVIEW_ROUND {
    string id PK
    string case_id FK
    int number
    int question_version
    string state
    timestamp decided_at
    timestamp closed_at
  }
  ROUND_ASSIGNMENT {
    long id PK
    string round_id FK
    string reviewer_id
    string assigned_by
    boolean automatic
  }
  EVALUATION {
    long id PK
    string round_id FK
    string reviewer_id
    string decision
    text observation
    timestamp submitted_at
    boolean late
  }
```

### notification_db

```mermaid
erDiagram
  NOTIFICATION {
    string id PK
    string recipient_id
    string type
    string question_id
    string subject
    text message
    timestamp created_at
    timestamp read_at
  }
  QUESTION_INFO {
    string question_id PK
    string title
    string author_id
    string admin_id
  }
```

### email_db

```mermaid
erDiagram
  SENT_EMAIL {
    string id PK
    string notification_id
    string recipient_id
    string to_address
    string subject
    text body
    string status
    string error
    timestamp attempted_at
  }
```

### history_db

```mermaid
erDiagram
  HISTORY_ENTRY {
    string id PK
    string event_id
    string question_id
    string actor_id
    string type
    string summary
    text details
    timestamp occurred_at
    timestamp recorded_at
  }
  QUESTION_INFO {
    string question_id PK
    string title
    string author_id
  }
  QUESTION_REVIEWER {
    long id PK
    string question_id
    string reviewer_id
  }
```

### audit_db

```mermaid
erDiagram
  AUDIT_ENTRY {
    string id PK
    string event_id
    string question_id
    string actor_id
    string type
    string summary
    text details
    timestamp occurred_at
    timestamp recorded_at
  }
```

### catalog_db

```mermaid
erDiagram
  CATALOG_QUESTION {
    string question_id PK
    int question_version
    string title
    string competency
    string topic
    string subtopic
    string difficulty
    text context
    text statement
    string option_a
    string option_b
    string option_c
    string option_d
    string correct_answer
    timestamp published_at
    boolean available
  }
```

### simulation_db

```mermaid
erDiagram
  SIMULATION ||--|{ SIMULATION_ITEM : contiene
  SIMULATION {
    string id PK
    string student_id
    string status
    timestamp started_at
    timestamp due_at
    timestamp finished_at
    int duration_minutes
    int requested_count
    string competencies
    string topics
    string difficulties
    int total
    int correct
    int incorrect
    int unanswered
    decimal score
    text by_competency
  }
  SIMULATION_ITEM {
    long id PK
    string simulation_id FK
    int position
    string question_id
    int question_version
    string competency
    string difficulty
    text statement
    string correct_answer
    string answer
  }
```

`simulation_item` guarda una copia completa de la pregunta (título, tema, subtema, contexto y las cuatro opciones; aquí se
muestran solo algunas columnas): el simulacro no cambia aunque la pregunta se archive después. El catálogo ya no está aquí.

### statistics_db

```mermaid
erDiagram
  RESULT_ENTRY {
    string simulation_id PK
    string student_id
    timestamp started_at
    timestamp finished_at
    long duration_seconds
    boolean expired
    int total
    int correct
    int incorrect
    int unanswered
    decimal score
    text by_competency
  }
```

### report_db

```mermaid
erDiagram
  STUDENT_SUMMARY {
    string student_id PK
    int simulations
    decimal score_sum
    decimal average
    decimal best
    decimal last_score
    string trend
    text weaknesses
    text by_competency
    timestamp updated_at
  }
```

## Despliegue

El despliegue completo (con `edge`, Eureka, Keycloak y las réplicas) está en [08 · Implementación](08-implementacion-seguridad-y-resiliencia.md#topología-desplegada). Todo se levanta con `docker compose up -d --build` desde `microservicios/`; los servicios de negocio no publican puertos al exterior (solo `docker-compose.debug.yml`).
