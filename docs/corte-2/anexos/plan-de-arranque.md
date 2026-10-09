# Corte 2 — Plan de arranque (listo para ejecutar tras confirmar con Libardo)

Complementa a `Corte2-Diseno-Microservicios.md`. Todo está basado en las respuestas del Docente 1. **No se escribe código ni se toca el repo hasta confirmar con Libardo.**

## 0. Puerta de salida: lo que se confirmó con Libardo

> Libardo ya respondió; las respuestas y decisiones están en [consultas a los docentes](../../general/consultas-a-docentes.md). La tabla se conserva como registro de lo que se preguntó.

| # | Punto | Impacto si cambia |
|---|---|---|
| 1 | Quién publica y archiva; transiciones sin retorno | Tabla de estados de `question-service` |
| 2 | Primer rechazo cierra la revisión, o se espera a todos | Lógica de `review-service` |
| 3 | Unanimidad y 1 a 3 revisores (contrastar con Docente 1) | `review-service` |
| 4 | ~~Correo real obligatorio, o basta simulado~~ **Resuelto:** simulado (Libardo) | `notification-service` |
| 5 | Angular o React; frontend en el Compose | Fase del frontend |
| 6 | Login mínimo con token firmado | `user-service` y gateway |
| 7 | Escalabilidad: qué evidencia | Fase de la prueba de carga |
| 8 | Bases: una instancia por servicio | `docker-compose` |

Lo demás (RabbitMQ, Spring Boot, usuarios precargados, autorización en el gateway) coincide con lo que ya se asumió.

## 1. Qué se reutiliza del Corte 1

Código de `modulo-preguntas` (leído, sin modificarlo):

| Pieza existente | Va a | Observación |
|---|---|---|
| `EstadoPregunta` (7 estados, `puedePasarA`) | `question-service` | Hoy: Borrador→Pendiente→En revisión→Aprobada/Rechazada; Rechazada→Borrador; Aprobada→Publicada; todo→Archivada. Es compatible con "estados sin retorno" |
| `Question`, `ContenidoPregunta`, `QuestionDistractors`, `Competencia`, `Dificultad` | `question-service` | Dominio puro: copiar y adaptar |
| `domain/validation/*` (8 reglas, `QuestionValidator`) | `question-service` | Patrón de reglas encadenadas; se conserva tal cual |
| `QuestionService`, `FiltroPreguntas`, `Pagina` | `question-service` | Lista con filtros y paginación (HU-03) |
| `QuestionRepository` + adaptador JPA del Taller 6 | `question-service` | Puerto y adaptador listos |
| `FuenteDePreguntasParaRevisar`, `TodasLasPreguntasEnRevision` (Strategy) | `review-service` | Base para la política de decisión |
| `Subject`/`Observer` | referencia | Se reemplaza por eventos reales de RabbitMQ |
| Presentación Swing (`presentation/*`) | **no se migra** | El frontend nuevo es web |
| `modulo-usuarios` (SQLite) | `user-service` | Usuarios y roles adaptados a PostgreSQL |
| `modulo-api-rest` | semilla de `question-service` | Controlador, mapper, `ApiError`, `GlobalExceptionHandler` |

## 2. Estructura del repositorio (cuando se autorice)

Ramas: `corte-1` (copia congelada del estado actual de `main`) y `corte-2` (trabajo nuevo). Merge a `main` solo al terminar y con tu confirmación.

```
microservicios/
  api-gateway/            (Spring Cloud Gateway)
  user-service/
  question-service/
  review-service/
  notification-service/
  audit-service/          (opcional)
  frontend/               (Angular o React)
  load-tests/             (k6)
  docker-compose.yml
  .env.example            (sin credenciales reales)
  postman/                (una colección por servicio)
docs/                     (segundo corte)
```

Cada servicio: `pom.xml` propio (Spring Boot 3, Java 21), `Dockerfile`, `mvnw`, paquetes `api / application / domain / infrastructure`, y **sin anotaciones de Spring dentro de `domain`**.

## 3. Puertos y contenedores

| Servicio | Puerto | Base de datos (contenedor propio) | Puerto host de la base |
|---|---|---|---|
| api-gateway | 8080 | — | — |
| user-service | 8081 | `user_db` | 5433 |
| question-service | 8082 | `question_db` | 5434 |
| review-service | 8083 | `review_db` | 5435 |
| notification-service | 8084 | `notification_db` | 5436 |
| audit-service | 8085 | `audit_db` | 5437 |
| RabbitMQ | 5672 / panel 15672 | — | — |
| Mailpit (correo de pruebas) | 1025 / panel 8025 | — | — |
| frontend | 4200 | — | — |

Variables en `.env` (ignorado por git): usuario y clave de cada base, credenciales de RabbitMQ, y `SMTP_HOST/PORT/USER/PASS/FROM` (en desarrollo apuntan a Mailpit).

## 4. Roles y rutas (autorización en el gateway)

| Rol | Puede |
|---|---|
| ADMIN | Ver preguntas pendientes de revisión, asignar de 1 a 3 revisores, listar revisores elegibles, ver todo el listado |
| AUTHOR | Crear y editar (solo en Borrador) sus preguntas, listarlas, enviarlas a revisión, ver las observaciones de sus preguntas |
| REVIEWER | Listar sus asignadas, evaluar (observaciones + aprobar/rechazar), ver las observaciones de los demás revisores de la misma pregunta |
| Todos | Ver y marcar leídas sus notificaciones |

Usuarios precargados (semilla con contraseñas BCrypt): 1 administrador, 2 autores, 3 revisores. El login está en `user-service` y el gateway valida el token.

## 5. Eventos (RabbitMQ)

Exchange `bancopreguntas.events` (topic, durable). Cola por consumidor: `review.q.question-submitted`, `question.q.reviewers-assigned`, `question.q.review-completed`, `notification.q.all`, `audit.q.all`. Mensajes persistentes con confirmación y consumidores idempotentes.

Sobre de todos los eventos:

```json
{
  "eventId": "uuid",
  "type": "review.completed",
  "occurredAt": "2026-10-06T10:15:30Z",
  "data": { }
}
```

| Evento | `data` |
|---|---|
| `question.submitted` | `questionId`, `authorId`, `title`, `competency` |
| `review.reviewers-assigned` | `questionId`, `authorId`, `reviewerIds[]`, `assignedBy` |
| `review.evaluation-submitted` | `questionId`, `reviewerId`, `decision`, `observation` |
| `review.completed` | `questionId`, `authorId`, `result` (`APPROVED` / `REJECTED`), `observations[]` |
| `question.state-changed` | `questionId`, `from`, `to`, `changedBy` |

## 6. API REST (detrás del gateway, prefijo `/api`)

| Servicio | Endpoints |
|---|---|
| user-service | `POST /auth/login`, `GET /users/me`, `GET /users?role=REVIEWER` |
| question-service | `POST /questions`, `GET /questions?author&state&competency&topic&page&size`, `GET /questions/{id}`, `PUT /questions/{id}` (solo Borrador), `POST /questions/{id}/submit`, `GET /questions/pending-review` (admin) |
| review-service | `POST /reviews/{questionId}/assignments`, `GET /reviews/assigned`, `GET /reviews/{questionId}`, `POST /reviews/{questionId}/evaluations` |
| notification-service | `GET /notifications`, `PATCH /notifications/{id}/read` |

Cada servicio expone su Swagger (`/swagger-ui.html`) y su colección de Postman, construidos a medida que se desarrolla (lo pidió el Docente 1).

## 7. Reglas de negocio

- 1 a 3 revisores por pregunta; el autor nunca es su revisor; un revisor no se repite.
- Pasa a "En revisión" en cuanto se asignan los revisores (evento `review.reviewers-assigned`).
- Aprobación por **unanimidad**. Con un rechazo, la pregunta es devuelta. **Pendiente (Libardo):** si cierra al primer rechazo o espera a todos.
- Los revisores ven las observaciones de sus colegas; sin puntaje rígido.
- Rechazar exige observación (propia, a validar).
- Edición solo en Borrador; tras el rechazo vuelve a Borrador (regla existente de Corte 1).
- Sin borrado físico (RNF-16).

## 8. Pruebas

- Unitarias de todas las clases de dominio (entidades y servicios), como en el Corte 1.
- Integración por servicio: repositorios contra PostgreSQL real con Testcontainers (requiere Docker abierto), y consumo y publicación de eventos contra RabbitMQ.
- Prueba de carga con k6 aparte (no cuenta como unitaria).
- Meta: mantener los 330 tests del Corte 1 como referencia y no bajar la calidad del dominio migrado.

## 9. Orden de construcción y definición de terminado

| Fase | Resultado verificable |
|---|---|
| 0 | Ramas creadas; `docker compose up` levanta 4 bases, RabbitMQ y Mailpit sanos |
| 1 | `question-service` con HU-01 a 03 por Swagger y Postman, pruebas verdes |
| 2 | `user-service` (login y revisores) y `api-gateway` con roles |
| 3 | `review-service`: asignación 1 a 3 y eventos `question.submitted`/`reviewers-assigned`; la pregunta cambia a "En revisión" sola |
| 4 | HU-05 completa + `notification-service` con correo en Mailpit |
| 5 | Prueba de carga: 1 réplica contra varias, anotando el límite de la máquina |
| 6 | Frontend contra el gateway (HU 1 a 5) |
| 7 | Documento, Sprint 2 y video |

Las fases 1 a 5 se prueban sin interfaz. Cada fase termina con commit propio.

## 10. Lo que tienes que preparar tú

1. Abrir Docker Desktop (el motor estaba apagado).
2. Decidir Angular o React (recomendado: Angular).
3. Crear la cuenta de correo para el SMTP real y una **contraseña de aplicación**. No me pases la clave por chat: va en tu `.env` local.
4. Confirmar con Libardo los 8 puntos de la sección 0.
5. Crear "Sprint 2" en Jira y arrastrar las historias SCRUM-40 a SCRUM-50.

## 11. Riesgos

- Testcontainers y varias bases a la vez consumen RAM; 23 GB alcanza.
- Outlook puede exigir OAuth2 para SMTP; el plan B es Gmail con contraseña de aplicación.
- Si Libardo cambia la regla de revisión, solo se toca `review-service`.
