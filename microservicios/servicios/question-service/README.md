# question-service · Preguntas

> **Primera distribución (7 servicios).** Este README describe el servicio tal como está publicado hoy. En la nueva distribución, de 14
> servicios, se divide en `question-service` (contenido), `validation-service` (reglas de calidad) y `lifecycle-service` (estados y su historial); ver el [README de los servicios](../README.md).

Dueño de las preguntas y de su ciclo de vida: el autor las redacta, las modifica mientras son borrador, las envía a revisión y, si el revisor las rechaza, las reabre como una versión nueva. El administrador publica las aprobadas.

| | |
|---|---|
| **Puerto** | 8082 (`QUESTION_SERVICE_PORT`) |
| **Base de datos** | PostgreSQL `question_db` (puerto 5434 en local), con migraciones de Flyway |
| **Swagger** | `http://localhost:8080/docs/question-service/v3/api-docs` (a través del gateway) |
| **Contrato exportado** | [`openapi-question-service.json`](../../../docs/corte-2/api/openapi/openapi-question-service.json) y [colección de Postman](../../../docs/corte-2/api/postman/question-service.postman_collection.json) |
| **Pruebas** | 174 pruebas (unitarias, de API, de persistencia y de arquitectura con ArchUnit) |

## Responsabilidad

Es el contexto de **Gestión de Preguntas** (HU-01, HU-02, HU-03 y la publicación de HU-07). Guarda la pregunta con su contexto, enunciado, cuatro opciones, justificación, bibliografía y clasificación (competencia, tema, subtema y dificultad), lleva el historial de estados y la versión, y es la única que cambia el estado de una pregunta: lo hace al actuar el autor o el administrador, y al reaccionar a lo que informa `review-service`.

## Reglas de negocio

- Una pregunta pasa por siete estados: Borrador, Pendiente de revisión, En revisión, Aprobada, Rechazada, Publicada y Archivada.
  Las transiciones válidas son las de RF-15; cualquier otra se rechaza con 409.
- Solo se modifica una pregunta en **Borrador** y solo su autor.
- Al crear, modificar y enviar a revisión se valida la estructura con siete reglas (campos obligatorios, contexto, una única pregunta
  directa, exactamente cuatro opciones, una única respuesta correcta, sin expresiones como «todas las anteriores» y longitud y estructura
  de las opciones). El validador reúne todas las violaciones para que el autor corrija todo de una vez.
- Una pregunta rechazada se **reabre** como Borrador con una versión nueva; el historial de estados se conserva.
- Ninguna pregunta se borra: cualquier estado puede pasar a Archivada (RNF-16).
- Si llega un evento que la fase actual de la pregunta no admite (por ejemplo, un resultado repetido), se ignora.

## API REST

Todas las rutas pasan por el gateway, que valida el token de Keycloak y comprueba el rol; el servicio vuelve a
comprobarlo con el usuario autenticado que recibe.

| Método | Ruta | Qué hace |
|---|---|---|
| `GET` | `/api/questions` | Listar mis preguntas con filtros y paginación (HU-03). Rol AUTHOR. |
| `POST` | `/api/questions` | Crear una pregunta en estado Borrador (HU-01). Rol AUTHOR. |
| `GET` | `/api/questions/all` | Buscar preguntas de todos los autores (por ejemplo, las aprobadas que esperan publicación). Rol ADMIN. |
| `GET` | `/api/questions/pending-review` | Preguntas pendientes de revisión (HU-04). Rol ADMIN. |
| `GET` | `/api/questions/{id}` | Consultar una pregunta con su historial de estados. ADMIN y REVIEWER ven cualquiera; AUTHOR, solo las suyas. |
| `PUT` | `/api/questions/{id}` | Modificar una pregunta; solo su autor y solo en estado Borrador (RF-06). Rol AUTHOR. |
| `POST` | `/api/questions/{id}/publish` | Publicar una pregunta aprobada: de Aprobada a Publicada. Rol ADMIN. |
| `POST` | `/api/questions/{id}/reopen` | Reabrir una pregunta rechazada para corregirla: vuelve a Borrador como una versión nueva. Rol AUTHOR. |
| `POST` | `/api/questions/{id}/submit` | Enviar a revisión: de Borrador a Pendiente de revisión (HU-02). Rol AUTHOR. |

## Mensajería (RabbitMQ)

**Publica** (con Outbox transaccional, en el exchange `bancopreguntas.events`):

| Evento | Cuándo | Quién lo consume |
|---|---|---|
| `question.submitted` | El autor envía la pregunta a revisión | review, notification, history |
| `question.reopened` | El autor reabre una rechazada | review, history |
| `question.published` | El administrador publica una aprobada; **lleva el contenido de la pregunta** | simulation, notification, history |
| `question.state-changed` | Cualquier cambio de estado | simulation, history |

**Consume:**

| Cola | Routing key | Efecto |
|---|---|---|
| `question.q.reviewers-assigned` | `review.reviewers-assigned` | La pregunta pasa a En revisión |
| `question.q.review-completed` | `review.completed` | La pregunta pasa a Aprobada o Rechazada |

## Base de datos

| Migración | Contenido |
|---|---|
| `V1__esquema_inicial.sql` | Preguntas, historial de estados, eventos procesados |
| `V2__version_de_la_pregunta.sql` | Número de versión de cada pregunta |
| `V3__indices_de_busqueda.sql` | Índices para listar y filtrar |
| `V4__bandeja_de_salida.sql` | Tabla del Outbox con los eventos pendientes de publicar |

## Estructura

```
src/main/java/co/unicauca/bancopreguntas/question/
├── api/              Controlador REST, DTO y manejo de errores
├── application/      Casos de uso y puertos (interfaces que el dominio necesita)
├── domain/           Reglas de negocio y eventos; no depende de Spring ni de la base de datos
└── infrastructure/   Adaptadores: JPA, RabbitMQ
```

**Patrones de diseño que usa:** Builder (`Question.Builder`), State (`FaseDePregunta` y sus siete fases), Template Method
(`ReglaPorOpcion`), Adapter (puertos y adaptadores) y Observer (los eventos). Ver el [documento de patrones](../../../docs/corte-2/05-patrones.md).

## Configuración

| Variable | Valor por defecto | Para qué sirve |
|---|---|---|
| `QUESTION_DB_URL` | `jdbc:postgresql://localhost:5434/question_db` | URL de la base de datos |
| `QUESTION_DB_USER` | `question` | Usuario de la base |
| `QUESTION_DB_PASSWORD` | `question` | Clave de la base |
| `RABBITMQ_HOST` | `localhost` | Servidor de RabbitMQ |
| `RABBITMQ_PORT` | `5672` | Puerto AMQP de RabbitMQ |
| `RABBITMQ_USER` | `guest` | Usuario de RabbitMQ |
| `RABBITMQ_PASSWORD` | `guest` | Clave de RabbitMQ |
| `QUESTION_SERVICE_PORT` | `8082` | Puerto HTTP |
| `EUREKA_ENABLED` | `true` | Registrarse en Eureka (se apaga en las pruebas) |
| `EUREKA_URL` | `http://localhost:8761/eureka` | Dirección de Eureka |

## Ejecutar y probar

```bash
# desde microservicios/
./mvnw -pl servicios/question-service -am test          # pruebas del servicio
docker compose up -d --build question-service    # el servicio con sus dependencias (base, RabbitMQ, Eureka, Keycloak)
```

La pila completa se levanta como se explica en el [README de microservicios](../../README.md). Para recorrer el
servicio a mano, importar su colección de Postman y el entorno `SaberPro_Local`.
