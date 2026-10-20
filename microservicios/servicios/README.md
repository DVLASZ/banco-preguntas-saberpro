# Servicios de negocio

Cada carpeta es un microservicio independiente con su propia base de datos (PostgreSQL), su propia imagen de
Docker y su propio Swagger. Se comunican por eventos en RabbitMQ; en la nueva distribución hay además tres llamadas REST
síncronas (de preguntas a validación y a ciclo de vida, y de simulacros a catálogo) con la identidad del usuario propagada.

## Publicados hoy: la primera distribución (7 servicios)

| Servicio | Función | Estado |
|---|---|---|
| [`question-service`](question-service/) | Preguntas, validación estructural y ciclo de vida (HU-01 a HU-03) | Publicado |
| [`review-service`](review-service/) | Asignación de revisores, rondas de evaluación y decisión por unanimidad | Publicado |
| [`notification-service`](notification-service/) | Bandeja de notificaciones y correo simulado | Publicado |
| [`history-service`](history-service/) | Historial de revisiones por pregunta y auditoría | Publicado |
| [`user-service`](user-service/) | Usuarios y roles | Publicado |
| [`simulation-service`](simulation-service/) | Catálogo de preguntas publicadas y simulacros aleatorios con tiempo y calificación automática (RF-21 a RF-24) | Publicado |
| [`tracking-service`](tracking-service/) | Historial de simulacros, estadísticas, fortalezas y debilidades y reportes del grupo (RF-25 a RF-28) | Publicado |

## En implementación: la nueva distribución (14 servicios)

Cada servicio queda con una sola responsabilidad. Está implementada y verificada en local; el código se publica por partes.

| Servicio publicado hoy (primera distribución, 7) | En la nueva distribución (14) |
|---|---|
| `question-service` | `question-service` (contenido), `validation-service` (reglas de calidad) y `lifecycle-service` (estados y su historial) |
| `review-service` | `assignment-service` (asignación de revisores) y `evaluation-service` (evaluación y unanimidad) |
| `notification-service` | `notification-service` (bandeja) y `email-service` (correo) |
| `history-service` | `history-service` (línea de tiempo de cada pregunta) y `audit-service` (auditoría de acciones e inicios de sesión) |
| `user-service` | `user-service` |
| `simulation-service` | `catalog-service` (preguntas publicadas y selección) y `simulation-service` (simulacros) |
| `tracking-service` | `statistics-service` (historial y estadísticas) y `report-service` (reporte del grupo) |

| Servicio nuevo | Puerto | Base de datos | Función |
|---|---|---|---|
| `question-service` | 8082 | `question_db` | Contenido de las preguntas; pide la validación y el ciclo de vida por REST |
| `validation-service` | 8095 | (sin base de datos) | Reglas de calidad del contenido |
| `lifecycle-service` | 8094 | `lifecycle_db` | Estados (patrón State), transiciones, historial de estados y sus eventos |
| `assignment-service` | 8083 | `assignment_db` | Rondas de revisión, asignación de 1 a 3 revisores y bandeja del revisor |
| `evaluation-service` | 8096 | `evaluation_db` | Evaluaciones de los revisores y decisión por unanimidad |
| `notification-service` | 8084 | `notification_db` | Bandeja de notificaciones |
| `email-service` | 8090 | `email_db` | Correo simulado con reintentos y registro |
| `history-service` | 8085 | `history_db` | Línea de tiempo de cada pregunta |
| `audit-service` | 8091 | `audit_db` | Auditoría de acciones e inicios de sesión |
| `user-service` | 8081 | `user_db` | Usuarios y roles |
| `catalog-service` | 8092 | `catalog_db` | Preguntas publicadas, resumen y selección al azar |
| `simulation-service` | 8086 | `simulation_db` | Simulacros con tiempo y calificación automática |
| `statistics-service` | 8087 | `statistics_db` | Historial, estadísticas, tendencia, fortalezas y debilidades |
| `report-service` | 8093 | `report_db` | Reporte del grupo para docentes (JSON y CSV) |

El detalle de la nueva distribución está en [`docs/corte-2`](../../docs/corte-2/README.md) (contextos y eventos, arquitectura,
decisiones y contratos de la API).

Todos comparten la biblioteca de [`../plataforma/bp-platform`](../plataforma/bp-platform/) y siguen la estructura
descrita en [convenciones](../../docs/general/convenciones.md).
