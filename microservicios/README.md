# Microservicios del Banco de Preguntas Saber Pro

Segunda iteración: la solución distribuida con microservicios orientados a eventos. El monolito del primer
corte se conserva en la rama `corte-1`. El diseño está en [`docs/corte-2`](../docs/corte-2/README.md).

## Servicios

La solución son **diez microservicios** que se comunican por eventos en RabbitMQ, detrás de un API Gateway, cada uno con su propia base de
datos PostgreSQL (menos `validation-service`, que no guarda datos). Dos servicios del primer diseño, `simulation-service` y `tracking-service`
(simulacros y seguimiento académico), siguen en el repositorio pero son del tercer corte y no forman parte de esta entrega.

| Servicio | Qué hace |
|---|---|
| `user-service` | Sincroniza los usuarios y roles de Keycloak y publica los eventos de acceso |
| `question-service` | Contenido de las preguntas: crear, modificar, consultar y filtrar (HU-01 y HU-03) |
| `validation-service` | Reglas de calidad de una pregunta, sin estado |
| `lifecycle-service` | Estados de la pregunta, transiciones, historial de estados y sus eventos (HU-02) |
| `assignment-service` | Rondas de revisión, asignación de 1 a 3 revisores y bandeja del revisor (HU-04) |
| `evaluation-service` | Evaluaciones de los revisores y decisión por unanimidad (HU-05) |
| `notification-service` | Bandeja de avisos de cada usuario |
| `email-service` | Correo simulado de cada aviso, con reintentos y registro |
| `history-service` | Línea de tiempo de cada pregunta |
| `audit-service` | Auditoría de acciones e inicios de sesión |

## Organización de la carpeta

```
microservicios/
├── pom.xml                       Pom padre: compila todos los módulos
├── docker-compose.yml            Toda la pila con un solo comando (docker-compose.debug.yml publica los puertos)
├── .env.example                  Variables de entorno de desarrollo
├── plataforma/
│   └── bp-platform/              Biblioteca común: seguridad JWT, sobre de eventos, Outbox, errores, OpenAPI
├── servicios/                    Servicios de negocio (cada uno con su base de datos)
│   ├── question-service/         Contenido de las preguntas
│   ├── lifecycle-service/        Estados y ciclo de vida de la pregunta
│   ├── assignment-service/       Asignación de revisores y bandeja del revisor
│   ├── evaluation-service/       Evaluación de los revisores y unanimidad
│   ├── notification-service/     Bandeja de notificaciones
│   ├── email-service/            Correo simulado de cada notificación
│   ├── history-service/          Historial por pregunta
│   ├── audit-service/            Auditoría de acciones e inicios de sesión
│   ├── validation-service/       Validación estructural de preguntas (sin estado)
│   ├── simulation-service/       (primer diseño, tercer corte) Simulacros
│   ├── tracking-service/         (primer diseño, tercer corte) Seguimiento académico
│   └── user-service/             Usuarios de Keycloak y eventos de acceso
├── infraestructura/              Piezas que sostienen a los servicios
│   ├── api-gateway/              Entrada única, autorización, circuit breaker
│   ├── discovery-server/         Eureka (descubrimiento de servicios)
│   ├── edge/                     nginx que reparte entre las réplicas del gateway
│   └── keycloak/                 Realm con los usuarios y roles de prueba
└── pruebas-de-carga/             Script k6 de escalabilidad
```

Pruebas (`./mvnw test`, no necesitan Docker):

| Módulo | Pruebas |
|---|---|
| [bp-platform](plataforma/bp-platform/) | 70 |
| [question-service](servicios/question-service/) | 141 |
| [validation-service](servicios/validation-service/) | 53 |
| [lifecycle-service](servicios/lifecycle-service/) | 99 |
| [assignment-service](servicios/assignment-service/) | 96 |
| [evaluation-service](servicios/evaluation-service/) | 107 |
| [notification-service](servicios/notification-service/) | 85 |
| [email-service](servicios/email-service/) | 38 |
| [history-service](servicios/history-service/) | 54 |
| [audit-service](servicios/audit-service/) | 38 |
| [user-service](servicios/user-service/) | 62 |
| [api-gateway](infraestructura/api-gateway/) | 25 |
| [discovery-server](infraestructura/discovery-server/) | 3 |
| [../frontend](../frontend/) (Angular) | 166 |

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
`docker compose -f docker-compose.yml -f docker-compose.debug.yml up -d` (8081 a 8096).

### Réplicas

```bash
docker compose up -d --scale question-service=3 --scale api-gateway=2
```

Las réplicas se registran solas en Eureka y `edge` reparte entre las del gateway.

## Variables

Ver [`.env.example`](.env.example). Las principales: `SECURITY_MODE` (`jwt` por defecto; `headers` solo para
pruebas), `KEYCLOAK_PUBLIC_URL`, `GATEWAY_ALLOWED_ORIGINS`, `ASSIGNMENT_MAX_REVIEWERS`.

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

### Prueba de carga

```bash
docker run --rm -i -e PW=<clave de prueba> -e VUS=600 -e HOLD=1m \
  -e BASE=http://host.docker.internal:8080 -e KC=http://host.docker.internal:8180 \
  grafana/k6 run - < pruebas-de-carga/escalabilidad.js
```

Resultados y análisis en [`04 · Escalabilidad`](../docs/corte-2/04-escalabilidad.md): 600 usuarios,
0 % de error, p95 de 30 ms a ritmo humano y 826 ms en estrés (921 peticiones/s).

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
| assignment | `GET /api/reviews/pending-assignment`, `POST /api/reviews/{id}/assignments` | Administrador |
| assignment | `GET /api/reviews/assigned` | Revisor |
| assignment | `GET /api/reviews/config` | Todos |
| evaluation | `POST /api/reviews/{id}/evaluations` | Revisor |
| evaluation | `GET /api/reviews/{id}` | Todos |
| notification | `GET /api/notifications`, `…/unread-count`, `POST …/{id}/read`, `…/read-all` | Todos |
| email | `GET /api/emails` | Administrador |
| history | `GET /api/history/questions/{id}`, `/me` | Todos |
| audit | `GET /api/audit` | Administrador |
| user | `GET /api/users/me` (todos), `GET /api/users`, `POST /api/users/sync` (administrador) | — |

## Organización de cada servicio

```
domain/           Entidades, estados, reglas y eventos de dominio (sin Spring)
application/      Casos de uso y puertos de salida
infrastructure/   Adaptadores: JPA/PostgreSQL (Flyway), RabbitMQ, configuración
api/              Controlador REST, DTOs y manejo de errores
```

## Pendiente (corte 3)

DDD formal en un servicio, simulacros y seguimiento académico, el documento final y la sustentación.
