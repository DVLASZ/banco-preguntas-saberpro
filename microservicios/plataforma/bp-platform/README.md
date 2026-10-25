# bp-platform · Código común de los servicios

Biblioteca que todos los microservicios de negocio declaran como dependencia. Reúne lo que no es dominio y que, si cada servicio lo
escribiera por su cuenta, se repetiría siete veces: el sobre de los eventos, la publicación fiable, el manejo de errores y la
identidad del usuario. **No contiene reglas de negocio** y no depende de ningún servicio.

Se activa sola (autoconfiguración de Spring Boot): basta agregar la dependencia `bp-platform` al `pom.xml` del servicio.

## Qué ofrece

| Paquete | Contenido |
|---|---|
| `events` | `EventPublisher` (interfaz), `OutboxEventPublisher` y `OutboxRelay` (publicación fiable con Outbox), `AmqpEventPublisher` (publicación directa), `EventTopology` (el exchange común `bancopreguntas.events`), `IncomingEvent` e `IncomingEventParser` (lectura del sobre de un evento recibido) |
| `security` | `AuthenticatedUser` (el usuario de la petición), `Roles` (`ADMIN`, `AUTHOR`, `REVIEWER`, `TEACHER`, `STUDENT`), `HeaderUserArgumentResolver` y `JwtUserArgumentResolver` (de dónde sale el usuario), `KeycloakJwtConverter`, `UserHeadersOpenApiCustomizer` |
| `api` | `ApiError` (el formato único de error) y los manejadores de excepciones: `PlatformExceptionHandler` (errores de entrada y de seguridad) y `PlatformFallbackExceptionHandler` (el último recurso) |
| `config` | `PlatformEventsAutoConfiguration` y `PlatformWebAutoConfiguration`, que ensamblan todo lo anterior |

## Eventos

Todos los eventos viajan en el mismo sobre `{eventId, type, occurredAt, data}` por el exchange *topic* `bancopreguntas.events`; la
routing key es el tipo del evento.

- **Publicar con Outbox** (por defecto, si el servicio tiene base de datos): el evento se guarda en la tabla `outbox_event` **en la
  misma transacción** que el cambio de negocio; `OutboxRelay` lo publica después, cada 0,5 s (`bancopreguntas.events.outbox.interval`),
  aunque RabbitMQ esté caído en ese momento. Con varias réplicas cada lote se toma con `FOR UPDATE SKIP LOCKED`, así que dos réplicas
  nunca publican el mismo evento. La entrega es *al menos una vez*: los consumidores son idempotentes.
- **Publicar directo** (`bancopreguntas.events.outbox.enabled=false`): se envía después del commit; lo usan los servicios que solo
  consumen (`history`, `tracking`).
- **Leer:** `IncomingEvent` entrega el cuerpo como JSON genérico, de modo que un consumidor no depende de las clases del productor. Un
  mensaje malformado se descarta sin reencolarlo, porque reintentarlo no lo arregla.

## Identidad y seguridad

`bancopreguntas.security.mode` define cómo se conoce al usuario:

| Modo | Cómo llega la identidad | Uso |
|---|---|---|
| `headers` (por defecto) | Cabeceras `X-User-Id` y `X-User-Roles` | Pruebas y desarrollo sin Keycloak |
| `jwt` | Token JWT de Keycloak, validado por el propio servicio; el usuario sale de `preferred_username` y los roles de `realm_access.roles` | Sistema completo (autorización redundante con el gateway) |

En el modo `jwt` las cabeceras se **ignoran**: nadie puede hacerse pasar por otro enviándolas. Los controladores reciben un
`AuthenticatedUser` como parámetro y no saben de dónde sale.

Cuando un servicio llama a otro por REST mientras atiende una petición, `IdentityPropagationInterceptor` copia el token y las
cabeceras de identidad a la llamada, así el servicio llamado vuelve a validar y a exigir el rol. Solo lo hace hacia servicios internos
(nombres sin puntos, como los de la red de Docker y Eureka): a un servidor externo nunca se envía el token.

## Errores

Todos los servicios responden los errores con el mismo formato `ApiError` (`código`, `mensaje` y `detalles`). Los errores de entrada
(datos inválidos, JSON mal formado, valores de enumeración inexistentes) y de seguridad (401 y 403) los traduce
`PlatformExceptionHandler`, con la máxima precedencia; lo que ningún manejador resuelve lo atrapa `PlatformFallbackExceptionHandler`,
que conserva el código HTTP que Spring ya había decidido (404, 405, 415...) y no revela detalles de los errores internos.

## Pruebas

70 pruebas: publicación directa y con Outbox, lectura de eventos, resolución del usuario por cabeceras y por JWT, formato de errores
y autoconfiguración. Se ejecutan con `./mvnw -pl plataforma/bp-platform test` desde `microservicios/`.
