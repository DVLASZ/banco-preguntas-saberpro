# history-service · Historial y auditoría

> **Primera distribución (7 servicios).** Este README describe el servicio tal como está publicado hoy. En la nueva distribución, de 14
> servicios, se divide en `history-service` (línea de tiempo de cada pregunta) y `audit-service` (auditoría); ver el [README de los servicios](../README.md).

Guarda, en orden cronológico, todo lo que le pasó a cada pregunta (envíos, asignaciones, evaluaciones, resultados) y la auditoría del sistema, incluidos los inicios de sesión.

| | |
|---|---|
| **Puerto** | 8085 (`HISTORY_SERVICE_PORT`) |
| **Base de datos** | PostgreSQL `history_db` (puerto 5437 en local), con migraciones de Flyway |
| **Swagger** | `http://localhost:8080/docs/history-service/v3/api-docs` (a través del gateway) |
| **Contrato exportado** | [`openapi-history-service.json`](../../../docs/corte-2/api/openapi/openapi-history-service.json) y [colección de Postman](../../../docs/corte-2/api/postman/history-service.postman_collection.json) |
| **Pruebas** | 63 pruebas (unitarias, de API, de persistencia y de arquitectura con ArchUnit) |

## Responsabilidad

Es el contexto de **Administración y Trazabilidad** (historia nueva del autor que ve el historial de revisiones y auditoría del administrador). Consume todos los eventos del sistema y los convierte en una línea de tiempo por pregunta; no decide nada, solo registra y responde consultas.

## Reglas de negocio

- Cada evento se guarda una sola vez (idempotencia por `eventId`).
- El **administrador** consulta cualquier pregunta y la auditoría completa; el **autor**, el rastro de sus preguntas; el **revisor**,
  el de las preguntas que revisa.
- La auditoría admite filtros (tipo de evento, actor, pregunta, desde y hasta) y es solo para el administrador.
- No tiene Outbox: no publica eventos.

## API REST

Todas las rutas pasan por el gateway, que valida el token de Keycloak y comprueba el rol; el servicio vuelve a
comprobarlo con el usuario autenticado que recibe.

| Método | Ruta | Qué hace |
|---|---|---|
| `GET` | `/api/history/audit` | Auditoría completa, incluidos los inicios de sesión, con filtros opcionales. Rol ADMIN. |
| `GET` | `/api/history/me` | Todo lo que puedo ver de mis preguntas, lo más reciente primero. ADMIN, AUTHOR o REVIEWER. |
| `GET` | `/api/history/questions/{questionId}` | El rastro de una pregunta en orden cronológico. ADMIN, AUTHOR (sus preguntas) o REVIEWER (las que revisa). |

## Mensajería (RabbitMQ)

**Publica:** nada; es un consumidor puro.

**Consume** (una cola por tipo de evento): `question.submitted`, `question.reopened`, `question.published`, `question.state-changed`,
`review.reviewers-assigned`, `review.evaluation-submitted`, `review.completed`, `auth.login-succeeded` y `auth.login-failed`
(colas `history.q.*`).

## Base de datos

| Migración | Contenido |
|---|---|
| `V1__esquema_inicial.sql` | Entradas del historial, datos de la pregunta y de sus revisores, y eventos ya procesados |

## Estructura

```
src/main/java/co/unicauca/bancopreguntas/history/
├── api/              Controlador REST, DTO y manejo de errores
├── application/      Casos de uso y puertos (interfaces que el dominio necesita)
├── domain/           Reglas de negocio y eventos; no depende de Spring ni de la base de datos
└── infrastructure/   Adaptadores: JPA, RabbitMQ
```

**Patrones de diseño que usa:** Adapter y Observer.

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
docker compose up -d --build history-service    # el servicio con sus dependencias (base, RabbitMQ, Eureka, Keycloak)
```

La pila completa se levanta como se explica en el [README de microservicios](../../README.md). Para recorrer el
servicio a mano, importar su colección de Postman y el entorno `SaberPro_Local`.
