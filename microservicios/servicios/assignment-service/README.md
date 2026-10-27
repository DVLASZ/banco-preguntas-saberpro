# assignment-service · Asignación de revisores

Abre una ronda de revisión por cada envío de una pregunta, deja que el administrador asigne revisores y arma la bandeja de cada revisor.

| | |
|---|---|
| **Puerto** | 8083 (`ASSIGNMENT_SERVICE_PORT`) |
| **Base de datos** | PostgreSQL `assignment_db` (puerto 5445 en local), con migraciones de Flyway |
| **Swagger** | `http://localhost:8080/docs/assignment-service/v3/api-docs` (a través del gateway) |
| **Contrato exportado** | [`openapi-assignment-service.json`](../../../docs/corte-2/api/openapi/openapi-assignment-service.json) y [colección de Postman](../../../docs/corte-2/api/postman/assignment-evaluation-services.postman_collection.json) |
| **Pruebas** | 96 pruebas (unitarias, de API, de persistencia y de arquitectura con ArchUnit) |

## Responsabilidad

Es el contexto de **Asignación de revisores** (HU-04) y de la **bandeja del revisor** (la consulta de HU-05). La decisión sobre la pregunta y las evaluaciones son de `evaluation-service`; aquí solo se recuerda quién evaluó (su decisión) y cómo terminó la ronda, para la bandeja y para saber cuándo una ronda ya no admite revisores.

## Reglas de negocio

- Cada ronda tiene de 1 a N revisores (`ASSIGNMENT_MAX_REVIEWERS`, 3 por defecto) y el autor nunca es uno de ellos.
- En una ronda nueva (tras un rechazo y la reapertura) se **reasignan solos los mismos revisores**; el administrador puede agregar más, no quitar.
- No se asigna a una ronda ya decidida (409); una asignación inválida responde 400 con el motivo.
- La bandeja separa pendientes y evaluadas; tras un rechazo, quien no evaluó sigue viéndola como pendiente (puede dejar una observación tardía).
- Un evento repetido no abre una segunda ronda ni cambia nada.

## API REST

Las rutas pasan por el gateway, que valida el token de Keycloak y comprueba el rol; el servicio vuelve a comprobarlo con el usuario autenticado que recibe.

| Método | Ruta | Qué hace |
|---|---|---|
| `GET` | `/api/reviews/assigned` | Mi bandeja de revisión (HU-05). Rol REVIEWER. |
| `GET` | `/api/reviews/config` | Límites de revisores por pregunta. Cualquier usuario autenticado. |
| `GET` | `/api/reviews/pending-assignment` | Preguntas que esperan revisores (HU-04). Rol ADMIN. |
| `POST` | `/api/reviews/{questionId}/assignments` | Asignar revisores a la ronda vigente, o agregar más a una en curso (HU-04). Rol ADMIN. |

## Mensajería (RabbitMQ)

**Publica** (con Outbox transaccional, en el exchange `bancopreguntas.events`):

| Evento | Cuándo | Quién lo consume |
|---|---|---|
| `review.reviewers-assigned` | El administrador asigna revisores, o el sistema reasigna los de la ronda anterior (con versión, título y competencia de la pregunta) | evaluation, lifecycle, notification, history, audit |

**Consume:**

| Cola | Routing key | Efecto |
|---|---|---|
| `assignment.q.question-submitted` | `question.submitted` | Abre el caso y la ronda; reasigna los revisores si ya hubo una ronda |
| `assignment.q.question-reopened` | `question.reopened` | Cierra la ronda rechazada |
| `assignment.q.evaluation-submitted` | `review.evaluation-submitted` | Anota la decisión del revisor (la bandeja lo muestra como evaluada) |
| `assignment.q.review-completed` | `review.completed` | La ronda queda aprobada o rechazada |

## Base de datos

| Migración | Contenido |
|---|---|
| `V1__esquema_inicial.sql` | assignment_db: casos de revisión, rondas, asignaciones de revisores, eventos procesados y bandeja de salida |

## Estructura

```
src/main/java/co/unicauca/bancopreguntas/assignment/
├── api/              Controlador REST, DTO y manejo de errores
├── application/      Casos de uso y puertos (interfaces que el dominio necesita)
├── domain/           Reglas de negocio y eventos; no depende de Spring ni de la base de datos
└── infrastructure/   Adaptadores: JPA, RabbitMQ, clientes REST y configuración
```

**Patrones de diseño que usa:** Adapter y Observer (eventos con Outbox). Detalle en [patrones](../../../docs/corte-2/05-patrones.md).

## Configuración

| Variable | Valor por defecto | Para qué sirve |
|---|---|---|
| `ASSIGNMENT_DB_URL` | `jdbc:postgresql://localhost:5445/assignment_db` | URL de la base de datos |
| `ASSIGNMENT_DB_USER` | `assignment` | Usuario de la base |
| `ASSIGNMENT_DB_PASSWORD` | `assignment` | Clave de la base |
| `RABBITMQ_HOST` | `localhost` | Servidor de RabbitMQ |
| `RABBITMQ_PORT` | `5672` | Puerto AMQP de RabbitMQ |
| `RABBITMQ_USER` | `guest` | Usuario de RabbitMQ |
| `RABBITMQ_PASSWORD` | `guest` | Clave de RabbitMQ |
| `ASSIGNMENT_SERVICE_PORT` | `8083` | Puerto HTTP |
| `EUREKA_ENABLED` | `true` | Registrarse en Eureka (se apaga en las pruebas) |
| `EUREKA_URL` | `http://localhost:8761/eureka` | Dirección de Eureka |
| `ASSIGNMENT_MAX_REVIEWERS` | `3` | Máximo de revisores por ronda |

## Ejecutar y probar

```bash
# desde microservicios/
./mvnw -pl servicios/assignment-service -am test          # pruebas del servicio
docker compose up -d --build assignment-service    # el servicio con sus dependencias
```

La pila completa se levanta como se explica en el [README de microservicios](../../README.md). Para recorrer el servicio a mano, importar su colección de Postman y el entorno `SaberPro_Local`.
