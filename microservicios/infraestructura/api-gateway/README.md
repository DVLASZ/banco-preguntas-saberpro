# api-gateway · Entrada única

Puerta de entrada de todas las peticiones de la aplicación web: **valida el token de Keycloak**, comprueba el rol que exige cada ruta
y reenvía la petición al servicio correcto. Está hecho con Spring Cloud Gateway y Resilience4J.

| | |
|---|---|
| **Puerto** | 8080 (`GATEWAY_PORT`) |
| **Descubrimiento** | Eureka: reparte entre las réplicas de cada servicio con `lb://<servicio>` |
| **Swagger agregado** | `http://localhost:8080/swagger-ui.html` (un solo Swagger para los siete servicios) |
| **Pruebas** | 25 |

## Qué hace

1. **Autenticación.** Valida la firma y el emisor del JWT con las claves públicas de Keycloak (`KEYCLOAK_JWK_SET_URI`,
   `KEYCLOAK_ISSUER_URI`). Sin token válido responde 401.
2. **Autorización por rol** (primera barrera, `RoleRules`). Cada servicio vuelve a comprobar el rol de cada operación, así que un
   fallo del gateway no abre nada. Gana la primera regla que coincide; por eso van primero las más específicas.
3. **Enrutamiento.** Cada servicio atiende sus caminos `/api/...` (preguntas, revisiones, notificaciones, historial, usuarios,
   simulacros y seguimiento) y publica su OpenAPI en `/docs/<servicio>/v3/api-docs`.
4. **Resiliencia.** Cada ruta tiene un circuit breaker (si el servicio no responde, contesta 503 de inmediato con el mensaje de
   `FallbackController` en lugar de dejar esperando), un bulkhead que limita las llamadas simultáneas y reintentos de las consultas
   GET cuando una réplica falla. El circuit breaker va por fuera y el reintento por dentro.
5. **Trazabilidad y seguridad de las cabeceras.** `RequestIdFilter` da a cada petición un identificador `X-Request-Id` (el que traiga o
   uno nuevo), lo envía al servicio y lo devuelve en la respuesta; además **descarta** las cabeceras `X-User-Id` y `X-User-Roles` que un
   cliente intente enviar, que solo valen entre servicios.
6. **CORS.** Solo los orígenes de `GATEWAY_ALLOWED_ORIGINS`.

## Quién puede qué

| Ruta | Roles |
|---|---|
| `GET /api/users/me` | Cualquier rol |
| `/api/users/**` | `ADMIN` |
| `GET /api/history/audit`, `GET /api/notifications/emails` | `ADMIN` |
| `/api/history/**`, `/api/notifications/**` | `ADMIN`, `AUTHOR`, `REVIEWER` |
| `POST /api/reviews/*/assignments`, `GET /api/reviews/pending-assignment` | `ADMIN` |
| `GET /api/reviews/assigned`, `POST /api/reviews/*/evaluations` | `REVIEWER` |
| `GET /api/reviews/**` | `ADMIN`, `AUTHOR`, `REVIEWER` |
| `POST /api/questions/*/publish`, `GET /api/questions/all`, `GET /api/questions/pending-review` | `ADMIN` |
| `GET /api/questions/*` | `ADMIN`, `AUTHOR`, `REVIEWER` |
| Resto de `/api/questions/**` | `AUTHOR` |
| `GET /api/simulations/**` | `ADMIN`, `TEACHER`, `STUDENT` |
| Resto de `/api/simulations/**` | `STUDENT` |
| `GET /api/tracking/me/**` | `STUDENT` |
| `GET /api/tracking/**` | `ADMIN`, `TEACHER` |

## Configuración

| Variable | Valor por defecto | Para qué sirve |
|---|---|---|
| `GATEWAY_PORT` | `8080` | Puerto HTTP |
| `GATEWAY_ALLOWED_ORIGINS` | `http://localhost:4200` | Orígenes del navegador permitidos |
| `GATEWAY_TIMEOUT` | `10s` | Espera máxima por un servicio antes de abrir el circuito |
| `KEYCLOAK_JWK_SET_URI` | `http://localhost:8180/realms/bancopreguntas/protocol/openid-connect/certs` | Claves públicas de Keycloak |
| `KEYCLOAK_ISSUER_URI` | `http://localhost:8180/realms/bancopreguntas` | Emisor esperado del token |
| `EUREKA_ENABLED` / `EUREKA_URL` | `true` / `http://localhost:8761/eureka` | Registro y descubrimiento |

## Ejecutar y probar

```bash
# desde microservicios/
./mvnw -pl infraestructura/api-gateway test         # pruebas
docker compose up -d --build api-gateway edge       # el gateway con el balanceador de entrada
```

En el sistema completo no se llama al gateway directamente: pasa por [`edge`](../edge/), que reparte entre sus réplicas
(`docker compose up -d --scale api-gateway=3`).
