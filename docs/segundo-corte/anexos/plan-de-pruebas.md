# Plan de pruebas del segundo corte

La rúbrica pide pruebas unitarias a todas las clases del dominio (entidades y servicios) y el docente
confirmó que se esperan pruebas unitarias y de integración sobre el dominio. Este plan fija qué se prueba
en cada servicio antes de escribir el código.

## Estrategia

| Nivel | Qué cubre | Herramientas |
|---|---|---|
| Unitarias | Entidades, objetos de valor, reglas y servicios de dominio, sin infraestructura | JUnit 5 y Mockito |
| Integración | Repositorios contra PostgreSQL real, publicación y consumo de eventos contra RabbitMQ, controladores | Spring Boot Test y Testcontainers (requiere Docker abierto) |
| Carga | Escalabilidad (no cuenta como unitaria) | k6 |

Meta: mantener el estándar del primer corte, en el que todas las clases de dominio tienen sus pruebas.

## question-service

| Clase de dominio | Casos principales |
|---|---|
| `EstadoPregunta` | Todas las transiciones válidas e inválidas; archivar desde cualquier estado; Rechazada vuelve a Borrador |
| `Question` | Construcción con Builder; solo se edita en Borrador; cambio de estado registra historial |
| Reglas de validación (8) | Una prueba por regla: contexto vacío, más de una pregunta directa, opciones distintas de cuatro, más de una respuesta correcta, "todas/ninguna de las anteriores", longitud y estructura de las opciones |
| `QuestionValidator` | Reporta todas las violaciones juntas, no solo la primera |
| `QuestionService` | Crear guarda en Borrador; listar aplica filtros y paginación; enviar a revisión publica el evento |
| Integración | Repositorio JPA con PostgreSQL; consumo de `review.reviewers-assigned` pasa a En revisión; consumo de `review.completed` pasa a Aprobada o Rechazada; evento duplicado se ignora |

## review-service

| Clase de dominio | Casos principales |
|---|---|
| `ReviewCase` | Se crea al recibir `question.submitted`; no se asignan revisores dos veces |
| Asignación | Rechaza 0 y 4 revisores; acepta 1, 2 y 3; rechaza repetidos; rechaza al autor como revisor |
| `Evaluation` y observación | Rechazar exige observación; un revisor no evalúa dos veces; solo evalúa quien está asignado |
| `DecisionPolicy` (unanimidad) | Todos aprueban da Aprobada; un rechazo da Rechazada; con evaluaciones pendientes y sin rechazo no decide |
| Casos de uso | Asignar publica `review.reviewers-assigned`; evaluar publica `review.evaluation-submitted`; decisión final publica `review.completed` |
| Integración | Repositorio JPA; consumo de `question.submitted`; consulta de observaciones de colegas permitida solo a participantes |

## user-service

| Clase | Casos principales |
|---|---|
| Usuario y roles | Un usuario con varios roles; usuario inactivo no inicia sesión |
| Autenticación | Contraseña correcta emite token con el rol; incorrecta da 401; el hash BCrypt no coincide con el texto plano |
| Consulta de revisores | Filtra por rol y excluye un identificador (el autor) |

## notification-service

| Clase | Casos principales |
|---|---|
| Destinatarios por evento | Cada evento genera las notificaciones de la tabla de "Quién recibe qué" |
| Plantillas | Reemplazo de datos; observaciones listadas en el rechazo |
| `EmailSender` (puerto) | Con un doble de prueba: falla de correo deja la notificación `FALLIDO` sin perder la bandeja |
| Idempotencia | Un `eventId` repetido no duplica notificaciones |
| Integración | Envío real contra Mailpit; marcar como leída |

## api-gateway

| Caso | Resultado esperado |
|---|---|
| Petición sin token | 401 |
| Token válido con rol incorrecto para la ruta | 403 |
| Ruta de administrador con rol ADMIN | Reenvía al servicio |
| Ruta pública de login | Reenvía sin token |

## Pruebas de aceptación manuales

Por cada historia, los criterios de aceptación de `01-historias-de-usuario.md` se recorren a mano con Swagger o
Postman y, al final, desde la interfaz web. Quedan registrados en el documento de arquitectura.
