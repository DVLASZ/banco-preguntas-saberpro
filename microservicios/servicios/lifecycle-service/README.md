# lifecycle-service · Ciclo de vida de las preguntas

Lleva el estado de cada pregunta: sus transiciones válidas, el historial de cambios y los eventos que avisan a los demás servicios.

| | |
|---|---|
| **Puerto** | 8094 (`LIFECYCLE_SERVICE_PORT`) |
| **Base de datos** | PostgreSQL `lifecycle_db` (puerto 5444 en local), con migraciones de Flyway |
| **Swagger** | `http://localhost:8080/docs/lifecycle-service/v3/api-docs` (a través del gateway; el servicio es interno y solo publica su documentación) |
| **Contrato exportado** | [`openapi-lifecycle-service.json`](../../../docs/corte-2/api/openapi/openapi-lifecycle-service.json) |
| **Pruebas** | 99 pruebas (unitarias, de API, de persistencia y de arquitectura con ArchUnit) |

## Responsabilidad

Es el contexto de **Ciclo de vida** (RF-14 y RF-15). Es el único que cambia el estado de una pregunta, ya sea por una operación del autor o del administrador (que le pide `question-service`) o por lo que informa el servicio de revisión. No guarda el contenido de la pregunta: en la publicación lo recibe para ponerlo en el evento `question.published`. El gateway no tiene rutas hacia él.

## Reglas de negocio

- Siete estados (Borrador, Pendiente de revisión, En revisión, Aprobada, Rechazada, Publicada, Archivada) y las transiciones de RF-15; cualquier otra se rechaza con 409. Cada estado es una clase (patrón State).
- Solo el autor envía o reabre su pregunta (403 si no lo es); solo el administrador publica.
- Reabrir una rechazada la deja en Borrador como una **versión nueva**.
- Cada cambio lleva una **secuencia** que solo crece (y la versión del contenido) en `question.state-changed`: quien guarda una copia del estado ignora los avisos repetidos o atrasados.
- Una asignación de revisores lleva la pregunta a En revisión y un resultado de revisión, a Aprobada o Rechazada; si la pregunta no está en el estado esperado, el evento se ignora. Los eventos repetidos no se aplican dos veces.
- Ninguna pregunta se borra (RNF-16): se archiva.

## API REST

Las rutas **no pasan por el gateway**: las llaman otros servicios con la identidad de quien hizo la petición, y el servicio vuelve a comprobar el token y el rol.

| Método | Ruta | Qué hace |
|---|---|---|
| `POST` | `/api/lifecycle/questions` | Registrar una pregunta recién creada en estado Borrador. Rol AUTHOR. |
| `GET` | `/api/lifecycle/questions/{id}` | Estado actual de una pregunta. ADMIN y REVIEWER ven cualquiera; AUTHOR, solo las suyas. |
| `GET` | `/api/lifecycle/questions/{id}/history` | Historial de estados de una pregunta, del más antiguo al más reciente. |
| `POST` | `/api/lifecycle/questions/{id}/publish` | Publicar una pregunta aprobada: de Aprobada a Publicada. Rol ADMIN. |
| `POST` | `/api/lifecycle/questions/{id}/reopen` | Reabrir una pregunta rechazada: vuelve a Borrador como una versión nueva. Rol AUTHOR. |
| `POST` | `/api/lifecycle/questions/{id}/submit` | Enviar a revisión: de Borrador a Pendiente de revisión (HU-02). Rol AUTHOR. |

## Mensajería (RabbitMQ)

**Publica** (con Outbox transaccional, en el exchange `bancopreguntas.events`):

| Evento | Cuándo | Quién lo consume |
|---|---|---|
| `question.submitted` | El autor envía la pregunta a revisión | assignment, evaluation, notification, history, audit |
| `question.state-changed` | Cualquier cambio de estado (con `sequence` y `version`) | question, catalog, history, audit |
| `question.reopened` | El autor reabre una rechazada | assignment, evaluation, history, audit |
| `question.published` | El administrador publica una aprobada; **lleva el contenido de la pregunta** | catalog, notification, history, audit |

**Consume:**

| Cola | Routing key | Efecto |
|---|---|---|
| `lifecycle.q.reviewers-assigned` | `review.reviewers-assigned` | La pregunta pasa a En revisión |
| `lifecycle.q.review-completed` | `review.completed` | La pregunta pasa a Aprobada o Rechazada |

## Base de datos

| Migración | Contenido |
|---|---|
| `V1__esquema_inicial.sql` | lifecycle_db: estado de cada pregunta, historial de cambios, eventos procesados y bandeja de salida |

## Estructura

```
src/main/java/co/unicauca/bancopreguntas/lifecycle/
├── api/              Controlador REST, DTO y manejo de errores
├── application/      Casos de uso y puertos (interfaces que el dominio necesita)
├── domain/           Reglas de negocio y eventos; no depende de Spring ni de la base de datos
└── infrastructure/   Adaptadores: JPA, RabbitMQ, clientes REST y configuración
```

**Patrones de diseño que usa:** State (`FaseDePregunta` y sus siete fases), Adapter y Observer (eventos con Outbox). Detalle en [patrones](../../../docs/corte-2/05-patrones.md).

## Configuración

| Variable | Valor por defecto | Para qué sirve |
|---|---|---|
| `LIFECYCLE_DB_URL` | `jdbc:postgresql://localhost:5444/lifecycle_db` | URL de la base de datos |
| `LIFECYCLE_DB_USER` | `lifecycle` | Usuario de la base |
| `LIFECYCLE_DB_PASSWORD` | `lifecycle` | Clave de la base |
| `RABBITMQ_HOST` | `localhost` | Servidor de RabbitMQ |
| `RABBITMQ_PORT` | `5672` | Puerto AMQP de RabbitMQ |
| `RABBITMQ_USER` | `guest` | Usuario de RabbitMQ |
| `RABBITMQ_PASSWORD` | `guest` | Clave de RabbitMQ |
| `LIFECYCLE_SERVICE_PORT` | `8094` | Puerto HTTP |
| `EUREKA_ENABLED` | `true` | Registrarse en Eureka (se apaga en las pruebas) |
| `EUREKA_URL` | `http://localhost:8761/eureka` | Dirección de Eureka |

## Ejecutar y probar

```bash
# desde microservicios/
./mvnw -pl servicios/lifecycle-service -am test          # pruebas del servicio
docker compose up -d --build lifecycle-service    # el servicio con sus dependencias
```

La pila completa se levanta como se explica en el [README de microservicios](../../README.md).
