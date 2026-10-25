# email-service · Correo simulado

Envía (de forma simulada) un correo por cada aviso creado y deja un registro que el administrador puede consultar.

| | |
|---|---|
| **Puerto** | 8090 (`EMAIL_SERVICE_PORT`) |
| **Base de datos** | PostgreSQL `email_db` (puerto 5440 en local), con migraciones de Flyway |
| **Swagger** | `http://localhost:8090/swagger-ui.html` (directo; el gateway lo expone en la nueva distribución) |
| **Pruebas** | 38 pruebas (unitarias, de API, de persistencia y de arquitectura con ArchUnit) |

## Responsabilidad

Es el contexto de **Correo electrónico**. Escucha `notification.created` y envía el correo del aviso. El envío es una simulación: no sale ningún mensaje real, pero se registra a quién, con qué asunto y si se logró.

## Reglas de negocio

- El envío tiene reintentos (`EMAIL_ATTEMPTS`, 3 por defecto) y deja en el registro cada intento y su resultado (patrón Decorator: el emisor real se envuelve con el reintento y con el registro).
- El correo se arma con el usuario y el dominio configurado (`EMAIL_DOMAIN`).
- Solo el administrador consulta el registro, con filtro por estado y paginación.
- Un aviso repetido no envía dos correos.

## API REST

Las rutas pasan por el gateway, que valida el token de Keycloak y comprueba el rol; el servicio vuelve a comprobarlo con el usuario autenticado que recibe.

| Método | Ruta | Qué hace |
|---|---|---|
| `GET` | `/api/emails` | Correos simulados registrados, el más reciente primero; se puede filtrar por estado. Rol ADMIN. |

## Mensajería (RabbitMQ)

**Publica:** nada; es un consumidor puro.

**Consume:**

| Cola | Routing key | Efecto |
|---|---|---|
| `email.q.notification-created` | `notification.created` | Envía el correo del aviso y lo registra |

## Base de datos

| Migración | Contenido |
|---|---|
| `V1__esquema_inicial.sql` | email_db: registro de los correos simulados enviados a partir de las notificaciones |

## Estructura

```
src/main/java/co/unicauca/bancopreguntas/email/
├── api/              Controlador REST, DTO y manejo de errores
├── application/      Casos de uso y puertos (interfaces que el dominio necesita)
├── domain/           Reglas de negocio y eventos; no depende de Spring ni de la base de datos
└── infrastructure/   Adaptadores: JPA, RabbitMQ, clientes REST y configuración
```

**Patrones de diseño que usa:** Decorator (`EmailConReintentos`, `EmailConRegistro`) y Adapter. Detalle en [patrones](../../../docs/corte-2/05-patrones.md).

## Configuración

| Variable | Valor por defecto | Para qué sirve |
|---|---|---|
| `EMAIL_DB_URL` | `jdbc:postgresql://localhost:5440/email_db` | URL de la base de datos |
| `EMAIL_DB_USER` | `email` | Usuario de la base |
| `EMAIL_DB_PASSWORD` | `email` | Clave de la base |
| `RABBITMQ_HOST` | `localhost` | Servidor de RabbitMQ |
| `RABBITMQ_PORT` | `5672` | Puerto AMQP de RabbitMQ |
| `RABBITMQ_USER` | `guest` | Usuario de RabbitMQ |
| `RABBITMQ_PASSWORD` | `guest` | Clave de RabbitMQ |
| `EMAIL_SERVICE_PORT` | `8090` | Puerto HTTP |
| `EUREKA_ENABLED` | `true` | Registrarse en Eureka (se apaga en las pruebas) |
| `EUREKA_URL` | `http://localhost:8761/eureka` | Dirección de Eureka |
| `EMAIL_DOMAIN` | `bancopreguntas.local` | Dominio de las direcciones de correo |
| `EMAIL_ATTEMPTS` | `3` | Intentos de envío |

## Ejecutar y probar

```bash
# desde microservicios/
./mvnw -pl servicios/email-service -am test          # pruebas del servicio
docker compose up -d --build email-service    # el servicio con sus dependencias
```

La pila completa se levanta como se explica en el [README de microservicios](../../README.md). Para recorrer el servicio a mano, importar su colección de Postman y el entorno `SaberPro_Local`.
