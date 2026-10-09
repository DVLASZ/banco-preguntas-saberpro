# Módulos del monolito

Módulos Maven del monolito del primer corte. Cada uno declara en su `pom.xml` de qué otros módulos depende; el
detalle y el grafo de dependencias están en el [README del monolito](../README.md).

| Módulo | Contenido |
|---|---|
| [`modulo-usuarios`](modulo-usuarios/) | Autenticación, registro y roles |
| [`modulo-preguntas`](modulo-preguntas/) | Banco de preguntas: redacción, validación, listado y ciclo de vida |
| [`modulo-simulacros`](modulo-simulacros/) | Generación y presentación de simulacros |
| [`modulo-microkernel`](modulo-microkernel/) | Generación de preguntas por plugins (Taller 5) |
| [`modulo-revision`](modulo-revision/) | Asignación de revisores a preguntas pendientes (HU-04) |
| [`modulo-api-rest`](modulo-api-rest/) | API REST con Spring Boot y JPA (Taller 6) |

El módulo `app`, hermano de esta carpeta, arma los módulos y arranca la aplicación de escritorio.
