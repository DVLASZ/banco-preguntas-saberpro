# API: contratos y colecciones

| Carpeta | Contenido |
|---|---|
| [`openapi/`](openapi/) | Contrato OpenAPI de cada uno de los **10 servicios**, **exportado de los propios servicios** (prueba `ContratoOpenApiTest`) |
| [`eventos/`](eventos/) | Esquema JSON de los diez eventos que se publican en RabbitMQ |
| [`postman/`](postman/) | Colecciones de Postman por servicio (7, las de los servicios que el gateway expone) y el entorno `SaberPro_Local` |

Cada servicio publica además su propio Swagger en `/swagger-ui.html` cuando está en ejecución; los archivos de `openapi/`
son una copia de lo que sirve `GET /docs/<servicio>/v3/api-docs` a través del gateway (se regeneran si cambia la API, ver
[`openapi/README.md`](openapi/README.md)).

## Contratos OpenAPI

| Archivo | Servicio | Rutas |
|---|---|---|
| [`openapi-user-service.json`](openapi/openapi-user-service.json) | `user-service` | `/api/users/me`, `/api/users`, `/api/users/sync` |
| [`openapi-question-service.json`](openapi/openapi-question-service.json) | `question-service` | `/api/questions` y sus subrutas (`/{id}`, `/submit`, `/reopen`, `/publish`, `/all`, `/pending-review`) |
| [`openapi-validation-service.json`](openapi/openapi-validation-service.json) | `validation-service` (interno) | `/api/validation/questions` |
| [`openapi-lifecycle-service.json`](openapi/openapi-lifecycle-service.json) | `lifecycle-service` (interno) | `/api/lifecycle/questions`, `/{id}`, `/{id}/history`, `/submit`, `/reopen`, `/publish` |
| [`openapi-assignment-service.json`](openapi/openapi-assignment-service.json) | `assignment-service` | `/api/reviews/config`, `/pending-assignment`, `/assigned`, `/{id}/assignments` |
| [`openapi-evaluation-service.json`](openapi/openapi-evaluation-service.json) | `evaluation-service` | `/api/reviews/{id}`, `/{id}/evaluations` |
| [`openapi-notification-service.json`](openapi/openapi-notification-service.json) | `notification-service` | `/api/notifications`, `/unread-count`, `/{id}/read`, `/read-all` |
| [`openapi-email-service.json`](openapi/openapi-email-service.json) | `email-service` | `/api/emails` |
| [`openapi-history-service.json`](openapi/openapi-history-service.json) | `history-service` | `/api/history/questions/{id}`, `/me` |
| [`openapi-audit-service.json`](openapi/openapi-audit-service.json) | `audit-service` | `/api/audit` |

Los servicios **internos** (`validation-service` y `lifecycle-service`) solo los llaman otros servicios, pasando la
identidad de quien hizo la petición; el gateway no tiene rutas hacia ellos (solo publica su documentación).

## Colecciones de Postman

Las colecciones piden el token a Keycloak (flujo de contraseña del cliente de pruebas locales) en su carpeta «0 · Sesión» y
lo usan en cada petición; cada una trae verificaciones automáticas de éxito y de error (401, 403, 400, 404, 409). Las de los dos
servicios internos no existen: se ejercitan a través de `question-service`, que los llama al crear, enviar, reabrir y publicar.

| Colección | Servicio | Peticiones | Verificaciones |
|---|---|---|---|
| [`user-service`](postman/user-service.postman_collection.json) | `user-service` | 16 | 29 |
| [`question-service`](postman/question-service.postman_collection.json) | `question-service` (con validation y lifecycle por dentro) | 23 | 36 |
| [`assignment-evaluation-services`](postman/assignment-evaluation-services.postman_collection.json) | `assignment-service` y `evaluation-service` | 23 | 36 |
| [`notification-service`](postman/notification-service.postman_collection.json) | `notification-service` | 15 | 24 |
| [`email-service`](postman/email-service.postman_collection.json) | `email-service` | 9 | 14 |
| [`history-service`](postman/history-service.postman_collection.json) | `history-service` | 12 | 19 |
| [`audit-service`](postman/audit-service.postman_collection.json) | `audit-service` | 9 | 14 |

### Cómo usarlas

1. Levantar la pila: `docker compose up -d --build` en `microservicios/` (esperar a que todo esté *healthy*).
2. En Postman, importar la colección que se quiera probar y el entorno [`SaberPro_Local`](postman/SaberPro_Local.postman_environment.json)
   y elegir ese entorno. Las variables son `baseUrl` (`http://localhost:8080`), `keycloakUrl` (`http://localhost:8180`) y
   `password` (la clave de desarrollo de los usuarios del realm).
3. Ejecutar la colección completa con el *Collection Runner*, de arriba abajo: cada petición guarda en variables (`tokenAutor`,
   `qId`…) lo que necesita la siguiente. Las peticiones que dependen de un evento de RabbitMQ esperan 1,5 segundos antes de consultar.

Por consola: `npx newman run user-service.postman_collection.json -e SaberPro_Local.postman_environment.json`.

> **Estado de la verificación.** Las 7 colecciones se ejecutaron completas con newman contra la pila de Docker el 26 de octubre de
> 2026: 107 peticiones y 172 verificaciones, sin fallos. Los contratos OpenAPI se regeneraron de los servicios. Las colecciones que
> preparan una pregunta publicada esperan unos segundos entre pasos porque los eventos viajan por RabbitMQ.
