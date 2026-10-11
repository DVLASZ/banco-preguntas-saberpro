# 02 · Contextos delimitados y eventos

## Contextos delimitados

El docente propone ocho contextos como aproximación. Para las historias de esta iteración (HU-01 a
HU-05) implementamos cinco servicios más el gateway, y dejamos documentados los demás.

```mermaid
flowchart TB
  subgraph Implementados
    US["Gestión de Usuarios<br/>user-service"]
    QS["Gestión de Preguntas, Validación<br/>y Ciclo de Vida<br/>question-service"]
    RS["Revisión por Pares<br/>review-service"]
    NS["Notificaciones<br/>notification-service"]
    AS["Administración y Trazabilidad<br/>history-service"]
  end
  subgraph Futuros
    SIM["Simulacros de Evaluación"]
    SEG["Seguimiento Académico"]
  end
  QS -- "question.submitted" --> RS
  RS -- "review.reviewers-assigned<br/>review.completed" --> QS
  RS -- "eventos de revisión" --> NS
  QS -- "question.state-changed" --> AS
  US -. "datos de usuario" .-> RS
  QS -. "preguntas publicadas" .-> SIM
  SIM -. "resultados" .-> SEG
```

| Contexto del docente | Servicio | Alcance |
|---|---|---|
| Gestión de Usuarios | `user-service` | Sincroniza los usuarios y roles de Keycloak y publica los eventos de acceso |
| Gestión de Preguntas | `question-service` | HU-01 a HU-03 |
| Validación de Preguntas | `question-service` | Módulo interno (reglas de validación estructural) |
| Ciclo de Vida de Preguntas | `question-service` | Módulo interno (máquina de estados) |
| Revisión por Pares | `review-service` | HU-04 y HU-05 |
| (transversal) | `notification-service` | Correo y bandeja de notificaciones |
| Administración y Trazabilidad | `history-service` | Historial por pregunta y auditoría de accesos (consume todos los eventos) |
| Simulacros de Evaluación | — | Corte 3 |
| Seguimiento Académico | — | Corte 3 |

**Por qué Validación y Ciclo de Vida no son servicios aparte.** Cada guardado de pregunta ejecuta la
validación y valida la transición de estado. Separarlos añadiría una llamada de red por operación sin
ninguna ventaja de escalado: son reglas que cambian junto con la pregunta. Se conservan como módulos
internos con límites claros.

## Eventos

Comunicación asíncrona por RabbitMQ: exchange `bancopreguntas.events` de tipo *topic*. Cada mensaje lleva
un sobre común y un cuerpo pequeño con identificadores, no copias de las entidades.

```json
{
  "eventId": "uuid",
  "type": "review.completed",
  "occurredAt": "2026-10-06T10:15:30Z",
  "data": {}
}
```

| Evento | Lo publica | Lo consumen | Efecto |
|---|---|---|---|
| `question.submitted` | question-service | review-service, history-service | Crea el caso de revisión pendiente de asignar |
| `review.reviewers-assigned` | review-service | question-service, notification-service, history-service | La pregunta pasa a "En revisión"; se avisa a cada revisor |
| `review.evaluation-submitted` | review-service | notification-service, history-service | Se avisa al administrador y al autor de la nueva evaluación |
| `review.completed` | review-service | question-service, notification-service, history-service | La pregunta pasa a Aprobada o Rechazada; se avisa a todos |
| `question.state-changed` | question-service | history-service | Traza del ciclo de vida |
| `question.reopened` | question-service | review-service, history-service | El autor corrige una pregunta rechazada u observada y abre una nueva ronda de revisión |
| `question.published` | question-service | history-service, notification-service | El administrador publica una pregunta aprobada |
| `auth.login-succeeded` / `auth.login-failed` | user-service | history-service | Auditoría de inicios de sesión, tomados de los eventos de Keycloak |

Cuerpo (`data`) de cada evento:

| Evento | Campos |
|---|---|
| `question.submitted` | `questionId`, `authorId`, `title`, `competency` |
| `review.reviewers-assigned` | `questionId`, `authorId`, `reviewerIds[]`, `assignedBy` |
| `review.evaluation-submitted` | `questionId`, `reviewerId`, `decision`, `observation` |
| `review.completed` | `questionId`, `authorId`, `result` (`APPROVED` o `REJECTED`), `observations[]` |
| `question.state-changed` | `questionId`, `from`, `to`, `changedBy` |

Garantías: mensajes persistentes con confirmación de entrega y consumidores **idempotentes** (se ignora un
`eventId` ya procesado).

## Flujo principal

```mermaid
sequenceDiagram
  participant A as Autor
  participant QS as question-service
  participant MQ as RabbitMQ
  participant RS as review-service
  participant NS as notification-service
  participant R as Revisor
  A->>QS: Enviar a revisión
  QS->>MQ: question.submitted
  MQ->>RS: question.submitted
  Note over RS: El administrador asigna de 1 a 3 revisores
  RS->>MQ: review.reviewers-assigned
  MQ->>QS: pasa a En revisión
  MQ->>NS: correo a cada revisor
  R->>RS: Evalúa, observa y decide
  RS->>MQ: review.completed
  MQ->>QS: pasa a Aprobada o Rechazada
  MQ->>NS: correo al autor y al administrador
```

## Cambios respecto al diseño inicial (implementación)

- El servicio opcional de auditoría se implementó como **`history-service`**: guarda el historial de cada pregunta
  y la auditoría de accesos, y alimenta la pantalla de historial y de auditoría del administrador.
- Se añadieron los eventos `question.reopened`, `question.published` y `auth.*` (tabla de arriba). Los eventos de
  revisión llevan además el número de **ronda** y, cuando corresponde, las banderas `automatic` (reasignación
  automática de revisores) y `late` (observación recibida cuando la ronda ya se había cerrado).
- Los eventos de `question-service`, `review-service` y `user-service` se publican con el **patrón Outbox** (se
  guardan en la misma transacción que el cambio y un relevo los envía), de modo que no se pierden si RabbitMQ está
  caído.
- Un mensaje que falla tras 4 reintentos va a la cola de mensajes muertos `bancopreguntas.dead-letter`.
