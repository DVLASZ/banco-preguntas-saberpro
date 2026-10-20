# Plan de pruebas del segundo corte

La rúbrica pide pruebas unitarias a todas las clases del dominio (entidades y servicios) y el docente confirmó que se
esperan pruebas unitarias y de integración sobre el dominio. Este documento recoge **qué se prueba y con qué**, tal como
quedó construido. Todas las pruebas automatizadas se ejecutan con `./mvnw test` (desde `microservicios/`) y `npm test`
(desde `frontend/`), sin necesidad de Docker.

## Estrategia

| Nivel | Qué cubre | Herramientas |
|---|---|---|
| Unitarias | Entidades, objetos de valor, reglas y servicios de dominio, sin infraestructura | JUnit 5 y AssertJ |
| Aplicación | Casos de uso con dobles en memoria de los puertos y reloj manual | JUnit 5 |
| API | Controladores, serialización, validación y códigos de error | Spring Boot Test y MockMvc |
| Persistencia | Repositorios y migraciones de Flyway sobre PostgreSQL real | PostgreSQL embebido (`zonky`) |
| Mensajería | Publicación y escucha de eventos, Outbox, mensajes malformados | Spring AMQP con doble de `RabbitTemplate`; Outbox sobre PostgreSQL real |
| Seguridad | JWT firmado, roles, anti-suplantación | Tokens firmados en la prueba con una clave propia |
| Arquitectura | El dominio no depende de Spring, JPA, Jackson ni de las capas externas | ArchUnit |
| Extremo a extremo por servicio | Flujo completo del servicio con su base y su API | Spring Boot Test |
| Frontend | Componentes, servicios, validaciones, etiquetas y rutas | Vitest |
| Funcional con Docker | Las historias de usuario contra la pila completa | `recorrido_funcional.py` |
| Contrato | Cada servicio por su API pública, con casos de éxito y de error | [Colecciones de Postman](../api/README.md) ejecutadas con newman |
| Carga | Escalabilidad (no cuenta como unitaria) | k6, ver [04](../04-escalabilidad.md) |

## Resultados (octubre de 2026, versión de 14 servicios)

| Módulo | Pruebas | Qué cubren |
|---|---|---|
| `bp-platform` | 69 | JWT y roles, anti-suplantación de cabeceras, propagación de la identidad entre servicios, sobre de eventos, publicador AMQP, lector de eventos entrantes, Outbox sobre PostgreSQL real, manejo de errores |
| `question-service` | 189 | `Question` y su Builder, copia del estado y su secuencia, filtros y paginación, casos de uso con dobles de validación y de ciclo de vida, clientes REST (`ValidadorRemoto`, `CicloDeVidaRemoto`), API, persistencia, escucha de `question.state-changed`, flujo completo |
| `validation-service` | 28 | Las siete reglas de validación y `QuestionValidator`, `ReglaPorOpcion`, API (siempre 200 con las violaciones), roles y flujo sin base de datos |
| `lifecycle-service` | 78 | Estados y fases (State), `CicloDeVida`, secuencia y versión, casos de uso (enviar, reabrir, publicar y reacción a la revisión, idempotencia), API, persistencia, publicador de eventos y flujo completo |
| `assignment-service` | 77 | `CasoDeAsignacion` (rondas, asignar, reasignación automática, reapertura), casos de uso, bandeja del revisor, API, persistencia, escucha de eventos, JWT y flujo completo |
| `evaluation-service` | 76 | `CasoDeEvaluacion` (evaluar, rondas, vistas por observador), `Unanimidad`, casos de uso, API, persistencia, concurrencia (varios revisores a la vez), mensajería, JWT |
| `notification-service` | 91 | `Notificacion`, Factory Method de avisos (`CreadoresDeAvisoTest`), casos de uso (quién recibe qué, idempotencia), publicación de `notification.created`, API, persistencia, escucha de eventos, flujo completo |
| `email-service` | 38 | Envío con reintentos y registro (Decorator), casos de uso, API (solo administrador), persistencia, escucha de `notification.created` y flujo completo |
| `history-service` | 54 | Entradas de historial, visibilidad por rol, casos de uso, API, persistencia, escucha de eventos |
| `audit-service` | 38 | Entradas de auditoría de los nueve eventos, filtros, casos de uso, API (solo administrador), persistencia, escucha de eventos y flujo completo |
| `catalog-service` | 47 | Catálogo por evento (versiones y archivado), criterios y selección aleatoria, resumen, casos de uso, API (con el 409 de preguntas insuficientes), persistencia y flujo completo |
| `simulation-service` | 79 | Criterios, calificación, control del tiempo y estados del simulacro, casos de uso con catálogo y reloj manual, cliente `CatalogoRemoto`, API, persistencia y flujo completo |
| `statistics-service` | 55 | Estadísticas y tendencia, fortalezas y debilidades, casos de uso, publicación de `statistics.student-updated`, API, persistencia, escucha de eventos y flujo completo |
| `report-service` | 31 | Reporte de grupo y CSV, resúmenes (el más reciente gana), casos de uso, API, persistencia, escucha de eventos y flujo completo |
| `user-service` | 62 | Perfiles, sincronización con Keycloak (cliente simulado), eventos de acceso, API, persistencia |
| `api-gateway` | 25 | Rutas de los 14 servicios, 401 y 403 por rol (incluidos estudiante y docente), anti-suplantación, circuit breaker, fallback, CORS y rutas internas sin exponer |
| `discovery-server` | 3 | Salud y registro de servicios de Eureka |
| **Backend** | **1040** | |
| `frontend` (Vitest) | 183 | Formularios y validación, listados, detalle, evaluación, selector de revisores, notificaciones, simulacros, presentación del simulacro, seguimiento, reporte del grupo, guardias de rutas, etiquetas y estados |

Cobertura del dominio: todas las clases del dominio de los 14 servicios tienen pruebas propias o directas
(incluidas las utilidades internas de validación, `TextoTest`). Los tipos de aviso se prueban uno por uno
(`CreadoresDeAvisoTest`).

## Casos principales por servicio

### question-service

| Elemento | Casos principales |
|---|---|
| `Question` | Construcción con Builder; solo se edita en Borrador; la copia del estado ignora una secuencia ya aplicada; la reapertura trae la versión nueva |
| Casos de uso | Crear valida, registra en el ciclo de vida y guarda en Borrador; enviar, reabrir y publicar piden la transición y guardan el estado que informa lifecycle-service; solo el autor opera sus preguntas; si validation o lifecycle caen, no se guarda nada |
| Clientes REST | `ValidadorRemoto` y `CicloDeVidaRemoto` con un servidor simulado: 403, 404 y 409 vuelven como las excepciones del dominio; un 5xx o una caída son «dependencia no disponible» |
| Mensajería | `question.state-changed` actualiza la copia; un aviso atrasado o repetido se ignora |

### validation-service

| Elemento | Casos principales |
|---|---|
| Reglas de validación (7) | Una por requisito: campos obligatorios, contexto, una sola pregunta directa, cuatro opciones distintas, una respuesta correcta, expresiones prohibidas («todas/ninguna de las anteriores»), longitud y estructura |
| `ReglaPorOpcion` (Template Method) | El esqueleto recorre A a D, omite las vacías y se puede cambiar el gancho `omitir` |
| `QuestionValidator` y API | Reporta todas las violaciones juntas, no solo la primera; siempre responde 200; exige rol AUTHOR o ADMIN |

### lifecycle-service

| Elemento | Casos principales |
|---|---|
| Fases (State) | Cada operación solo desde su estado; coincidencia con la tabla de RF-15; archivar desde cualquier estado |
| `CicloDeVida` | Cada transición devuelve una copia con la secuencia siguiente; reabrir sube la versión; los invariantes |
| Casos de uso | Registrar (idempotente), enviar con `question.submitted`, reabrir con `question.reopened`, publicar con el contenido; solo el autor mueve su pregunta; la reacción a la revisión es idempotente y se ignora si el estado no corresponde |
| Flujo completo | Ciclo con rechazo, reapertura y aprobación: historial de nueve cambios y doce eventos en la bandeja de salida, en orden |

### assignment-service y evaluation-service

| Elemento | Casos principales |
|---|---|
| Asignación | Rechaza 0 y más del máximo (3); acepta 1, 2 y 3; rechaza repetidos y al autor como revisor; no asigna a una ronda decidida |
| Rondas | La corrección y reenvío abre una ronda nueva con los mismos revisores (asignación automática) |
| Bandeja | Separa pendientes y evaluadas con lo que informa evaluation-service; tras un rechazo sigue pendiente para quien no evaluó |
| Evaluación | Rechazar exige observación; un revisor no evalúa dos veces; solo evalúa quien está asignado; las observaciones tardías se marcan |
| `Unanimidad` | Todos aprueban da Aprobada; un rechazo da Rechazada; con evaluaciones pendientes y sin rechazo no decide |
| Visibilidad | El autor ve las observaciones solo cuando corresponde; el administrador y los revisores asignados ven el detalle |
| Orden de los eventos | Si la asignación llega antes que el aviso del envío, la ronda se abre con los datos del propio evento |
| Concurrencia | Varios revisores evaluando a la vez deciden la ronda una sola vez |

### notification-service y email-service

| Elemento | Casos principales |
|---|---|
| Destinatarios por evento | Cada evento genera los avisos de la tabla «Quién recibe qué» ([notificaciones](notificaciones-y-correos.md)) |
| Factory Method | Cada `TipoNotificacion` tiene su creador concreto; los textos cambian según manual o automática, aprobada o rechazada, con o sin observaciones |
| `notification.created` | Se publica por Outbox junto con el aviso |
| Correo | Con un doble: reintento hasta que pasa, última excepción si no, conteo del registro y apilado de decoradores; si el correo falla, el aviso sigue en la bandeja |
| Idempotencia | Un `eventId` repetido no duplica avisos ni correos |
| API | Bandeja paginada, no leídas, marcar una o todas; registro de correos solo para el administrador |

### user-service, history-service y audit-service

| Servicio | Casos principales |
|---|---|
| `user-service` | Sincroniza perfiles y roles desde Keycloak, desactiva los que desaparecen, publica los eventos de acceso una sola vez (cursor), responde `/me` |
| `history-service` | Guarda una entrada por evento de la pregunta, ignora eventos repetidos, el autor ve solo sus preguntas, el revisor las asignadas y el administrador todo |
| `audit-service` | Guarda los nueve eventos (incluidos los inicios de sesión), filtra por tipo, usuario, pregunta y fechas, y solo el administrador consulta |

### catalog-service, simulation-service, statistics-service y report-service

| Elemento | Casos principales |
|---|---|
| Catálogo | Versiones y archivado; filtros por competencia, tema (sin tildes, también por subtema) y dificultad; selección sin repetir; 409 si no hay suficientes |
| `Simulacro` | Contestar y cambiar respuestas, el límite exacto del tiempo, finalizar a tiempo y expirado, no contestar un simulacro terminado |
| `Calificador` | Aciertos, errores y sin responder; puntaje con dos decimales; agrupación por competencia |
| Casos de uso (simulacros) | Generar con el catálogo, contestar con el tiempo agotado, cierre periódico de vencidos, acceso solo a lo propio, 503 si el catálogo cae |
| `EstadisticasIndividuales` y `AnalisisDeCompetencias` | Resumen, tendencia (ventana de 3), fortalezas, debilidades e intermedias; límites de los umbrales |
| `ReporteDeGrupo` | Promedios, orden, empates y CSV con escape; un resumen más viejo no reemplaza al más nuevo |
| Flujo de extremo a extremo | Del evento al simulacro calificado, a las estadísticas y al reporte por la API |

### api-gateway

| Caso | Resultado esperado |
|---|---|
| Petición sin token | 401 |
| Token válido con rol incorrecto para la ruta | 403 |
| Ruta de administrador con rol ADMIN | Reenvía al servicio |
| Cabeceras `X-User-Id` y `X-User-Roles` en la petición | Se eliminan (anti-suplantación) |
| Servicio caído | 503 con mensaje en español (circuit breaker) |

## Pruebas de aceptación

Los criterios de aceptación de [01 · Historias de usuario](../01-historias-de-usuario.md) se recorren con el recorrido
funcional automático y las colecciones de Postman (11 colecciones, 314 verificaciones). Ambos se ejecutaron contra la pila de Docker
de 14 servicios el 19 de octubre de 2026 (73 y 314 comprobaciones, sin fallos), y k6 se corrió como prueba de humo con 30 usuarios
(umbrales cumplidos, p95 de 17 ms); la prueba de carga completa con 600 usuarios queda por repetir. La prueba de usabilidad con usuarios reales está pendiente de aplicar
([guion y plantillas](prueba-de-usabilidad.md)).
