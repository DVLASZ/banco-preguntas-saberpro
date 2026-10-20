# notification-service · Notificaciones

> **Primera distribución (7 servicios).** Este README describe el servicio tal como está publicado hoy. En la nueva distribución, de 14
> servicios, se divide en `notification-service` (bandeja) y `email-service` (correo); ver el [README de los servicios](../README.md).

Avisa a cada usuario de lo que ocurre con sus preguntas y revisiones: una bandeja dentro de la aplicación y un correo simulado, sin bloquear nunca a quien origina el evento.

| | |
|---|---|
| **Puerto** | 8084 (`NOTIFICATION_SERVICE_PORT`) |
| **Base de datos** | PostgreSQL `notification_db` (puerto 5436 en local), con migraciones de Flyway |
| **Swagger** | `http://localhost:8080/docs/notification-service/v3/api-docs` (a través del gateway) |
| **Contrato exportado** | [`openapi-notification-service.json`](../../../docs/corte-2/api/openapi/openapi-notification-service.json) y [colección de Postman](../../../docs/corte-2/api/postman/notification-service.postman_collection.json) |
| **Pruebas** | 74 pruebas (unitarias, de API, de persistencia y de arquitectura con ArchUnit) |

## Responsabilidad

Es el contexto transversal de **Notificaciones**. Escucha los eventos del flujo de revisión, decide a quién avisar y con qué texto, guarda la notificación en la bandeja del destinatario y «envía» el correo (queda registrado y consultable por el administrador, pero no sale a un servidor real).

## Reglas de negocio

- Al **autor** le avisa cuando su pregunta entra en revisión, cuando se aprueba o se rechaza (con las observaciones de los revisores)
  y cuando se publica; si llega una evaluación cuando la ronda ya estaba rechazada, le avisa como observación tardía.
- Al **revisor** le avisa cuando se le asigna una pregunta (de forma manual o automática) y cuando la revisión termina.
- Al **administrador** le avisa de cada evaluación recibida a tiempo y del resultado de la revisión.
- Cada evento se procesa una sola vez (idempotencia por `eventId`).
- Cada usuario solo ve y marca como leídas sus notificaciones; el administrador consulta los correos simulados enviados.

## API REST

Todas las rutas pasan por el gateway, que valida el token de Keycloak y comprueba el rol; el servicio vuelve a
comprobarlo con el usuario autenticado que recibe.

| Método | Ruta | Qué hace |
|---|---|---|
| `GET` | `/api/notifications` | Mi bandeja de notificaciones, la más reciente primero. ADMIN, AUTHOR o REVIEWER. |
| `GET` | `/api/notifications/emails` | Correos simulados enviados, el más reciente primero. Rol ADMIN. |
| `POST` | `/api/notifications/read-all` | Marcar todas mis notificaciones como leídas. |
| `GET` | `/api/notifications/unread-count` | Cuántas notificaciones tengo sin leer. |
| `POST` | `/api/notifications/{id}/read` | Marcar una notificación mía como leída. |

## Mensajería (RabbitMQ)

**Publica:** nada; es un consumidor puro.

**Consume:**

| Cola | Routing key | Aviso que genera |
|---|---|---|
| `notification.q.question-submitted` | `question.submitted` | Ninguno: solo recuerda el título y el autor de la pregunta |
| `notification.q.question-published` | `question.published` | `PREGUNTA_PUBLICADA` (al autor) |
| `notification.q.reviewers-assigned` | `review.reviewers-assigned` | `PREGUNTA_EN_REVISION` (al autor) y `REVISORES_ASIGNADOS` (a cada revisor) |
| `notification.q.evaluation-submitted` | `review.evaluation-submitted` | `EVALUACION_RECIBIDA` (al administrador) u `OBSERVACION_TARDIA` (al autor) |
| `notification.q.review-completed` | `review.completed` | `REVISION_APROBADA` o `REVISION_RECHAZADA` (al autor) y `RESULTADO_DE_REVISION` (al administrador y a los revisores) |

## Base de datos

| Migración | Contenido |
|---|---|
| `V1__esquema_inicial.sql` | Notificaciones, datos mínimos de cada pregunta y eventos ya procesados |
| `V2__administrador_de_la_pregunta.sql` | Datos de la pregunta (incluido su administrador) para armar los avisos |

## Estructura

```
src/main/java/co/unicauca/bancopreguntas/notification/
├── api/              Controlador REST, DTO y manejo de errores
├── application/      Casos de uso y puertos (interfaces que el dominio necesita)
├── domain/           Reglas de negocio y eventos; no depende de Spring ni de la base de datos
└── infrastructure/   Adaptadores: JPA, RabbitMQ, correo simulado (decoradores)
```

**Patrones de diseño que usa:** Factory Method (`CreadorDeAviso` y ocho creadores, uno por tipo de aviso), Decorator
(`EmailConReintentos` y `EmailConRegistro` envuelven al envío de correo), Adapter y Observer.

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
| `NOTIFICATION_EMAIL_DOMAIN` | `bancopreguntas.local` | Dominio de las direcciones de los correos simulados |
| `NOTIFICATION_EMAIL_ATTEMPTS` | `3` | Intentos de envío del correo antes de darlo por fallido |
| `EUREKA_ENABLED` | `true` | Registrarse en Eureka (se apaga en las pruebas) |
| `EUREKA_URL` | `http://localhost:8761/eureka` | Dirección de Eureka |

## Ejecutar y probar

```bash
# desde microservicios/
./mvnw -pl servicios/notification-service -am test          # pruebas del servicio
docker compose up -d --build notification-service    # el servicio con sus dependencias (base, RabbitMQ, Eureka, Keycloak)
```

La pila completa se levanta como se explica en el [README de microservicios](../../README.md). Para recorrer el
servicio a mano, importar su colección de Postman y el entorno `SaberPro_Local`.
