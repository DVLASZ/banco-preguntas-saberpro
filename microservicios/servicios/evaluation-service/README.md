# evaluation-service · Evaluación por pares

Recibe la evaluación de cada revisor, decide la ronda por unanimidad y publica el resultado de la revisión.

| | |
|---|---|
| **Puerto** | 8096 (`EVALUATION_SERVICE_PORT`) |
| **Base de datos** | PostgreSQL `evaluation_db` (puerto 5446 en local), con migraciones de Flyway |
| **Swagger** | `http://localhost:8080/docs/evaluation-service/v3/api-docs` (a través del gateway) |
| **Contrato exportado** | [`openapi-evaluation-service.json`](../../../docs/corte-2/api/openapi/openapi-evaluation-service.json) y [colección de Postman](../../../docs/corte-2/api/postman/assignment-evaluation-services.postman_collection.json) |
| **Pruebas** | 107 pruebas (unitarias, de API, de persistencia y de arquitectura con ArchUnit) |

## Responsabilidad

Es el contexto de **Evaluación** (HU-05). Guarda una copia de cada ronda (con los revisores que informa `assignment-service`) y las evaluaciones, que son definitivas. Decide con una política (patrón Strategy) y avisa el resultado.

## Reglas de negocio

- Un revisor solo evalúa si está asignado a la ronda (403) y una sola vez (409); la observación es obligatoria al rechazar y tiene un máximo de 4000 caracteres.
- **Unanimidad:** la ronda se aprueba cuando todos los revisores asignados aprueban; un solo rechazo la rechaza sin esperar a los demás.
- Tras un rechazo la ronda sigue abierta: los revisores que faltaban pueden dejar una observación **tardía**, que no cambia el resultado, hasta que el autor reabre la pregunta.
- El administrador y los revisores ven toda la revisión; el autor, el detalle de las rondas ya decididas.
- Si dos revisores evalúan a la vez, la ronda se decide una sola vez (el caso se bloquea durante la transacción).

## API REST

Las rutas pasan por el gateway, que valida el token de Keycloak y comprueba el rol; el servicio vuelve a comprobarlo con el usuario autenticado que recibe.

| Método | Ruta | Qué hace |
|---|---|---|
| `GET` | `/api/reviews/{questionId}` | La revisión de una pregunta según quién consulta. ADMIN, AUTHOR o REVIEWER. |
| `POST` | `/api/reviews/{questionId}/evaluations` | Enviar mi evaluación de la pregunta (HU-05). Rol REVIEWER. |

## Mensajería (RabbitMQ)

**Publica** (con Outbox transaccional, en el exchange `bancopreguntas.events`):

| Evento | Cuándo | Quién lo consume |
|---|---|---|
| `review.evaluation-submitted` | Un revisor envía su evaluación (`late` marca las tardías) | assignment, notification, history, audit |
| `review.completed` | La ronda se decide (APPROVED o REJECTED) con las observaciones recibidas | assignment, lifecycle, notification, history, audit |

**Consume:**

| Cola | Routing key | Efecto |
|---|---|---|
| `evaluation.q.question-submitted` | `question.submitted` | Abre el caso y la ronda de esa versión |
| `evaluation.q.question-reopened` | `question.reopened` | Cierra la ronda rechazada |
| `evaluation.q.reviewers-assigned` | `review.reviewers-assigned` | Registra los revisores de la ronda; la ronda queda en curso |

## Base de datos

| Migración | Contenido |
|---|---|
| `V1__esquema_inicial.sql` | evaluation_db: casos de revisión, rondas, revisores asignados (los informa assignment-service), evaluaciones, eventos procesados y bandeja de salida |

## Estructura

```
src/main/java/co/unicauca/bancopreguntas/evaluation/
├── api/              Controlador REST, DTO y manejo de errores
├── application/      Casos de uso y puertos (interfaces que el dominio necesita)
├── domain/           Reglas de negocio y eventos; no depende de Spring ni de la base de datos
└── infrastructure/   Adaptadores: JPA, RabbitMQ, clientes REST y configuración
```

**Patrones de diseño que usa:** Strategy (`PoliticaDeDecision` y `Unanimidad`), Adapter y Observer (eventos con Outbox). Detalle en [patrones](../../../docs/corte-2/05-patrones.md).

## Configuración

| Variable | Valor por defecto | Para qué sirve |
|---|---|---|
| `EVALUATION_DB_URL` | `jdbc:postgresql://localhost:5446/evaluation_db` | URL de la base de datos |
| `EVALUATION_DB_USER` | `evaluation` | Usuario de la base |
| `EVALUATION_DB_PASSWORD` | `evaluation` | Clave de la base |
| `RABBITMQ_HOST` | `localhost` | Servidor de RabbitMQ |
| `RABBITMQ_PORT` | `5672` | Puerto AMQP de RabbitMQ |
| `RABBITMQ_USER` | `guest` | Usuario de RabbitMQ |
| `RABBITMQ_PASSWORD` | `guest` | Clave de RabbitMQ |
| `EVALUATION_SERVICE_PORT` | `8096` | Puerto HTTP |

## Ejecutar y probar

```bash
# desde microservicios/
./mvnw -pl servicios/evaluation-service -am test          # pruebas del servicio
docker compose up -d --build evaluation-service    # el servicio con sus dependencias
```

La pila completa se levanta como se explica en el [README de microservicios](../../README.md). Para recorrer el servicio a mano, importar su colección de Postman y el entorno `SaberPro_Local`.
