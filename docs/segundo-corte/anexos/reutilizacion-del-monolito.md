# Reutilización del monolito del primer corte

> Plan de trabajo, sujeto a cambios. Describe cómo se aprovecha el monolito modular del corte 1 al construir
> los microservicios del corte 2, sin perderlo ni duplicar esfuerzo sin necesidad.

## Principio

El monolito **no se descarta**: ya tiene el dominio, las reglas de negocio y las pruebas validadas en el corte 1.
La migración consiste en **trasladar** ese código a servicios con su propia base de datos y a un frontend web, y en
cambiar solo lo que cambia con la arquitectura: las llamadas directas entre módulos pasan a ser eventos o
llamadas de red, y la interfaz Swing pasa a ser una aplicación web. La lógica de dominio se conserva.

El monolito sigue disponible de dos formas:

- La rama **`corte-1`** guarda el estado entregado del primer corte y no se modifica.
- Los módulos Maven de la raíz (`modulo-*` y `app`) siguen en el repositorio y la aplicación de escritorio sigue
  ejecutándose con `MainApp`. Sirve como referencia viva y para demostrar el corte 1.

## Qué módulo se convierte en qué

| Módulo del monolito | Destino en la arquitectura distribuida | Qué se reutiliza | Qué cambia |
|---|---|---|---|
| `modulo-preguntas` | `question-service` | El dominio completo: `Question`, `EstadoPregunta`, `ContenidoPregunta`, filtros, paginación y las reglas de validación estructural | La persistencia pasa de JDBC/SQLite a JPA sobre PostgreSQL con Flyway; las vistas Swing (MVC y Observer) se reemplazan por una API REST y eventos |
| `modulo-revision` | `review-service` | Asignación de revisores, directorio de revisores y notificador, como puertos | Se amplía a 1–3 revisores, evaluaciones con observaciones y decisión por unanimidad; el aviso a los revisores pasa a ser un evento |
| `modulo-usuarios` | Keycloak + `user-service` | Roles (administrador, autor, revisor) y las reglas de contraseña como referencia | La autenticación deja de ser propia: la emite Keycloak y los servicios validan el token |
| `modulo-microkernel` | Servicio de generación por plugins (por definir) | Núcleo, plugins y tuberías tal como están | Se expondría como servicio que publica preguntas en el banco; no es parte de esta iteración |
| `modulo-simulacros` | Contexto de Simulacros (corte 3) | Dominio de generación y presentación | Quedará como servicio propio cuando se aborde ese contexto |
| `modulo-api-rest` | Absorbido por `question-service` | Es el Taller 6 (Spring Boot + JPA) y sirvió de punto de partida | Deja de ser un módulo aparte |
| `app` | Reemplazado por `api-gateway` y el frontend | La idea de *composition root* | El ensamblado ahora lo hacen Docker Compose, Eureka y el gateway |

## Cómo se hace la migración

1. **Copiar, no mover.** El código de dominio se copia al servicio nuevo y el módulo original queda intacto. Así el
   monolito sigue compilando y las pruebas del corte 1 siguen pasando.
2. **Conservar las pruebas.** Las pruebas de dominio del monolito se trasladan junto con el código; si pasan en el
   servicio nuevo, la migración no cambió el comportamiento.
3. **Sustituir los puertos.** Los repositorios y notificadores ya son interfaces; el servicio nuevo implementa los
   mismos puertos con adaptadores nuevos (JPA, RabbitMQ).
4. **Reemplazar las llamadas directas por eventos.** Donde un módulo llamaba a otro (por ejemplo, revisión leyendo
   preguntas pendientes) el servicio publica o consume un evento del catálogo de
   [contextos y eventos](../02-contextos-y-eventos.md).
5. **Un servicio a la vez.** Se empieza por `question-service`, que es la base; después revisión, notificaciones y
   usuarios, y por último el gateway y el frontend.

## Convivencia en el repositorio y en el IDE

| Carpeta | Qué es | Cómo se ejecuta |
|---|---|---|
| Raíz (`pom.xml`, `modulo-*`, `app`) | Monolito del corte 1 | `MainApp` en `app`: abre la aplicación de escritorio Swing |
| `microservicios/` | Solución distribuida del corte 2, con su propio `pom.xml` padre | Docker Compose, o cada servicio desde el IDE |
| `frontend/` | Aplicación web | `ng serve`, o dentro de Docker Compose |

Los dos proyectos Maven son independientes: el `pom.xml` de la raíz no incluye `microservicios/`, por eso al
importar la raíz en IntelliJ solo aparece el monolito. Para trabajar con los microservicios hay que añadir
`microservicios/pom.xml` como segundo proyecto Maven del IDE.

## Qué se hará con el monolito al final

- Durante el corte 2 los módulos se mantienen donde están.
- Cuando todos los contextos estén migrados, se decide entre dejarlos como referencia o moverlos a una carpeta
  `monolito/` para que la raíz del repositorio quede centrada en la solución distribuida. Ese cambio de carpeta
  se hará de una sola vez y con el equipo, porque afecta las rutas de la documentación.
- Nunca se borra la rama `corte-1`.
