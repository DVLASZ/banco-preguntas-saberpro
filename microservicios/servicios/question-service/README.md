# question-service · Preguntas

Dueño del contenido de las preguntas: el autor las redacta, las modifica mientras son borrador y las consulta con filtros; para validarlas, moverlas de estado y llevar su historial se apoya en `validation-service` y `lifecycle-service`.

| | |
|---|---|
| **Puerto** | 8082 (`QUESTION_SERVICE_PORT`) |
| **Base de datos** | PostgreSQL `question_db` (puerto 5434 en local), con migraciones de Flyway |
| **Swagger** | `http://localhost:8080/docs/question-service/v3/api-docs` (a través del gateway) |
| **Contrato exportado** | [`openapi-question-service.json`](../../../docs/corte-2/api/openapi/openapi-question-service.json) y [colección de Postman](../../../docs/corte-2/api/postman/question-service.postman_collection.json) |
| **Pruebas** | 141 pruebas (unitarias, de API, de persistencia y de arquitectura con ArchUnit) |

## Responsabilidad

Es el contexto de **Gestión de Preguntas** (HU-01, HU-02, HU-03 y la publicación de HU-07). Guarda la pregunta con su contexto, enunciado, cuatro opciones, justificación, bibliografía y clasificación (competencia, tema, subtema y dificultad) y su versión. **No decide el estado**: guarda una copia (con el número del último cambio aplicado) que mantiene al día con lo que le informa `lifecycle-service`, para poder listar y filtrar por estado y saber si la pregunta todavía se puede editar.

## Reglas de negocio

- Solo se modifica una pregunta en **Borrador** y solo su autor (RF-06); fuera de ese estado se responde 409.
- Al crear, modificar y enviar a revisión se pide la validación estructural a `validation-service`, que reúne todas las violaciones para que el autor corrija todo de una vez (400 con el detalle).
- Crear registra la pregunta en `lifecycle-service`; enviar, reabrir y publicar piden la transición a ese servicio, que es quien la autoriza, la anota en el historial y publica los eventos. Lo que ese servicio rechaza (403, 404, 409) vuelve igual al cliente.
- La copia del estado se actualiza con la respuesta de la transición y con `question.state-changed`; un aviso repetido o atrasado se ignora gracias al número de secuencia.
- Si `validation-service` o `lifecycle-service` no responden, la operación no se hace y se informa 503; la consulta de una pregunta sigue funcionando y solo omite el historial.
- Ninguna pregunta se borra (RNF-16).

## API REST

Las rutas pasan por el gateway, que valida el token de Keycloak y comprueba el rol; el servicio vuelve a comprobarlo con el usuario autenticado que recibe.

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

## Llamadas a otros servicios

Son llamadas REST síncronas con la identidad de quien hizo la petición (la propaga `bp-platform`):

- `validation-service` (`VALIDATION_SERVICE_URL`): revisar el contenido.
- `lifecycle-service` (`LIFECYCLE_SERVICE_URL`): registrar, enviar, reabrir, publicar y leer el historial.

## Mensajería (RabbitMQ)

**Publica:** nada; es un consumidor puro.

**Consume:**

| Cola | Routing key | Efecto |
|---|---|---|
| `question.q.state-changed` | `question.state-changed` | Actualiza la copia del estado de la pregunta |

## Base de datos

| Migración | Contenido |
|---|---|
| `V1__esquema_inicial.sql` | question_db: preguntas, historial de estados y eventos procesados |
| `V2__version_de_la_pregunta.sql` | Número de versión del contenido: sube cada vez que el autor reabre una pregunta rechazada |
| `V3__indices_de_busqueda.sql` | Índices para las listas paginadas (más recientes primero): sin ellos, cada consulta ordena todas las preguntas del autor, o de toda la tabla en la lista del administrador |
| `V4__bandeja_de_salida.sql` | Patrón Outbox: los eventos se guardan aquí en la misma transacción que el cambio de negocio y un relevo los publica en el broker; así no se pierden si RabbitMQ está caído |
| `V5__ciclo_de_vida_en_otro_servicio.sql` | El estado, el historial y los eventos de las preguntas pasaron a lifecycle-service: aquí solo queda una copia del estado |

## Estructura

```
src/main/java/co/unicauca/bancopreguntas/question/
├── api/              Controlador REST, DTO y manejo de errores
├── application/      Casos de uso y puertos (interfaces que el dominio necesita)
├── domain/           Reglas de negocio y eventos; no depende de Spring ni de la base de datos
└── infrastructure/   Adaptadores: JPA, RabbitMQ, clientes REST y configuración
```

**Patrones de diseño que usa:** Builder (`Question`), Adapter (puertos de validación, ciclo de vida y persistencia) y Facade (`QuestionApplicationService`). Detalle en [patrones](../../../docs/corte-2/05-patrones.md).

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
| `VALIDATION_SERVICE_URL` | `http://localhost:8095` | Dónde está validation-service |
| `LIFECYCLE_SERVICE_URL` | `http://localhost:8094` | Dónde está lifecycle-service |

## Ejecutar y probar

```bash
# desde microservicios/
./mvnw -pl servicios/question-service -am test          # pruebas del servicio
docker compose up -d --build question-service    # el servicio con sus dependencias
```

La pila completa se levanta como se explica en el [README de microservicios](../../README.md). Para recorrer el servicio a mano, importar su colección de Postman y el entorno `SaberPro_Local`.
