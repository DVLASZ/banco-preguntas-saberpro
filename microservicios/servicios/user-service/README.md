# user-service · Usuarios

> **Primera distribución (7 servicios).** Este README describe el servicio tal como está publicado hoy. En la nueva distribución, de 14
> servicios, se mantiene igual en la nueva distribución; ver el [README de los servicios](../README.md).

Mantiene el directorio de usuarios del banco, sincronizado desde Keycloak, y avisa de los inicios de sesión.

| | |
|---|---|
| **Puerto** | 8081 (`USER_SERVICE_PORT`) |
| **Base de datos** | PostgreSQL `user_db` (puerto 5433 en local), con migraciones de Flyway |
| **Swagger** | `http://localhost:8080/docs/user-service/v3/api-docs` (a través del gateway) |
| **Contrato exportado** | [`openapi-user-service.json`](../../../docs/corte-2/api/openapi/openapi-user-service.json) y [colección de Postman](../../../docs/corte-2/api/postman/user-service.postman_collection.json) |
| **Pruebas** | 62 pruebas (unitarias, de API, de persistencia y de arquitectura con ArchUnit) |

## Responsabilidad

Es el contexto de **Gestión de Usuarios**. Keycloak autentica y emite los tokens; este servicio guarda una copia de los usuarios con sus roles (administrador, autor, revisor, docente y estudiante) y su perfil, para que el resto del sistema consulte quién es quién. Detecta los inicios de sesión y los publica para la auditoría.

## Reglas de negocio

- La sincronización con Keycloak usa la API de administración con una cuenta de servicio (`bancopreguntas-admin`), de forma
  periódica y bajo demanda (`POST /api/users/sync`, solo administrador). Se puede apagar con `USER_SYNC_ENABLED=false`.
- `GET /api/users/me` responde a **cualquier rol** del sistema, incluidos docente y estudiante.
- Los usuarios nuevos de Keycloak aparecen en el directorio sin tocar código.
- Los inicios de sesión se detectan leyendo los eventos `LOGIN` y `LOGIN_ERROR` de Keycloak, con un cursor para no repetirlos, y se
  publican; nunca se guarda ni se publica una contraseña.

## API REST

Todas las rutas pasan por el gateway, que valida el token de Keycloak y comprueba el rol; el servicio vuelve a
comprobarlo con el usuario autenticado que recibe.

| Método | Ruta | Qué hace |
|---|---|---|
| `GET` | `/api/users` | Listar usuarios, por ejemplo los revisores disponibles. Rol ADMIN. |
| `GET` | `/api/users/me` | Quién soy: mi usuario, roles y perfil. Cualquier rol del sistema. |
| `POST` | `/api/users/sync` | Sincronizar ahora con Keycloak: usuarios nuevos y inicios de sesión. Rol ADMIN. |

## Mensajería (RabbitMQ)

**Publica** (con Outbox transaccional):

| Evento | Cuándo | Quién lo consume |
|---|---|---|
| `auth.login-succeeded` | Un usuario inicia sesión | history |
| `auth.login-failed` | Un intento de inicio de sesión falla | history |

**Consume:** nada.

## Base de datos

| Migración | Contenido |
|---|---|
| `V1__esquema_inicial.sql` | Perfil de usuario y cursor con el que se leen los inicios de sesión de Keycloak |
| `V2__bandeja_de_salida.sql` | Tabla del Outbox |

## Estructura

```
src/main/java/co/unicauca/bancopreguntas/user/
├── api/              Controlador REST, DTO y manejo de errores
├── application/      Casos de uso y puertos (interfaces que el dominio necesita)
├── domain/           Reglas de negocio y eventos; no depende de Spring ni de la base de datos
└── infrastructure/   Adaptadores: JPA, RabbitMQ, cliente de la API de administración de Keycloak
```

**Patrones de diseño que usa:** Adapter (`KeycloakIdentityProvider` oculta la API de Keycloak detrás de un puerto) y Observer.

## Configuración

| Variable | Valor por defecto | Para qué sirve |
|---|---|---|
| `USER_DB_URL` | `jdbc:postgresql://localhost:5433/user_db` | URL de la base de datos |
| `USER_DB_USER` | `userdb` | Usuario de la base |
| `USER_DB_PASSWORD` | `userdb` | Clave de la base |
| `RABBITMQ_HOST` | `localhost` | Servidor de RabbitMQ |
| `RABBITMQ_PORT` | `5672` | Puerto AMQP de RabbitMQ |
| `RABBITMQ_USER` | `guest` | Usuario de RabbitMQ |
| `RABBITMQ_PASSWORD` | `guest` | Clave de RabbitMQ |
| `USER_SERVICE_PORT` | `8081` | Puerto HTTP |
| `KEYCLOAK_URL` | `http://localhost:8180` | Dirección de Keycloak |
| `KEYCLOAK_REALM` | `bancopreguntas` | Realm del banco |
| `KEYCLOAK_ADMIN_CLIENT_ID` | `bancopreguntas-admin` | Cliente de servicio con permiso para leer usuarios |
| `KEYCLOAK_ADMIN_CLIENT_SECRET` | `(vacío)` | Secreto de ese cliente |
| `USER_SYNC_ENABLED` | `true` | Activa la sincronización periódica |
| `EUREKA_ENABLED` | `true` | Registrarse en Eureka (se apaga en las pruebas) |
| `EUREKA_URL` | `http://localhost:8761/eureka` | Dirección de Eureka |

## Ejecutar y probar

```bash
# desde microservicios/
./mvnw -pl servicios/user-service -am test          # pruebas del servicio
docker compose up -d --build user-service    # el servicio con sus dependencias (base, RabbitMQ, Eureka, Keycloak)
```

La pila completa se levanta como se explica en el [README de microservicios](../../README.md). Para recorrer el
servicio a mano, importar su colección de Postman y el entorno `SaberPro_Local`.
