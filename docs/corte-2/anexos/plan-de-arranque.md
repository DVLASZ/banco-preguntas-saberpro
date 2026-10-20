# Estructura, puertos y reglas de negocio (como quedó construido)

Este anexo nació como plan de arranque antes de escribir código y se actualizó con lo que se construyó. Para el detalle
de la seguridad y la resiliencia ver [08](../08-implementacion-seguridad-y-resiliencia.md); para el diseño de la
arquitectura, [03](../03-arquitectura-c4-uml.md).

## 1. Qué se reutilizó del primer corte

Del código del monolito (`monolito/`) se migró el dominio, que ya estaba separado de la interfaz Swing:

| Pieza del monolito | Dónde quedó | Observación |
|---|---|---|
| `EstadoPregunta` (7 estados, `puedePasarA`) | `lifecycle-service` | Se conservó la tabla de transiciones; cada estado es una clase (patrón State) |
| `Question`, `QuestionDistractors`, `Competencia`, `Dificultad` | `question-service` | Dominio puro, adaptado (versión de la pregunta, Builder, copia del estado) |
| `ContenidoPregunta`, `Competencia`, `Dificultad` | `validation-service` | La entrada de las reglas de validación |
| Reglas de validación y `QuestionValidator` | `validation-service` | Siete reglas independientes; el validador las ejecuta todas y reúne las violaciones |
| `FiltroPreguntas`, `Pagina` | `question-service` | Listado con filtros y paginación (HU-03) |
| Repositorio con adaptador JPA | `question-service` | Ahora sobre PostgreSQL con Flyway |
| Módulo de revisión y su estrategia de decisión | `assignment-service` y `evaluation-service` | Se amplió a 1–3 revisores, rondas y la estrategia `PoliticaDeDecision` (`Unanimidad`); la asignación y la evaluación son servicios separados |
| Observer del primer corte | RabbitMQ | El Observer en memoria se reemplazó por eventos reales entre servicios |
| Presentación Swing | No se migró | El frontend nuevo es web (Angular) |
| Módulo de usuarios (SQLite) | Keycloak y `user-service` | Los usuarios y la autenticación pasaron a Keycloak |

El análisis completo está en [reutilización del monolito](../../general/reutilizacion-del-monolito.md).

## 2. Estructura del repositorio

```
├── monolito/                 Corte 1 (también en la rama corte-1)
├── frontend/                 Angular
├── microservicios/
│   ├── plataforma/bp-platform/     Biblioteca común: seguridad JWT, sobre de eventos, Outbox, errores, OpenAPI
│   ├── servicios/                  los 14 servicios: user, question, validation, lifecycle, assignment, evaluation,
│   │                               notification, email, history, audit, catalog, simulation, statistics y report
│   ├── infraestructura/            api-gateway, discovery-server, edge (nginx) y keycloak
│   ├── pruebas-de-carga/           k6
│   ├── pruebas-funcionales/        Recorrido de las historias contra Docker
│   ├── docker-compose.yml          Toda la pila con un comando
│   └── .env.example                Variables de entorno de desarrollo (el .env no se versiona)
└── docs/                     general, corte-1, corte-2 y corte-3
```

Cada servicio tiene su `pom.xml`, `Dockerfile` y los paquetes `api / application / domain / infrastructure`, sin
anotaciones de Spring dentro de `domain` (lo vigila `ArquitecturaTest`).

## 3. Puertos y contenedores

| Contenedor | Puerto | Base de datos | Puerto de la base en el equipo |
|---|---|---|---|
| frontend | 4200 | — | — |
| edge → api-gateway | 8080 | — | — |
| keycloak | 8180 | — | — |
| discovery-server | 8761 | — | — |
| user-service | 8081 (solo depuración) | `user_db` | 5433 |
| question-service | 8082 (solo depuración) | `question_db` | 5434 |
| assignment-service | 8083 (solo depuración) | `assignment_db` | 5445 |
| notification-service | 8084 (solo depuración) | `notification_db` | 5436 |
| history-service | 8085 (solo depuración) | `history_db` | 5437 |
| simulation-service | 8086 (solo depuración) | `simulation_db` | 5438 |
| statistics-service | 8087 (solo depuración) | `statistics_db` | 5439 |
| email-service | 8090 (solo depuración) | `email_db` | 5440 |
| audit-service | 8091 (solo depuración) | `audit_db` | 5441 |
| catalog-service | 8092 (solo depuración) | `catalog_db` | 5443 |
| report-service | 8093 (solo depuración) | `report_db` | 5442 |
| lifecycle-service | 8094 (solo depuración) | `lifecycle_db` | 5444 |
| validation-service | 8095 (solo depuración) | — (sin base de datos) | — |
| evaluation-service | 8096 (solo depuración) | `evaluation_db` | 5446 |
| RabbitMQ | 5672 / panel 15672 | — | — |

Las variables (usuarios y claves de las bases, RabbitMQ, Keycloak, `SECURITY_MODE`, `REVIEW_MAX_REVIEWERS`,
`NOTIFICATION_EMAIL_DOMAIN`) están en `.env.example`.

## 4. Roles y rutas

La autorización está en el gateway (tabla `RoleRules`) y se repite en cada controlador.

| Rol | Puede |
|---|---|
| ADMIN | Ver todas las preguntas y las pendientes de asignación, asignar de 1 a 3 revisores, publicar, listar y sincronizar usuarios, ver la auditoría y los correos enviados |
| AUTHOR | Crear y editar (solo en Borrador) sus preguntas, listarlas, enviarlas a revisión, reabrir las rechazadas, ver las observaciones y el historial de las suyas |
| REVIEWER | Listar sus preguntas asignadas, evaluar (decisión y observación) y ver las observaciones de los demás revisores de la misma pregunta |
| Todos los de arriba | Ver y marcar como leídas sus notificaciones |
| STUDENT | Generar, presentar y finalizar sus simulacros; ver su historial, estadísticas y fortalezas |
| TEACHER | Consultar simulacros y el seguimiento de cualquier estudiante; reporte del grupo |

Los usuarios y roles de demostración están en [datos semilla](datos-semilla.md).

## 5. Eventos (RabbitMQ)

Exchange `bancopreguntas.events` (*topic*, durable). Cada consumidor tiene su cola (`review.q.question-submitted`,
`question.q.reviewers-assigned`, `notification.q.…`, `history.q.…`) con mensajes persistentes, reintentos y cola de
mensajes muertos. El catálogo completo y el cuerpo de cada evento están en [02](../02-contextos-y-eventos.md) y en el
[esquema JSON](../api/eventos/eventos.schema.json).

## 6. API REST (detrás del gateway, prefijo `/api`)

La lista de endpoints por servicio y rol está en el [README de microservicios](../../../microservicios/README.md#endpoints-principales);
los contratos OpenAPI generados por los servicios están en [`api/openapi`](../api/openapi/) y cada servicio expone su Swagger
en `/swagger-ui.html`.

## 7. Reglas de negocio

- De 1 a 3 revisores por pregunta (el máximo es configurable con `REVIEW_MAX_REVIEWERS`, por defecto 3); el autor nunca es
  su revisor y un revisor no se repite en la misma ronda.
- La pregunta pasa a «En revisión» en cuanto se asignan los revisores (evento `review.reviewers-assigned`).
- Aprobación por **unanimidad**. Con un solo rechazo la ronda se cierra y la pregunta queda Rechazada; las observaciones que
  lleguen después se guardan como tardías.
- Los revisores ven las observaciones de sus colegas; no hay puntaje rígido. Rechazar exige una observación.
- Una pregunta solo se edita en Borrador; la rechazada se reabre (vuelve a Borrador con una versión nueva) y al reenviarla se
  abre una ronda nueva con los mismos revisores.
- Sin borrado físico (RNF-16): todo cambio de estado queda en el historial.

## 8. Pruebas

Ver [plan de pruebas](plan-de-pruebas.md).

## 9. Cómo se construyó (orden de las fases)

| Fase | Resultado verificable |
|---|---|
| 0 | Ramas creadas; `docker compose up` levanta las bases y el broker sanos |
| 1 | `question-service` con HU-01 a HU-03 probados por Swagger y Postman |
| 2 | `user-service`, `api-gateway` y roles |
| 3 | `assignment-service` (entonces parte de `review-service`): asignación de 1 a 3 revisores y eventos; la pregunta pasa sola a «En revisión» |
| 4 | HU-05 completa y `notification-service` con correo simulado |
| 5 | Prueba de carga con k6: una réplica contra varias, anotando el límite de la máquina |
| 6 | Keycloak, frontend Angular, `history-service`, Outbox y resiliencia |
| 7 | Documentación, planificación del Sprint 2 y sustentación |
| 8 | Por adelantado del corte 3: simulacros y seguimiento (RF-21 a RF-28) |
| 9 | Redistribución en 14 servicios pequeños: validación, ciclo de vida, asignación, evaluación, correo, auditoría, catálogo, estadísticas y reportes salen de los servicios que los contenían |
