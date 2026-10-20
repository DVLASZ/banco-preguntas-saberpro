# tracking-service · Seguimiento académico

> **Primera distribución (7 servicios).** Este README describe el servicio tal como está publicado hoy. En la nueva distribución, de 14
> servicios, se divide en `statistics-service` (historial y estadísticas) y `report-service` (reporte del grupo); ver el [README de los servicios](../README.md).

Convierte los resultados de los simulacros en historial, estadísticas, fortalezas y debilidades por competencia, y en el reporte del grupo para el docente.

| | |
|---|---|
| **Puerto** | 8087 (`TRACKING_SERVICE_PORT`) |
| **Base de datos** | PostgreSQL `tracking_db` (puerto 5439 en local), con migraciones de Flyway |
| **Swagger** | `http://localhost:8080/docs/tracking-service/v3/api-docs` (a través del gateway) |
| **Contrato exportado** | `openapi-tracking-service.json` y colección de Postman |
| **Pruebas** | 61 pruebas (unitarias, de API, de persistencia y de arquitectura con ArchUnit) |

## Responsabilidad

Es el contexto de **Seguimiento Académico** (RF-25 a RF-28, construido por adelantado del tercer corte). Consume `simulation.completed`, acumula los resultados de cada estudiante y responde las consultas del propio estudiante y del docente.

## Reglas de negocio

- El estudiante ve su historial (el más reciente primero), sus estadísticas con la tendencia y sus fortalezas y debilidades; un
  estudiante sin simulacros recibe ceros.
- Una competencia es **fortaleza** con aciertos desde `TRACKING_STRENGTH_THRESHOLD` (70 % por defecto) y **debilidad** por debajo de
  `TRACKING_WEAKNESS_THRESHOLD` (50 %); las que quedan entre los dos umbrales son intermedias, y con menos de `TRACKING_MIN_ANSWERED`
  respuestas (5) no hay datos suficientes para clasificarla.
- El docente y el administrador consultan a cualquier estudiante y descargan el **reporte del grupo** en JSON y en CSV.
- Un evento repetido no duplica el historial (idempotencia por `eventId`).

## API REST

Todas las rutas pasan por el gateway, que valida el token de Keycloak y comprueba el rol; el servicio vuelve a
comprobarlo con el usuario autenticado que recibe.

| Método | Ruta | Qué hace |
|---|---|---|
| `GET` | `/api/tracking/me/competencies` | Mis fortalezas y debilidades por competencia (RF-27). Rol STUDENT. |
| `GET` | `/api/tracking/me/history` | Mi historial de simulacros, el más reciente primero (RF-25). Rol STUDENT. |
| `GET` | `/api/tracking/me/stats` | Mis estadísticas individuales (RF-26). Rol STUDENT. |
| `GET` | `/api/tracking/reports/group` | Reporte del grupo: promedio general, desempeño por competencia y una fila por estudiante (RF-28). Roles TEACHER o ADMIN. |
| `GET` | `/api/tracking/reports/group.csv` | El detalle del reporte de grupo en CSV, para abrirlo en una hoja de cálculo (RF-28). Roles TEACHER o ADMIN. |
| `GET` | `/api/tracking/students/{estudianteId}/competencies` | Fortalezas y debilidades de un estudiante por competencia. Roles TEACHER o ADMIN. |
| `GET` | `/api/tracking/students/{estudianteId}/history` | Historial de simulacros de un estudiante. Roles TEACHER o ADMIN. |
| `GET` | `/api/tracking/students/{estudianteId}/stats` | Estadísticas individuales de un estudiante. Roles TEACHER o ADMIN. |

## Mensajería (RabbitMQ)

**Publica:** nada; es un consumidor puro.

**Consume:**

| Cola | Routing key | Efecto |
|---|---|---|
| `tracking.q.simulation-completed` | `simulation.completed` | Registra el resultado en el historial del estudiante |

## Base de datos

| Migración | Contenido |
|---|---|
| `V1__esquema_inicial.sql` | Resultados registrados de cada simulacro y eventos procesados |

## Estructura

```
src/main/java/co/unicauca/bancopreguntas/tracking/
├── api/              Controlador REST, DTO y manejo de errores
├── application/      Casos de uso y puertos (interfaces que el dominio necesita)
├── domain/           Reglas de negocio y eventos; no depende de Spring ni de la base de datos
└── infrastructure/   Adaptadores: JPA, RabbitMQ
```

**Patrones de diseño que usa:** Adapter y Observer. Detalle en [simulacros y seguimiento](../../../docs/corte-3/simulacros-y-seguimiento.md).

## Configuración

| Variable | Valor por defecto | Para qué sirve |
|---|---|---|
| `TRACKING_DB_URL` | `jdbc:postgresql://localhost:5439/tracking_db` | URL de la base de datos |
| `TRACKING_DB_USER` | `tracking` | Usuario de la base |
| `TRACKING_DB_PASSWORD` | `tracking` | Clave de la base |
| `RABBITMQ_HOST` | `localhost` | Servidor de RabbitMQ |
| `RABBITMQ_PORT` | `5672` | Puerto AMQP de RabbitMQ |
| `RABBITMQ_USER` | `guest` | Usuario de RabbitMQ |
| `RABBITMQ_PASSWORD` | `guest` | Clave de RabbitMQ |
| `TRACKING_SERVICE_PORT` | `8087` | Puerto HTTP |
| `EUREKA_ENABLED` | `true` | Registrarse en Eureka (se apaga en las pruebas) |
| `EUREKA_URL` | `http://localhost:8761/eureka` | Dirección de Eureka |
| `TRACKING_STRENGTH_THRESHOLD` | `70` | Promedio mínimo (%) para ser fortaleza |
| `TRACKING_WEAKNESS_THRESHOLD` | `50` | Promedio (%) por debajo del cual es debilidad |
| `TRACKING_MIN_ANSWERED` | `5` | Respuestas mínimas para clasificar una competencia |

## Ejecutar y probar

```bash
# desde microservicios/
./mvnw -pl servicios/tracking-service -am test          # pruebas del servicio
docker compose up -d --build tracking-service    # el servicio con sus dependencias (base, RabbitMQ, Eureka, Keycloak)
```

La pila completa se levanta como se explica en el [README de microservicios](../../README.md). Para recorrer el
servicio a mano, importar su colección de Postman y el entorno `SaberPro_Local`.
