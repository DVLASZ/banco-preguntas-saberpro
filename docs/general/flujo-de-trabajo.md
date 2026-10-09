# Flujo de trabajo

## Ramas

| Rama | Para qué sirve |
|---|---|
| `main` | Entregas estables |
| `corte-1` | Monolito del primer corte, congelado: no se modifica |
| `corte-2` | Trabajo del segundo corte (microservicios y frontend) |
| `corte-3` | Se crea al empezar el tercer corte |
| Ramas personales | Cada integrante trabaja en una rama propia, con su identidad de Git, y no toca `main` |

Los cambios se integran con Pull Request usando «Create a merge commit» o «Rebase and merge» (no «Squash»), para que
cada commit conserve a su autor.

## Commits

- Mensajes en español, en una línea que diga qué cambia y, si hace falta, un párrafo con el porqué.
- Cada commit debe dejar el proyecto compilando y con las pruebas en verde.
- Los commits llevan solo a su autor: sin líneas de coautoría de herramientas.

## Publicación por partes

El código se publica de a poco, una parte coherente por commit y siempre compilando: primero la documentación y
la base de los microservicios, después cada servicio, el gateway, la seguridad y el frontend. La documentación de
un componente se publica junto con él (o después), nunca antes.

## Antes de integrar

| Qué | Comando |
|---|---|
| Pruebas del monolito | `cd monolito && mvn test` |
| Pruebas de los microservicios | `cd microservicios && ./mvnw test` |

## Herramientas

| Herramienta | Uso |
|---|---|
| Jira (proyecto SCRUM) | Historias, tareas y sprints; el Sprint 2 va del 13 al 30 de octubre de 2026 |
| Figma | Prototipos de las pantallas (ver [prototipos del corte 2](../corte-2/06-prototipos.md)) |
| GitHub | Repositorio y Pull Requests |

## Archivos que no se versionan

Variables de entorno (`.env`), bases de datos locales (`*.db`), carpetas de compilación (`target/`, `dist/`,
`node_modules/`) y el material personal de estudio o sustentación.
