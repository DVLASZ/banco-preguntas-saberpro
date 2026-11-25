# Flujo de trabajo

## Ramas

| Rama | Para qué sirve |
|---|---|
| `main` | Entregas estables |
| `corte-1` | Monolito del primer corte, congelado: no se modifica |
| `corte-2` | Trabajo del segundo corte (microservicios y frontend) |
| `corte-3` | Trabajo del tercer corte (hexagonal y DDD, seguridad, simulacros, pruebas del dominio y documentación) |
| Ramas personales | Cada integrante trabaja en una rama propia, con su identidad de Git, y no toca `main` |

Los cambios se integran con Pull Request usando "Create a merge commit" o "Rebase and merge" (no "Squash"), para que
cada commit conserve a su autor.

## Commits

- Mensajes en español, en una sola línea corta y en minúscula que diga qué cambia, sin punto final.
- Cada commit debe dejar el proyecto compilando y con las pruebas en verde.
- Los commits llevan solo a su autor: sin líneas de coautoría de herramientas.
- El archivo `.mailmap` junta las distintas firmas de una misma persona para que GitHub y `git shortlog` cuenten bien.

## Cómo se ve el avance en Git

Cada corte tiene su rama y cada historia de usuario se reparte en commits pequeños, uno por cambio. El historial se lee con:

```bash
git log --oneline --graph corte-3
git shortlog -sn corte-3
```

La primera línea da la lista de cambios en orden y la segunda cuenta los commits de cada integrante. La rama de cada corte queda como foto de esa entrega.

## Cómo se ve el avance en Jira

Cada historia tiene sus subtareas, un responsable y un estado (Por hacer, En curso, Finalizada). El tablero del sprint muestra lo que falta y el
backlog guarda lo que quedó fuera. Las historias que no se terminan se comentan con la razón y pasan al siguiente sprint.

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
| Jira (proyecto SCRUM) | Historias, tareas y sprints; Sprint 1 del 17 al 26 de septiembre, Sprint 2 del 13 al 30 de octubre y Sprint 3 del 4 al 18 de noviembre de 2026 |
| Figma | Prototipos de las pantallas (ver [prototipos del corte 2](../corte-2/06-prototipos.md)) |
| GitHub | Repositorio y Pull Requests |

## Archivos que no se versionan

Variables de entorno (`.env`), bases de datos locales (`*.db`), carpetas de compilación (`target/`, `dist/`,
`node_modules/`) y el material personal de estudio o sustentación.
