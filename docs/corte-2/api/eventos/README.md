# Contrato de los eventos

[`eventos.schema.json`](eventos.schema.json) describe, en JSON Schema, el sobre común y los **diez eventos** que se intercambian los
servicios por RabbitMQ (exchange *topic* `bancopreguntas.events`; la routing key es el tipo del evento).

| Evento | Lo publica | Lo consumen |
|---|---|---|
| `question.submitted` | lifecycle-service | assignment, evaluation, notification, history, audit |
| `question.reopened` | lifecycle-service | assignment, evaluation, history, audit |
| `question.published` (con el contenido de la pregunta) | lifecycle-service | notification, history, audit |
| `question.state-changed` (con `sequence` y `version`) | lifecycle-service | question, history, audit |
| `review.reviewers-assigned` (con versión, título y competencia) | assignment-service | evaluation, lifecycle, notification, history, audit |
| `review.evaluation-submitted` | evaluation-service | assignment, notification, history, audit |
| `review.completed` | evaluation-service | assignment, lifecycle, notification, history, audit |
| `notification.created` | notification-service | email |
| `auth.login-succeeded` | user-service | audit |
| `auth.login-failed` | user-service | audit |

El sobre es `{eventId, type, occurredAt, data}`. Los consumidores descartan un evento repetido por su `eventId`. Todos los servicios que
publican lo hacen por el patrón **Outbox**: el evento se guarda en la misma transacción que el cambio y un relevo lo envía al broker.
`validation-service` no publica ni escucha eventos.

Los cambios de esta versión sobre los eventos anteriores solo **agregan** campos (`sequence` y `version` en `question.state-changed`;
`questionVersion`, `title` y `competency` en `review.reviewers-assigned`), así que los consumidores existentes siguen funcionando.
