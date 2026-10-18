# Microservicios del Banco de Preguntas Saber Pro

Segunda iteración: la solución distribuida con microservicios orientados a eventos. El monolito del primer
corte se conserva en la rama `corte-1`. El diseño está en [`docs/corte-2`](../docs/corte-2/README.md).

## Organización de la carpeta

```
microservicios/
├── pom.xml                       Pom padre: compila todos los módulos
├── docker-compose.yml            Toda la pila con un solo comando (docker-compose.debug.yml publica los puertos)
├── .env.example                  Variables de entorno de desarrollo
├── plataforma/
│   └── bp-platform/              Biblioteca común: seguridad JWT, sobre de eventos, Outbox, errores, OpenAPI
├── servicios/                    Servicios de negocio (cada uno con su base de datos)
│   ├── question-service/         Preguntas, validación estructural y ciclo de vida
│   ├── review-service/           Asignación de revisores, rondas, unanimidad y reasignación automática
│   ├── notification-service/     Bandeja de notificaciones y correo simulado
│   ├── history-service/          Historial por pregunta y auditoría de accesos
│   └── user-service/             Usuarios de Keycloak y eventos de acceso
├── infraestructura/              Piezas que sostienen a los servicios
│   ├── api-gateway/              Entrada única, autorización, circuit breaker
│   ├── discovery-server/         Eureka (descubrimiento de servicios)
│   ├── edge/                     nginx que reparte entre las réplicas del gateway
│   └── keycloak/                 Realm con los usuarios y roles de prueba
└── pruebas-de-carga/             Script k6 de escalabilidad
```

| Módulo | Pruebas |
|---|---|
| [bp-platform](plataforma/bp-platform/) | 65 |
| [question-service](servicios/question-service/) | 210 |
| [review-service](servicios/review-service/) | 145 |
| [notification-service](servicios/notification-service/) | 74 |
| [history-service](servicios/history-service/) | 62 |
| [user-service](servicios/user-service/) | 63 |
| [api-gateway](infraestructura/api-gateway/) | 22 |
| [../frontend](../frontend/) (Angular) | 162 |

Infraestructura externa: PostgreSQL 16 por servicio, RabbitMQ (panel http://localhost:15672), Keycloak
(http://localhost:8180) y `edge` (nginx) como único punto de entrada en el puerto 8080.

## Requisitos

- JDK 21 y Maven (el proyecto trae `mvnw`).
- Docker Desktop con la virtualización activa (para ejecutar la pila; las pruebas no lo necesitan).

## Levantar todo

```bash
cd microservicios
cp .env.example .env     # opcional: cambia los valores de desarrollo
docker compose up -d --build
```

| Qué | Dónde |
|---|---|
| Aplicación web | http://localhost:4200 |
| API (a través de `edge` y el gateway) | http://localhost:8080 |
| Keycloak (administración) | http://localhost:8180 |
| Eureka | http://localhost:8761 |
| RabbitMQ | http://localhost:15672 |

Usuarios de prueba (solo entorno local), definidos en [`infraestructura/keycloak/bancopreguntas-realm.json`](infraestructura/keycloak/bancopreguntas-realm.json):
`admin`, `autor1`, `autor2` y `revisor1` a `revisor5`. La clave de desarrollo está en ese archivo.

Para depurar un servicio desde el IDE se publican sus puertos con
`docker compose -f docker-compose.yml -f docker-compose.debug.yml up -d` (8081 a 8085).

### Réplicas

```bash
docker compose up -d --scale question-service=3 --scale api-gateway=2
```

Las réplicas se registran solas en Eureka y `edge` reparte entre las del gateway.

## Variables

Ver [`.env.example`](.env.example). Las principales: `SECURITY_MODE` (`jwt` por defecto; `headers` solo para
pruebas), `KEYCLOAK_PUBLIC_URL`, `GATEWAY_ALLOWED_ORIGINS`, `REVIEW_MAX_REVIEWERS`.

## Pruebas

Desde `microservicios/` (si se ejecuta dentro de un módulo, `-pl` no encuentra los demás):

```bash
./mvnw test                            # todos los módulos
./mvnw test -pl servicios/question-service -am   # un servicio y la biblioteca común
```

Sin Docker: PostgreSQL embebido, RabbitMQ simulado y Keycloak simulado con tokens firmados en la prueba. Cubren
dominio, aplicación, API (`MockMvc`), persistencia (Flyway sobre PostgreSQL real), mensajería (incluido el
Outbox), seguridad JWT, concurrencia (dos revisores a la vez, `FOR UPDATE SKIP LOCKED`) y reglas de arquitectura
(ArchUnit: el dominio no depende de Spring).

## Probar el API a mano

Se necesita un token de Keycloak (flujo de contraseña del cliente `bancopreguntas-web`, habilitado solo en local):

```bash
TOKEN=$(curl -s -X POST http://localhost:8180/realms/bancopreguntas/protocol/openid-connect/token \
  -d grant_type=password -d client_id=bancopreguntas-web -d username=autor1 --data-urlencode "password=<clave>" \
  | grep -o '"access_token":"[^"]*"' | sed 's/.*:"//; s/"$//')
curl -s http://localhost:8080/api/questions -H "Authorization: Bearer $TOKEN"
```

Swagger agregado de todos los servicios a través del gateway: `http://localhost:8080/docs/<servicio>/v3/api-docs`.

## Endpoints principales

| Servicio | Ruta | Rol |
|---|---|---|
| question | `POST/GET /api/questions`, `GET/PUT /api/questions/{id}`, `POST …/{id}/submit`, `…/reopen` | Autor |
| question | `GET /api/questions/all`, `GET …/pending-review`, `POST …/{id}/publish` | Administrador |
| review | `GET /api/reviews/pending-assignment`, `POST /api/reviews/{id}/assignments` | Administrador |
| review | `GET /api/reviews/assigned`, `POST /api/reviews/{id}/evaluations` | Revisor |
| review | `GET /api/reviews/config`, `GET /api/reviews/{id}` | Todos |
| notification | `GET /api/notifications`, `…/unread-count`, `POST …/{id}/read`, `…/read-all` | Todos |
| notification | `GET /api/notifications/emails` | Administrador |
| history | `GET /api/history/questions/{id}`, `/me` | Todos |
| history | `GET /api/history/audit` | Administrador |
| user | `GET /api/users/me` (todos), `GET /api/users`, `POST /api/users/sync` (administrador) | — |

## Organización de cada servicio

```
domain/           Entidades, estados, reglas y eventos de dominio (sin Spring)
application/      Casos de uso y puertos de salida
infrastructure/   Adaptadores: JPA/PostgreSQL (Flyway), RabbitMQ, configuración
api/              Controlador REST, DTOs y manejo de errores
```

## Pendiente (corte 3)

Arquitectura hexagonal/DDD formal en un servicio, HU-06 y pitch.
