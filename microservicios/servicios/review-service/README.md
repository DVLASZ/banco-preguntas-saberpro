# review-service · Revisión por pares

> **Primera distribución (7 servicios).** Este README describe el servicio tal como está publicado hoy. En la nueva distribución, de 14
> servicios, se divide en `assignment-service` (asignación de revisores) y `evaluation-service` (evaluación y unanimidad); ver el [README de los servicios](../README.md).

Organiza la revisión por pares: el administrador asigna revisores, cada revisor evalúa y la pregunta se aprueba solo si todos aprueban.

| | |
|---|---|
| **Puerto** | 8083 (`REVIEW_SERVICE_PORT`) |
| **Base de datos** | PostgreSQL `review_db` (puerto 5435 en local), con migraciones de Flyway |
| **Swagger** | `http://localhost:8080/docs/review-service/v3/api-docs` (a través del gateway) |
| **Contrato exportado** | `openapi-review-service.json` y colección de Postman |
| **Pruebas** | 102 pruebas (unitarias, de API, de persistencia y de arquitectura con ArchUnit) |

## Responsabilidad

Es el contexto de **Revisión por Pares** (HU-04, HU-05 y HU-06). Abre un caso de revisión cuando llega una pregunta enviada, asigna revisores a la ronda vigente, recibe las evaluaciones, decide el resultado y lleva las rondas (una nueva por cada vez que el autor corrige y reenvía). No cambia el estado de la pregunta: informa lo ocurrido por eventos y `question-service` actúa.

## Reglas de negocio

- El administrador asigna de **1 a 3 revisores** por pregunta (`REVIEW_MAX_REVIEWERS`, 3 por defecto); no se repite ninguno y
  el autor nunca puede revisar su propia pregunta.
- La decisión es **por unanimidad** (patrón Strategy: `PoliticaDeDecision` y `Unanimidad`): aprueba si todos aprueban; un solo rechazo
  cierra la ronda y la pregunta queda rechazada. Rechazar exige una observación.
- Las observaciones de todos los revisores de la misma pregunta son visibles entre ellos y para el autor.
- Se puede agregar más revisores a una ronda en curso mientras no se pase del máximo.

## API REST

Todas las rutas pasan por el gateway, que valida el token de Keycloak y comprueba el rol; el servicio vuelve a
comprobarlo con el usuario autenticado que recibe.

| Método | Ruta | Qué hace |
|---|---|---|
| `GET` | `/api/reviews/assigned` | Mi bandeja de revisión (HU-05). Rol REVIEWER. |
| `GET` | `/api/reviews/config` | Límites de revisores por pregunta. Cualquier usuario autenticado. |
| `GET` | `/api/reviews/pending-assignment` | Preguntas que esperan revisores (HU-04). Rol ADMIN. |
| `GET` | `/api/reviews/{questionId}` | La revisión de una pregunta según quién consulta. ADMIN, AUTHOR o REVIEWER. |
| `POST` | `/api/reviews/{questionId}/assignments` | Asignar revisores a la ronda vigente, o agregar más a una en curso (HU-04). Rol ADMIN. |
| `POST` | `/api/reviews/{questionId}/evaluations` | Enviar mi evaluación de la pregunta (HU-05). Rol REVIEWER. |

## Mensajería (RabbitMQ)

**Publica** (con Outbox transaccional):

| Evento | Cuándo | Quién lo consume |
|---|---|---|
| `review.reviewers-assigned` | El administrador asigna revisores | question, notification, history |
| `review.evaluation-submitted` | Un revisor envía su evaluación | notification, history |
| `review.completed` | La ronda se cierra con aprobada o rechazada | question, notification, history |

**Consume:**

| Cola | Routing key | Efecto |
|---|---|---|
| `review.q.question-submitted` | `question.submitted` | Abre el caso y la ronda de revisión |
| `review.q.question-reopened` | `question.reopened` | Prepara la ronda siguiente |

## Base de datos

| Migración | Contenido |
|---|---|
| `V1__esquema_inicial.sql` | Casos de revisión, rondas, asignaciones, evaluaciones y eventos procesados |
| `V2__bandeja_de_salida.sql` | Tabla del Outbox |

## Estructura

```
src/main/java/co/unicauca/bancopreguntas/review/
├── api/              Controlador REST, DTO y manejo de errores
├── application/      Casos de uso y puertos (interfaces que el dominio necesita)
├── domain/           Reglas de negocio y eventos; no depende de Spring ni de la base de datos
└── infrastructure/   Adaptadores: JPA, RabbitMQ
```

**Patrones de diseño que usa:** Strategy (`PoliticaDeDecision`, `Unanimidad`), Adapter y Observer.

## Configuración

| Variable | Valor por defecto | Para qué sirve |
|---|---|---|
| `REVIEW_DB_URL` | `jdbc:postgresql://localhost:5435/review_db` | URL de la base de datos |
| `REVIEW_DB_USER` | `review` | Usuario de la base |
| `REVIEW_DB_PASSWORD` | `review` | Clave de la base |
| `RABBITMQ_HOST` | `localhost` | Servidor de RabbitMQ |
| `RABBITMQ_PORT` | `5672` | Puerto AMQP de RabbitMQ |
| `RABBITMQ_USER` | `guest` | Usuario de RabbitMQ |
| `RABBITMQ_PASSWORD` | `guest` | Clave de RabbitMQ |
| `REVIEW_SERVICE_PORT` | `8083` | Puerto HTTP |
| `REVIEW_MAX_REVIEWERS` | `3` | Máximo de revisores por pregunta |
| `EUREKA_ENABLED` | `true` | Registrarse en Eureka (se apaga en las pruebas) |
| `EUREKA_URL` | `http://localhost:8761/eureka` | Dirección de Eureka |

## Ejecutar y probar

```bash
# desde microservicios/
./mvnw -pl servicios/review-service -am test          # pruebas del servicio
docker compose up -d --build review-service    # el servicio con sus dependencias (base, RabbitMQ, Eureka, Keycloak)
```

La pila completa se levanta como se explica en el [README de microservicios](../../README.md). Para recorrer el
servicio a mano, importar su colección de Postman y el entorno `SaberPro_Local`.
