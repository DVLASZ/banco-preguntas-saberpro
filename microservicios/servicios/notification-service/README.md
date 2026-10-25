# notification-service · Notificaciones

Convierte lo que ocurre en la revisión en avisos de la bandeja de cada usuario y publica un evento por cada aviso creado.

| | |
|---|---|
| **Puerto** | 8084 (`NOTIFICATION_SERVICE_PORT`) |
| **Base de datos** | PostgreSQL `notification_db` (puerto 5436 en local), con migraciones de Flyway |
| **Swagger** | `http://localhost:8080/docs/notification-service/v3/api-docs` (a través del gateway) |
| **Contrato exportado** | [`openapi-notification-service.json`](../../../docs/corte-2/api/openapi/openapi-notification-service.json) y [colección de Postman](../../../docs/corte-2/api/postman/notification-service.postman_collection.json) |
| **Pruebas** | 91 pruebas (unitarias, de API, de persistencia y de arquitectura con ArchUnit) |

## Responsabilidad

Es el contexto de **Notificaciones**. Escucha los eventos de preguntas y revisión y crea el aviso que corresponde al autor, al administrador y a los revisores. **No envía correos**: publica `notification.created` y `email-service` se encarga.

## Reglas de negocio

- Cada tipo de aviso lo crea una fábrica distinta (patrón Factory Method): revisores asignados, pregunta en revisión, evaluación recibida, resultado de la revisión, observación tardía, pregunta publicada.
- Cada usuario ve solo sus avisos; puede marcarlos como leídos uno a uno o todos, y consultar cuántos tiene sin leer.
- Un evento repetido no duplica avisos (idempotencia por `eventId`).

## API REST

Las rutas pasan por el gateway, que valida el token de Keycloak y comprueba el rol; el servicio vuelve a comprobarlo con el usuario autenticado que recibe.

| Método | Ruta | Qué hace |
|---|---|---|
| `GET` | `/api/notifications` | Mi bandeja de notificaciones, la más reciente primero. ADMIN, AUTHOR o REVIEWER. |
| `POST` | `/api/notifications/read-all` | Marcar todas mis notificaciones como leídas. |
| `GET` | `/api/notifications/unread-count` | Cuántas notificaciones tengo sin leer. |
| `POST` | `/api/notifications/{id}/read` | Marcar una notificación mía como leída. |

## Mensajería (RabbitMQ)

**Publica** (con Outbox transaccional, en el exchange `bancopreguntas.events`):

| Evento | Cuándo | Quién lo consume |
|---|---|---|
| `notification.created` | Se crea un aviso en la bandeja de un usuario | email |

**Consume:**

| Cola | Routing key | Efecto |
|---|---|---|
| `notification.q.question-submitted` | `question.submitted` | Avisa al autor que su pregunta está en revisión |
| `notification.q.reviewers-assigned` | `review.reviewers-assigned` | Avisa a los revisores asignados |
| `notification.q.evaluation-submitted` | `review.evaluation-submitted` | Avisa al autor la evaluación o la observación tardía |
| `notification.q.review-completed` | `review.completed` | Avisa el resultado al autor, al administrador y a los revisores |
| `notification.q.question-published` | `question.published` | Avisa al autor que su pregunta se publicó |

## Base de datos

| Migración | Contenido |
|---|---|
| `V1__esquema_inicial.sql` | notification_db: bandeja de notificaciones, correos simulados y datos mínimos de las preguntas |
| `V2__administrador_de_la_pregunta.sql` | Quién asignó los revisores de la pregunta (aprendido de review.reviewers-assigned), para avisarle de las evaluaciones y del resultado de la revisión |
| `V3__correo_y_bandeja_de_salida.sql` | El correo pasó al email-service: la notificación ya no guarda la dirección ni la marca de envío |

## Estructura

```
src/main/java/co/unicauca/bancopreguntas/notification/
├── api/              Controlador REST, DTO y manejo de errores
├── application/      Casos de uso y puertos (interfaces que el dominio necesita)
├── domain/           Reglas de negocio y eventos; no depende de Spring ni de la base de datos
└── infrastructure/   Adaptadores: JPA, RabbitMQ, clientes REST y configuración
```

**Patrones de diseño que usa:** Factory Method (`CreadorDeAviso` y sus creadores), Adapter y Observer (eventos con Outbox). Detalle en [patrones](../../../docs/corte-2/05-patrones.md).

## Configuración

| Variable | Valor por defecto | Para qué sirve |
|---|---|---|
| `NOTIFICATION_DB_URL` | `jdbc:postgresql://localhost:5436/notification_db` | URL de la base de datos |
| `NOTIFICATION_DB_USER` | `notification` | Usuario de la base |
| `NOTIFICATION_DB_PASSWORD` | `notification` | Clave de la base |
| `RABBITMQ_HOST` | `localhost` | Servidor de RabbitMQ |
| `RABBITMQ_PORT` | `5672` | Puerto AMQP de RabbitMQ |
| `RABBITMQ_USER` | `guest` | Usuario de RabbitMQ |
| `RABBITMQ_PASSWORD` | `guest` | Clave de RabbitMQ |
| `NOTIFICATION_SERVICE_PORT` | `8084` | Puerto HTTP |
| `EUREKA_ENABLED` | `true` | Registrarse en Eureka (se apaga en las pruebas) |
| `EUREKA_URL` | `http://localhost:8761/eureka` | Dirección de Eureka |

## Ejecutar y probar

```bash
# desde microservicios/
./mvnw -pl servicios/notification-service -am test          # pruebas del servicio
docker compose up -d --build notification-service    # el servicio con sus dependencias
```

La pila completa se levanta como se explica en el [README de microservicios](../../README.md). Para recorrer el servicio a mano, importar su colección de Postman y el entorno `SaberPro_Local`.
