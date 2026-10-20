# simulation-service · Simulacros

> **Primera distribución (7 servicios).** Este README describe el servicio tal como está publicado hoy. En la nueva distribución, de 14
> servicios, se divide en `catalog-service` (preguntas publicadas y selección) y `simulation-service` (simulacros); ver el [README de los servicios](../README.md).

Permite al estudiante practicar con simulacros armados al azar con preguntas publicadas, con tiempo límite y calificación automática.

| | |
|---|---|
| **Puerto** | 8086 (`SIMULATION_SERVICE_PORT`) |
| **Base de datos** | PostgreSQL `simulation_db` (puerto 5438 en local), con migraciones de Flyway |
| **Swagger** | `http://localhost:8080/docs/simulation-service/v3/api-docs` (a través del gateway) |
| **Contrato exportado** | [`openapi-simulation-service.json`](../../../docs/corte-2/api/openapi/openapi-simulation-service.json) y [colección de Postman](../../../docs/corte-2/api/postman/simulation-service.postman_collection.json) |
| **Pruebas** | 88 pruebas (unitarias, de API, de persistencia y de arquitectura con ArchUnit) |

## Responsabilidad

Es el contexto de **Simulacros de Evaluación** (RF-21 a RF-24, construido por adelantado del tercer corte). Mantiene su propio catálogo con las preguntas publicadas, alimentado por el evento `question.published` (que lleva el contenido de la pregunta), de modo que no necesita consultar a `question-service`. Genera simulacros, controla el tiempo, califica y publica el resultado.

## Reglas de negocio

- El estudiante genera un simulacro con la cantidad de preguntas y la duración que pide, filtrando por competencia, tema y
  dificultad; las preguntas se escogen al azar (patrón Strategy: `SelectorDePreguntas` y `SeleccionAleatoria`). Si no hay suficientes,
  responde 409 con cuántas hay. El máximo es `SIMULATION_MAX_QUESTIONS`.
- Mientras el simulacro está en curso **no se revela la respuesta correcta**; al finalizar, o cuando se agota el tiempo, se califica y se
  muestran. Una tarea periódica cierra los simulacros vencidos (`SIMULATION_EXPIRATION_INTERVAL_MS`).
- Se puede cambiar una respuesta mientras el simulacro siga en curso; un simulacro no se resuelve dos veces.
- Un estudiante solo ve sus simulacros; el docente y el administrador ven cualquiera.
- Una pregunta que se archiva sale del catálogo (`question.state-changed`), sin afectar a los simulacros ya generados.

## API REST

Todas las rutas pasan por el gateway, que valida el token de Keycloak y comprueba el rol; el servicio vuelve a
comprobarlo con el usuario autenticado que recibe.

| Método | Ruta | Qué hace |
|---|---|---|
| `GET` | `/api/simulations` | Mis simulacros, el más reciente primero. Rol STUDENT. |
| `POST` | `/api/simulations` | Generar un simulacro aleatorio con preguntas publicadas y empezar a contar el tiempo. Rol STUDENT. |
| `GET` | `/api/simulations/{id}` | Consultar un simulacro y el tiempo que le queda. El estudiante ve los suyos; TEACHER y ADMIN, cualquiera. |
| `GET` | `/api/simulations/catalog/summary` | Cuántas preguntas publicadas hay para simulacros, por competencia y dificultad. Roles STUDENT, TEACHER o ADMIN. |
| `POST` | `/api/simulations/{id}/answers` | Responder una pregunta del simulacro (se puede cambiar antes de finalizar). Rol STUDENT. |
| `POST` | `/api/simulations/{id}/finish` | Finalizar el simulacro: se califica automáticamente y se muestran las respuestas correctas. Rol STUDENT. |

## Mensajería (RabbitMQ)

**Publica** (con Outbox transaccional):

| Evento | Cuándo | Quién lo consume |
|---|---|---|
| `simulation.completed` | Un simulacro se finaliza o vence | tracking |

**Consume:**

| Cola | Routing key | Efecto |
|---|---|---|
| `simulation.q.question-published` | `question.published` | Agrega la pregunta al catálogo |
| `simulation.q.question-state-changed` | `question.state-changed` | Retira del catálogo las preguntas archivadas |

## Base de datos

| Migración | Contenido |
|---|---|
| `V1__esquema_inicial.sql` | Catálogo de preguntas, simulacros y sus preguntas, eventos procesados y Outbox |

## Estructura

```
src/main/java/co/unicauca/bancopreguntas/simulation/
├── api/              Controlador REST, DTO y manejo de errores
├── application/      Casos de uso y puertos (interfaces que el dominio necesita)
├── domain/           Reglas de negocio y eventos; no depende de Spring ni de la base de datos
└── infrastructure/   Adaptadores: JPA, RabbitMQ
```

**Patrones de diseño que usa:** Strategy (`SelectorDePreguntas`, `SeleccionAleatoria`), Adapter y Observer. Detalle en [simulacros y seguimiento](../../../docs/corte-3/simulacros-y-seguimiento.md).

## Configuración

| Variable | Valor por defecto | Para qué sirve |
|---|---|---|
| `SIMULATION_DB_URL` | `jdbc:postgresql://localhost:5438/simulation_db` | URL de la base de datos |
| `SIMULATION_DB_USER` | `simulation` | Usuario de la base |
| `SIMULATION_DB_PASSWORD` | `simulation` | Clave de la base |
| `RABBITMQ_HOST` | `localhost` | Servidor de RabbitMQ |
| `RABBITMQ_PORT` | `5672` | Puerto AMQP de RabbitMQ |
| `RABBITMQ_USER` | `guest` | Usuario de RabbitMQ |
| `RABBITMQ_PASSWORD` | `guest` | Clave de RabbitMQ |
| `SIMULATION_SERVICE_PORT` | `8086` | Puerto HTTP |
| `EUREKA_ENABLED` | `true` | Registrarse en Eureka (se apaga en las pruebas) |
| `EUREKA_URL` | `http://localhost:8761/eureka` | Dirección de Eureka |
| `SIMULATION_MAX_QUESTIONS` | `50` | Máximo de preguntas por simulacro |
| `SIMULATION_EXPIRATION_INTERVAL_MS` | `30000` | Cada cuántos milisegundos se cierran los simulacros vencidos |

## Ejecutar y probar

```bash
# desde microservicios/
./mvnw -pl servicios/simulation-service -am test          # pruebas del servicio
docker compose up -d --build simulation-service    # el servicio con sus dependencias (base, RabbitMQ, Eureka, Keycloak)
```

La pila completa se levanta como se explica en el [README de microservicios](../../README.md). Para recorrer el
servicio a mano, importar su colección de Postman y el entorno `SaberPro_Local`.
