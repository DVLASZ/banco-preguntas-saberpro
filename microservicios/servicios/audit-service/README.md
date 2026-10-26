# audit-service · Auditoría

Guarda una entrada de auditoría por cada acción relevante: cambios de las preguntas, revisión e inicios de sesión.

| | |
|---|---|
| **Puerto** | 8091 (`AUDIT_SERVICE_PORT`) |
| **Base de datos** | PostgreSQL `audit_db` (puerto 5441 en local), con migraciones de Flyway |
| **Swagger** | `http://localhost:8091/swagger-ui.html` (directo; el gateway lo expone en la nueva distribución) |
| **Pruebas** | 38 pruebas (unitarias, de API, de persistencia y de arquitectura con ArchUnit) |

## Responsabilidad

Es el contexto de **Auditoría** (RNF de trazabilidad). Escucha los eventos de preguntas, revisión y autenticación y deja un registro que solo el administrador consulta, con filtros.

## Reglas de negocio

- Solo el administrador consulta la auditoría (403 para los demás).
- Filtros: tipo de entrada, usuario que actuó, pregunta y rango de fechas; resultado paginado, lo más reciente primero.
- Incluye los inicios de sesión correctos y fallidos.
- Un evento repetido no duplica entradas.

## API REST

Las rutas pasan por el gateway, que valida el token de Keycloak y comprueba el rol; el servicio vuelve a comprobarlo con el usuario autenticado que recibe.

| Método | Ruta | Qué hace |
|---|---|---|
| `GET` | `/api/audit` | Auditoría completa, incluidos los inicios de sesión, con filtros opcionales. Rol ADMIN. |

## Mensajería (RabbitMQ)

**Publica:** nada; es un consumidor puro.

**Consume:**

| Cola | Routing key | Efecto |
|---|---|---|
| `audit.q.question-submitted` | `question.submitted` | Envío a revisión |
| `audit.q.question-reopened` | `question.reopened` | Reapertura |
| `audit.q.question-published` | `question.published` | Publicación |
| `audit.q.question-state-changed` | `question.state-changed` | Cambio de estado |
| `audit.q.reviewers-assigned` | `review.reviewers-assigned` | Asignación de revisores |
| `audit.q.evaluation-submitted` | `review.evaluation-submitted` | Evaluación |
| `audit.q.review-completed` | `review.completed` | Resultado de la ronda |
| `audit.q.login-succeeded` | `auth.login-succeeded` | Inicio de sesión |
| `audit.q.login-failed` | `auth.login-failed` | Inicio de sesión fallido |

## Base de datos

| Migración | Contenido |
|---|---|
| `V1__esquema_inicial.sql` | audit_db: auditoría del sistema (hechos de las preguntas, de su revisión y de la seguridad) |

## Estructura

```
src/main/java/co/unicauca/bancopreguntas/audit/
├── api/              Controlador REST, DTO y manejo de errores
├── application/      Casos de uso y puertos (interfaces que el dominio necesita)
├── domain/           Reglas de negocio y eventos; no depende de Spring ni de la base de datos
└── infrastructure/   Adaptadores: JPA, RabbitMQ, clientes REST y configuración
```

**Patrones de diseño que usa:** Adapter y Observer. Detalle en [patrones](../../../docs/corte-2/05-patrones.md).

## Configuración

| Variable | Valor por defecto | Para qué sirve |
|---|---|---|
| `AUDIT_DB_URL` | `jdbc:postgresql://localhost:5441/audit_db` | URL de la base de datos |
| `AUDIT_DB_USER` | `audit` | Usuario de la base |
| `AUDIT_DB_PASSWORD` | `audit` | Clave de la base |
| `RABBITMQ_HOST` | `localhost` | Servidor de RabbitMQ |
| `RABBITMQ_PORT` | `5672` | Puerto AMQP de RabbitMQ |
| `RABBITMQ_USER` | `guest` | Usuario de RabbitMQ |
| `RABBITMQ_PASSWORD` | `guest` | Clave de RabbitMQ |
| `AUDIT_SERVICE_PORT` | `8091` | Puerto HTTP |
| `EUREKA_ENABLED` | `true` | Registrarse en Eureka (se apaga en las pruebas) |
| `EUREKA_URL` | `http://localhost:8761/eureka` | Dirección de Eureka |

## Ejecutar y probar

```bash
# desde microservicios/
./mvnw -pl servicios/audit-service -am test          # pruebas del servicio
docker compose up -d --build audit-service    # el servicio con sus dependencias
```

La pila completa se levanta como se explica en el [README de microservicios](../../README.md). Para recorrer el servicio a mano, importar su colección de Postman y el entorno `SaberPro_Local`.
