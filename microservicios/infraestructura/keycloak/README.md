# keycloak · Realm de demostración

Contiene [`bancopreguntas-realm.json`](bancopreguntas-realm.json), el realm `bancopreguntas` que Keycloak importa al arrancar
(`docker compose up -d keycloak`, consola en `http://localhost:8180`). Autentica a los usuarios y emite los tokens JWT que valida el
gateway.

## Roles

`ADMIN`, `AUTHOR`, `REVIEWER`, `TEACHER` y `STUDENT`.

## Clientes

| Cliente | Tipo | Para qué |
|---|---|---|
| `bancopreguntas-web` | Público, *Authorization Code* con PKCE | La aplicación web de Angular |
| `bancopreguntas-admin` | Confidencial, cuenta de servicio | `user-service`, para leer los usuarios y los eventos de inicio de sesión |

## Usuarios de demostración

La clave de todos es `Banco2026*`. Son datos de prueba para el entorno local; en un despliegue real se cambian.

| Rol | Usuarios |
|---|---|
| `ADMIN` | `admin` |
| `AUTHOR` | `autor1`, `autor2` |
| `REVIEWER` | `revisor1` a `revisor5` |
| `TEACHER` | `docente1` |
| `STUDENT` | `estudiante1`, `estudiante2`, `estudiante3` |

Un usuario nuevo creado en Keycloak aparece en el sistema cuando `user-service` sincroniza (periódicamente o con
`POST /api/users/sync`).
