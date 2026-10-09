# Microservicios del Banco de Preguntas Saber Pro

Segunda iteración: la solución distribuida con microservicios orientados a eventos. El monolito del primer
corte se conserva en la carpeta [`monolito/`](../monolito/README.md) y en la rama `corte-1`. El diseño está en
[`docs/corte-2`](../docs/corte-2/README.md) y la forma de reutilizar el monolito en
[`reutilizacion-del-monolito.md`](../docs/general/reutilizacion-del-monolito.md).

> Proyecto Maven independiente del monolito: para abrirlo en el IDE, añade `microservicios/pom.xml` como proyecto
> Maven.

## Organización de la carpeta

```
microservicios/
├── pom.xml                  Pom padre: compila todos los módulos
├── docker-compose.yml       PostgreSQL, RabbitMQ y question-service con un solo comando
├── .env.example             Variables de entorno de desarrollo
├── plataforma/
│   └── bp-platform/         Biblioteca común: sobre de eventos, Outbox, errores, identidad del usuario, OpenAPI
└── servicios/
    └── question-service/    HU-01 a HU-03: preguntas, validación estructural y ciclo de vida, con eventos (puerto 8082)
```

Los demás servicios (revisión, notificaciones, historial, usuarios), el gateway y Keycloak se agregan en las
siguientes partes, en `servicios/` e `infraestructura/`.

## Requisitos

- JDK 21 (el proyecto trae `mvnw`).
- Docker Desktop con la virtualización activa, solo para PostgreSQL, RabbitMQ y las imágenes. Las pruebas no lo
  necesitan.

## Arrancar

Todo en contenedores:

```bash
cd microservicios
docker compose up -d --build
```

O solo la infraestructura, con el servicio ejecutado desde el IDE (clase `QuestionServiceApplication`, con
`-Deureka.client.enabled=false`):

```bash
docker compose up -d postgres-question rabbitmq
```

| Qué | Dónde |
|---|---|
| Swagger de preguntas | http://localhost:8082/swagger-ui.html |
| RabbitMQ (usuario y clave `guest`) | http://localhost:15672 |

Para cambiar usuarios o claves de desarrollo, copia `.env.example` como `.env` (no se versiona).

## Probar el API a mano

En esta parte la identidad llega en las cabeceras `X-User-Id` y `X-User-Roles` (el modo `jwt` con Keycloak se
agrega después):

```bash
curl -s -X POST http://localhost:8082/api/questions \
  -H "X-User-Id: autor1" -H "X-User-Roles: AUTHOR" -H "Content-Type: application/json" \
  -d '{"nombre":"Patrón Observer","contexto":"Una aplicación debe actualizar varias ventanas cuando cambian los datos.",
       "enunciado":"¿Qué patrón notifica a varios objetos cuando cambia el estado de otro?",
       "opcionA":"Factory","opcionB":"Observer","opcionC":"Singleton","opcionD":"Adapter","respuestaCorrecta":"B",
       "justificacion":"Observer define una dependencia uno a muchos.","bibliografia":"Gamma et al. (1994).",
       "competencia":"LECTURA_CRITICA","tema":"Patrones de diseño","subtema":"Observer","dificultad":"INTERMEDIO"}'
```

Después se envía a revisión con `POST /api/questions/{id}/submit` y se consulta con `GET /api/questions/{id}`.
Mientras no exista `review-service`, los eventos que esperaría se pueden publicar a mano en el panel de RabbitMQ
(exchange `bancopreguntas.events`):

| Routing key | Cuerpo | Efecto |
|---|---|---|
| `review.reviewers-assigned` | `{"eventId":"e-1","type":"review.reviewers-assigned","occurredAt":"2026-10-08T15:00:00Z","data":{"questionId":"<id>"}}` | De Pendiente de revisión a En revisión |
| `review.completed` | `{"eventId":"e-2","type":"review.completed","occurredAt":"2026-10-08T15:00:00Z","data":{"questionId":"<id>","result":"APPROVED"}}` | De En revisión a Aprobada (`REJECTED` para Rechazada) |

El `eventId` debe ser distinto en cada evento: un identificador repetido se ignora (idempotencia).

## Pruebas

Desde `microservicios/` (si se ejecuta dentro de un módulo, `-pl` no encuentra los demás):

```bash
./mvnw test
```

Sin Docker: PostgreSQL embebido y RabbitMQ simulado. Cubren dominio, aplicación, API (`MockMvc`), persistencia
(Flyway sobre PostgreSQL real), mensajería (incluido el Outbox) y reglas de arquitectura (ArchUnit: el dominio
no depende de Spring).

## Organización del servicio

```
domain/           Entidad Question, estados, reglas de validación y eventos de dominio (sin Spring)
application/      Casos de uso y puertos de salida
infrastructure/   Adaptadores: JPA/PostgreSQL (Flyway), RabbitMQ, configuración
api/              Controlador REST, DTOs y manejo de errores
```

El dominio no depende de ningún marco: es el del primer corte, reutilizado. Los adaptadores implementan los
puertos, así que se pueden sustituir sin tocar la lógica.
