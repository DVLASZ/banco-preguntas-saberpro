# 03 · Arquitectura (C4 y UML)

> Cada diagrama de este documento también está como imagen (SVG y PNG) en [`img/diagramas`](img/diagramas/), lista para el documento formal en Word o PDF.

## Estilo arquitectónico

Microservicios orientados a eventos con un API Gateway como único punto de entrada, una base de datos
por servicio y un broker de mensajes para la comunicación entre servicios. Frontend y backend están
separados y se comunican solo por API REST.

## C4 · Nivel 1: contexto del sistema

```mermaid
flowchart LR
  AUT["Autor de preguntas"]
  ADM["Administrador"]
  REV["Revisor"]
  SYS["Sistema Banco de Preguntas<br/>Saber Pro"]
  MAIL["Servidor de correo SMTP"]
  AUT -- "crea, envía y consulta preguntas" --> SYS
  ADM -- "asigna revisores" --> SYS
  REV -- "evalúa preguntas" --> SYS
  SYS -- "envía notificaciones" --> MAIL
```

## C4 · Nivel 2: contenedores

```mermaid
flowchart TB
  FE["Frontend web<br/>Angular"]
  EDGE["edge<br/>nginx (balanceador)"]
  KC["Keycloak<br/>identidad (JWT)"]
  EU["discovery-server<br/>Eureka"]
  GW["api-gateway<br/>Spring Cloud Gateway<br/>valida token y rol"]
  US["user-service<br/>Spring Boot"]
  QS["question-service<br/>Spring Boot"]
  RS["review-service<br/>Spring Boot"]
  NS["notification-service<br/>Spring Boot"]
  AS["history-service<br/>Spring Boot"]
  MQ{{"RabbitMQ<br/>exchange bancopreguntas.events"}}
  SMTP["Servidor SMTP"]
  DU[("user_db<br/>PostgreSQL")]
  DQ[("question_db<br/>PostgreSQL")]
  DR[("review_db<br/>PostgreSQL")]
  DN[("notification_db<br/>PostgreSQL")]
  DA[("history_db<br/>PostgreSQL")]
  FE -- "REST/JSON" --> EDGE
  FE -. "login PKCE" .-> KC
  EDGE --> GW
  GW -. "registro" .-> EU
  GW -. "claves públicas" .-> KC
  US -. "eventos de acceso" .-> KC
  GW --> AS
  GW --> US
  GW --> QS
  GW --> RS
  GW --> NS
  US --- DU
  QS --- DQ
  RS --- DR
  NS --- DN
  AS --- DA
  QS <-. "eventos" .-> MQ
  RS <-. "eventos" .-> MQ
  NS <-. "eventos" .-> MQ
  AS <-. "eventos" .-> MQ
  NS -- "SMTP" --> SMTP
```

| Contenedor | Responsabilidad | Puerto |
|---|---|---|
| edge | Balanceador nginx delante de las réplicas del gateway | 8080 |
| api-gateway | Enrutamiento por Eureka, validación del token, autorización por rol, circuit breaker y reintentos | interno |
| discovery-server | Registro de servicios (Eureka) | 8761 |
| keycloak | Proveedor de identidad: inicio de sesión, roles y token JWT | 8180 |
| user-service | Sincroniza usuarios y roles de Keycloak; consulta sus eventos de acceso | 8081 (depuración) |
| question-service | Preguntas, validación estructural y ciclo de vida | 8082 (depuración) |
| review-service | Asignación de revisores, rondas de evaluación y observaciones | 8083 (depuración) |
| notification-service | Consume eventos, envía correo y guarda la bandeja | 8084 (depuración) |
| history-service | Historial por pregunta y auditoría de accesos | 8085 (depuración) |
| RabbitMQ | Broker de eventos | 5672, panel 15672 |

## C4 · Nivel 3: componentes

Todos los servicios siguen la misma organización interna: `api`, `application`, `domain` e
`infrastructure`. El dominio no depende de Spring, de modo que en el corte 3 el paso a hexagonal sea un
refinamiento y no una reescritura.

### review-service

```mermaid
flowchart LR
  subgraph api
    RC["ReviewController"]
  end
  subgraph application
    AR["AssignReviewersUseCase"]
    SE["SubmitEvaluationUseCase"]
    RQ["ReviewQueryService"]
  end
  subgraph domain
    CASE["ReviewCase"]
    ASG["Assignment"]
    EVA["Evaluation"]
    POL["DecisionPolicy<br/>(UnanimityPolicy)"]
    PR["ReviewRepository (puerto)"]
    PE["EventPublisher (puerto)"]
  end
  subgraph infrastructure
    JPA["JpaReviewRepository"]
    PUB["RabbitEventPublisher"]
    LIS["QuestionSubmittedListener"]
  end
  RC --> AR
  RC --> SE
  RC --> RQ
  AR --> CASE
  SE --> CASE
  CASE --> ASG
  ASG --> EVA
  CASE --> POL
  AR --> PR
  SE --> PR
  AR --> PE
  SE --> PE
  JPA -. implementa .-> PR
  PUB -. implementa .-> PE
  LIS --> AR
```

### question-service

```mermaid
flowchart LR
  subgraph api
    QC["QuestionController"]
  end
  subgraph application
    QSV["QuestionService"]
    SUB["SubmitForReviewUseCase"]
  end
  subgraph domain
    Q["Question (Builder)"]
    EST["EstadoPregunta"]
    VAL["QuestionValidator<br/>+ reglas"]
    QR["QuestionRepository (puerto)"]
    QP["EventPublisher (puerto)"]
  end
  subgraph infrastructure
    QJ["JpaQuestionRepository"]
    QPUB["RabbitEventPublisher"]
    QL["ReviewEventsListener"]
  end
  QC --> QSV
  QC --> SUB
  QSV --> Q
  QSV --> VAL
  Q --> EST
  QSV --> QR
  SUB --> QP
  QJ -. implementa .-> QR
  QPUB -. implementa .-> QP
  QL --> QSV
```

## Máquina de estados de la pregunta

```mermaid
stateDiagram-v2
  [*] --> Borrador
  Borrador --> PendienteDeRevision: autor envía
  PendienteDeRevision --> EnRevision: administrador asigna revisores
  EnRevision --> Aprobada: todos aprueban
  EnRevision --> Rechazada: algún rechazo
  Rechazada --> Borrador: autor corrige
  Aprobada --> Publicada
  Borrador --> Archivada
  PendienteDeRevision --> Archivada
  EnRevision --> Archivada
  Aprobada --> Archivada
  Rechazada --> Archivada
  Publicada --> Archivada
```

*Pendiente: quién publica y archiva, y qué transiciones no tienen retorno (consulta con el profesor
Libardo). La tabla es la del primer corte.*

## Secuencia · HU-04: asignar revisores

```mermaid
sequenceDiagram
  actor ADM as Administrador
  participant GW as api-gateway
  participant RS as review-service
  participant DB as review_db
  participant MQ as RabbitMQ
  participant QS as question-service
  participant NS as notification-service
  ADM->>GW: POST /api/reviews/{id}/assignments (1 a 3 revisores)
  GW->>GW: valida token y rol ADMIN
  GW->>RS: reenvía la petición
  RS->>RS: valida 1 a 3, sin autor y sin repetidos
  RS->>DB: guarda caso y asignaciones
  RS->>MQ: review.reviewers-assigned
  RS-->>ADM: 201 Created
  MQ->>QS: pasa a En revisión
  MQ->>NS: correo a cada revisor
```

## Secuencia · HU-05: evaluar una pregunta

```mermaid
sequenceDiagram
  actor REV as Revisor
  participant GW as api-gateway
  participant RS as review-service
  participant DB as review_db
  participant MQ as RabbitMQ
  participant QS as question-service
  participant NS as notification-service
  REV->>GW: POST /api/reviews/{id}/evaluations (decisión y observación)
  GW->>RS: valida rol REVIEWER y reenvía
  RS->>RS: verifica que esté asignado y no haya evaluado
  RS->>DB: guarda evaluación y observación
  RS->>MQ: review.evaluation-submitted
  alt todos evaluaron o hubo rechazo
    RS->>RS: DecisionPolicy decide (unanimidad)
    RS->>MQ: review.completed
    MQ->>QS: Aprobada o Rechazada
    MQ->>NS: correo al autor y al administrador
  end
  RS-->>REV: 201 Created
```

## Modelo de datos por servicio

Los servicios se refieren entre sí solo por identificador, sin claves foráneas entre bases. No hay borrado
físico (RNF-16): se usan estados y fechas.

### user_db

```mermaid
erDiagram
  APP_USER ||--o{ USER_ROLE : tiene
  ROLE ||--o{ USER_ROLE : asigna
  APP_USER {
    uuid id PK
    string username
    string email
    string password_hash
    string full_name
    boolean active
  }
  ROLE {
    int id PK
    string name
  }
  USER_ROLE {
    uuid user_id FK
    int role_id FK
  }
```

### question_db

El modelo conserva las cuatro opciones y la letra de la respuesta correcta, igual que el dominio del primer corte
(`ContenidoPregunta`), para reutilizarlo sin cambios.

```mermaid
erDiagram
  QUESTION ||--o{ QUESTION_STATE_HISTORY : registra
  QUESTION {
    uuid id PK
    uuid author_id
    string nombre
    string contexto
    string enunciado
    string opcion_a
    string opcion_b
    string opcion_c
    string opcion_d
    string respuesta_correcta
    string justificacion
    string bibliografia
    string competencia
    string tema
    string subtema
    string dificultad
    string estado
    timestamp creada_en
    timestamp actualizada_en
  }
  QUESTION_STATE_HISTORY {
    uuid id PK
    uuid question_id FK
    string desde
    string hacia
    uuid cambiado_por
    timestamp fecha
  }
```

### review_db

```mermaid
erDiagram
  REVIEW_CASE ||--|{ ASSIGNMENT : tiene
  ASSIGNMENT ||--o| EVALUATION : produce
  EVALUATION ||--o{ OBSERVATION : incluye
  REVIEW_CASE {
    uuid id PK
    uuid question_id
    uuid author_id
    string status
    string result
    timestamp created_at
    timestamp closed_at
  }
  ASSIGNMENT {
    uuid id PK
    uuid review_case_id FK
    uuid reviewer_id
    uuid assigned_by
    timestamp assigned_at
  }
  EVALUATION {
    uuid id PK
    uuid assignment_id FK
    string decision
    timestamp submitted_at
  }
  OBSERVATION {
    uuid id PK
    uuid evaluation_id FK
    string text
    timestamp created_at
  }
```

### notification_db y history_db

```mermaid
erDiagram
  NOTIFICATION {
    uuid id PK
    uuid recipient_id
    string recipient_email
    string type
    string subject
    string body
    uuid question_id
    boolean is_read
    string email_status
    timestamp created_at
  }
  PROCESSED_EVENT {
    uuid event_id PK
    timestamp processed_at
  }
  HISTORY_ENTRY {
    uuid id PK
    uuid event_id
    string type
    uuid actor_id
    string payload
    timestamp occurred_at
  }
```

Todos los consumidores guardan `PROCESSED_EVENT` (idempotencia). `question_db`, `review_db` y `user_db` guardan además `OUTBOX_EVENT` (patrón Outbox: `seq`, `event_id`, `routing_key`, `payload`, `created_at`, `sent_at`).

## Despliegue

El despliegue completo (con `edge`, Eureka, Keycloak y las réplicas) se levanta con `docker compose up -d --build` desde `microservicios/`; los servicios de negocio no publican puertos al exterior (solo `docker-compose.debug.yml`).
