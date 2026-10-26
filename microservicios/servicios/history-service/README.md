# history-service · Historial de preguntas

Arma la línea de tiempo de cada pregunta con lo que ocurre a lo largo de la revisión.

| | |
|---|---|
| **Puerto** | 8085 (`HISTORY_SERVICE_PORT`) |
| **Base de datos** | PostgreSQL `history_db` (puerto 5437 en local), con migraciones de Flyway |
| **Swagger** | `http://localhost:8080/docs/history-service/v3/api-docs` (a través del gateway) |
| **Contrato exportado** | [`openapi-history-service.json`](../../../docs/corte-2/api/openapi/openapi-history-service.json) y [colección de Postman](../../../docs/corte-2/api/postman/history-service.postman_collection.json) |
| **Pruebas** | 54 pruebas (unitarias, de API, de persistencia y de arquitectura con ArchUnit) |

## Responsabilidad

Es el contexto de **Historial** (RF-29). Escucha los eventos de preguntas y revisión y guarda una entrada por cada uno, para que el autor, los revisores y el administrador vean el recorrido de una pregunta. La auditoría de accesos y acciones es de `audit-service`.

## Reglas de negocio

- Cada evento con pregunta deja una entrada con quién, qué y cuándo.
- El autor ve el historial de sus preguntas; el revisor, el de las que le asignaron; el administrador, el de todas.
- «Mi historial» muestra todo lo que el usuario puede ver, lo más reciente primero.
- Un evento repetido no duplica entradas.

## API REST

Las rutas pasan por el gateway, que valida el token de Keycloak y comprueba el rol; el servicio vuelve a comprobarlo con el usuario autenticado que recibe.

| Método | Ruta | Qué hace |
|---|---|---|
| `GET` | `/api/history/me` | Todo lo que puedo ver de mis preguntas, lo más reciente primero. ADMIN, AUTHOR o REVIEWER. |
| `GET` | `/api/history/questions/{questionId}` | El rastro de una pregunta en orden cronológico. ADMIN, AUTHOR (sus preguntas) o REVIEWER (las que revisa). |

## Mensajería (RabbitMQ)

**Publica:** nada; es un consumidor puro.

**Consume:**

| Cola | Routing key | Efecto |
|---|---|---|
| `history.q.question-submitted` | `question.submitted` | Registra el envío a revisión |
| `history.q.question-reopened` | `question.reopened` | Registra la reapertura |
| `history.q.question-published` | `question.published` | Registra la publicación |
| `history.q.question-state-changed` | `question.state-changed` | Registra el cambio de estado |
| `history.q.reviewers-assigned` | `review.reviewers-assigned` | Registra la asignación de revisores |
| `history.q.evaluation-submitted` | `review.evaluation-submitted` | Registra cada evaluación |
| `history.q.review-completed` | `review.completed` | Registra el resultado de la ronda |

## Base de datos

| Migración | Contenido |
|---|---|
| `V1__esquema_inicial.sql` | history_db: historial de las preguntas y auditoría del sistema |
| `V2__sin_auditoria.sql` | La auditoría (y con ella los inicios de sesión) pasó al audit-service: aquí quedan solo los hechos de las preguntas. Lo que se quita se copia antes a `history_entry_auditoria` |

## Estructura

```
src/main/java/co/unicauca/bancopreguntas/history/
├── api/              Controlador REST, DTO y manejo de errores
├── application/      Casos de uso y puertos (interfaces que el dominio necesita)
├── domain/           Reglas de negocio y eventos; no depende de Spring ni de la base de datos
└── infrastructure/   Adaptadores: JPA, RabbitMQ, clientes REST y configuración
```

**Patrones de diseño que usa:** Adapter y Observer. Detalle en [patrones](../../../docs/corte-2/05-patrones.md).

## Configuración

| Variable | Valor por defecto | Para qué sirve |
|---|---|---|
| `HISTORY_DB_URL` | `jdbc:postgresql://localhost:5437/history_db` | URL de la base de datos |
| `HISTORY_DB_USER` | `history` | Usuario de la base |
| `HISTORY_DB_PASSWORD` | `history` | Clave de la base |
| `RABBITMQ_HOST` | `localhost` | Servidor de RabbitMQ |
| `RABBITMQ_PORT` | `5672` | Puerto AMQP de RabbitMQ |
| `RABBITMQ_USER` | `guest` | Usuario de RabbitMQ |
| `RABBITMQ_PASSWORD` | `guest` | Clave de RabbitMQ |
| `HISTORY_SERVICE_PORT` | `8085` | Puerto HTTP |
| `EUREKA_ENABLED` | `true` | Registrarse en Eureka (se apaga en las pruebas) |
| `EUREKA_URL` | `http://localhost:8761/eureka` | Dirección de Eureka |

## Ejecutar y probar

```bash
# desde microservicios/
./mvnw -pl servicios/history-service -am test          # pruebas del servicio
docker compose up -d --build history-service    # el servicio con sus dependencias
```

La pila completa se levanta como se explica en el [README de microservicios](../../README.md). Para recorrer el servicio a mano, importar su colección de Postman y el entorno `SaberPro_Local`.
