# Sistema de Banco de Preguntas Saber Pro

Proyecto de curso — Ingeniería de Software II, Universidad del Cauca
(Periodo 2-2026).

**Sistema para la Gestión, Validación y Administración de un Banco de
Preguntas para la Preparación de las Pruebas Saber Pro.** Aplicación de
escritorio en Java SE (Swing) que permite: redactar y revisar preguntas
por pares, y generar/presentar simulacros a partir de preguntas ya
publicadas.

Este repositorio es la continuación, ya como proyecto único, de los
talleres sueltos de la materia (login/roles del Taller 2, modelo C4 del
Taller 3, capas/MVC/Observer del Taller 4, microkernel del Taller 5 y API
REST del Taller 6) — ver la sección
[Historia y talleres relacionados](#historia-y-talleres-relacionados).

## Arquitectura: monolito modular

Por indicación de la docente del curso, el proyecto se construye como un
**monolito modular**: un único proceso desplegable (un solo `.jar`), pero
dividido internamente en módulos Maven con límites explícitos — cada uno
declara en su propio `pom.xml` de qué otros módulos depende, en vez de
que todo el código comparta paquetes y clases libremente. La razón:
eventualmente el proyecto migrará a microservicios, y partir de un
monolito ya modular hace ese cambio mucho más simple que partir de un
monolito con todo mezclado (solo hay que reemplazar las llamadas a Java
directas entre módulos por llamadas de red, la lógica de negocio interna
de cada módulo no cambia).

```
banco-preguntas-saberpro/          (pom padre — packaging "pom")
├── modulo-usuarios/                Autenticación, registro y roles (RF-01 a RF-03)
├── modulo-preguntas/                Banco de preguntas: redacción, listado y ciclo de vida RF-14 (depende de: nada)
├── modulo-simulacros/               Generación y presentación de simulacros HU-12 a HU-14 (depende de: modulo-preguntas, modulo-usuarios)
├── modulo-microkernel/              Generación de preguntas por plugins (Taller 5) (depende de: modulo-preguntas)
├── modulo-revision/                 Asignación de revisores a preguntas pendientes, HU-04 (implementado) (depende de: modulo-preguntas, modulo-usuarios)
├── modulo-api-rest/                 Microservicio REST con Spring Boot + JPA (Taller 6) (depende de: modulo-preguntas)
└── app/                             Composition root: arma los módulos y arranca la app de escritorio (depende de: usuarios, preguntas, simulacros, microkernel y revisión)
```

Grafo de dependencias entre módulos (siempre en una sola dirección, sin
ciclos):

```
app  →  modulo-simulacros  →  modulo-preguntas
 │                          ↘
 │                            modulo-usuarios
 ├──────────────────────────────────↗
 ├→  modulo-microkernel  →  modulo-preguntas
 └→  modulo-revision  →  modulo-preguntas, modulo-usuarios

modulo-api-rest  →  modulo-preguntas      (aparte: app no depende de él)
```

`modulo-preguntas` y `modulo-usuarios` no dependen de ningún otro módulo
del proyecto — son la base. `modulo-simulacros` depende de ambos (necesita
preguntas publicadas y necesita saber qué usuario presenta el simulacro).
`modulo-microkernel` solo depende de `modulo-preguntas` (genera preguntas
reales del banco a partir de plugins). `modulo-revision` depende de
`modulo-preguntas` (las preguntas y sus estados) y de `modulo-usuarios`
(para saber quiénes son los revisores). `modulo-api-rest` también solo
depende de `modulo-preguntas`: expone su servicio de dominio por HTTP y le
conecta una base de datos con JPA sin que el dominio sepa de Spring ni de
JPA. `app` es el único módulo que arma la aplicación de escritorio: hace la
inyección de dependencias manualmente y define `SesionRouter`, que decide
qué ventana abrir según el rol autenticado.

Cada módulo, a su vez, aplica internamente arquitectura en capas
(dominio / acceso a datos / presentación) y el micropatrón MVC — ver el
README de cada módulo para el detalle de sus capas.

## Cómo compilar y ejecutar

Requiere Java 17+ y Maven.

```bash
mvn test      # ejecuta las 330 pruebas de los módulos con lógica de negocio
mvn package   # genera app/target/banco-preguntas-saberpro.jar (con todas las dependencias)
java -jar app/target/banco-preguntas-saberpro.jar
```

Para levantar el **microservicio REST** (Taller 6) en `http://localhost:8080`:

```bash
cd modulo-api-rest
java -jar target/banco-preguntas-api.jar
```

Los endpoints, el formato de errores y la colección de Postman están en el
[README del módulo](modulo-api-rest/README.md).

Al iniciar se muestra el login. Usuarios de prueba (contraseña
`Saber2026!` para todos, se siembran solos la primera vez que se corre):

| Usuario | Rol | Ventana que abre |
|---|---|---|
| `autor1` | Autor de preguntas | Redactar preguntas (borrador → enviar a revisión) y ver su listado con paginación y filtros |
| `revisor1` | Revisor | Evaluar y decidir (Aprobar/Rechazar) |
| `docente1` | Docente | Generar simulacros |
| `estudiante1` | Estudiante | Presentar simulacros |
| `admin1` | Administrador | Tablero, estadísticas/gráfica del banco y asignación de revisores (HU-04) |

Al cerrar la ventana principal de un rol vuelve el login, para cambiar de usuario sin
reiniciar (las preguntas viven en memoria mientras la aplicación esté abierta).

## Primer corte

El primer corte implementa cuatro historias de usuario de alto valor:

| Historia | Descripción | Estado |
|---|---|---|
| HU-01 | El Autor crea una pregunta de selección múltiple con validación estructural | Implementada |
| HU-02 | El Autor cambia el estado de "Borrador" a "Pendiente de revisión" | Implementada |
| HU-03 | El Autor lista sus preguntas con paginación y filtros | Implementada |
| HU-04 | El Administrador asigna revisores a las preguntas pendientes | Implementada |

Las HU-01 a HU-04 son las del backlog del equipo en Jira; los códigos RF y las
historias HU03 (validación estructural) o HU-12 a HU-17 que aparecen más abajo son
los del documento del proyecto de curso.

## Flujo de trabajo y ramas

- `main`: entregas estables. Contiene el primer corte del proyecto de curso.
- `primer-corte`: integración de todo el primer corte, ya fusionada a `main`.
- Ramas de trabajo (`hu04-asignacion-revisores`, `docs-arquitectura`, …): cada
  integrante trabaja en la suya, con su propia identidad de Git, y la integra a
  `primer-corte` con un Pull Request. Se fusionan con "Create a merge commit" o
  "Rebase and merge" (no "Squash") para que cada commit conserve a su autor.
- Antes de fusionar se ejecuta `mvn test`.

## Estado actual del proyecto

**Implementado:**
- Login, registro y roles (RF-01 a RF-03), con SQLite y hash Argon2id.
- Redacción de preguntas por el Autor con todos sus campos (contexto,
  pregunta directa, cuatro opciones, respuesta correcta, justificación,
  bibliografía, competencia, tema, subtema y dificultad). Al guardar se aplica
  la validación estructural HU03 (RF-08 a RF-13), con una regla independiente
  por requisito, y los campos que incumplen se resaltan en rojo.
- Ciclo de vida RF-14 con transiciones válidas (RF-15): el Autor guarda un
  **borrador** (solo él puede modificarlo mientras lo sea, RF-06) y lo
  **envía a revisión** con confirmación; los estados se muestran con color.
- Listado "Mis preguntas" del Autor (RF-07): tabla paginada (5, 10 o 20 por
  página) con filtros por estado, competencia y texto (nombre, tema, subtema o
  pregunta, sin importar mayúsculas ni tildes), el estado de cada pregunta con
  su color, y apertura de la pregunta para verla o editarla. Sigue MVC: la vista
  `PanelMisPreguntas`, el controlador `MisPreguntasController` y el modelo
  `QuestionService.buscarDelAutor`.
- MVC explícito en las ventanas del Autor y del Revisor: las vistas (`GUIQuestions`,
  `GUIRevisor`) solo pintan y preguntan al usuario a través de una interfaz
  (`RedaccionPreguntaVista`, `RevisionVista`); las decisiones las toman los
  controladores (`RedaccionPreguntaController`, `RevisionController`), que se
  prueban sin ventana usando una vista falsa.
- Asignación de revisores por el Administrador (HU-04): al menos un revisor por
  pregunta pendiente, la pregunta pasa a "En revisión" y cada revisor se notifica
  por correo (simulado); el autor de la pregunta no se ofrece como su propio
  revisor — ver `modulo-revision`.
- Revisión por un Revisor: aprobar/rechazar solo las preguntas que se le
  asignaron.
- Generación de simulacros por el Docente filtrando por competencia,
  tema y dificultad (HU-12), usando solo preguntas publicadas.
- Presentación de un simulacro por el Estudiante con cronómetro y
  calificación automática al finalizar (HU-13/HU-14).
- Vista de estadísticas y gráfica de pastel del banco de preguntas para
  el Administrador (patrón Observer).
- Generación de preguntas por plugins (Taller 5): arquitectura
  Microkernel con carga dinámica por Reflexión desde `plugins.properties`
  (3 plugins: selección múltiple, caso de estudio, multimedia), cada uno
  validado por un pipeline de Tuberías y Filtros (4 filtros) antes de
  crear la pregunta real y notificar a las vistas observadoras — ver
  `modulo-microkernel`.
- API REST de preguntas (Taller 6): microservicio Spring Boot con CRUD
  (GET/POST/PUT/DELETE) sobre la `Question` real, persistencia con Spring
  Data JPA + H2 detrás del puerto `QuestionRepository`, y DELETE que archiva
  en vez de borrar (RNF-16) — ver `modulo-api-rest`.

**Pendiente:**
- Persistencia real de preguntas y simulacros (hoy son en memoria; solo
  usuarios usa SQLite).
- Historial de revisiones con observaciones (HU-10/HU-11) — hoy el Revisor
  decide sin dejar un registro de sus observaciones más allá del estado.
- Búsqueda de preguntas para todos los roles (HU-05) — hoy la búsqueda
  solo existe internamente (`QuestionService.buscarPublicadas`) para armar
  simulacros, y el listado con filtros es solo para el Autor.
- Historial de simulacros de un estudiante (HU-15) y estadísticas
  individuales de desempeño (HU-16).
- Reportes agregados de desempeño por grupo (HU-17).
- Vista propia del Administrador para gestionar usuarios (hoy usa el
  tablero genérico heredado del Taller 2).

## Historia y talleres relacionados

Este proyecto nació de una serie de talleres sueltos de la materia, en
el repositorio [`ingenieria-software-2`](https://github.com/DVLASZ/ingenieria-software-2):

- **Taller 2** — módulo de usuarios (SOLID), origen de `modulo-usuarios`.
- **Taller 3** — modelo de arquitectura C4 (Contexto/Contenedores/
  Componentes/Clases) de todo el sistema, incluyendo las historias de
  usuario (HU) y requisitos funcionales (RF) citados en este README.
- **Taller 4** — capas + MVC + Observer sobre el banco de preguntas,
  origen de `modulo-preguntas` y `modulo-simulacros`; se conserva intacto
  en ese repositorio como entregable del laboratorio, mientras este
  repositorio es donde el proyecto sigue avanzando.
- **Taller 5** — arquitectura Microkernel combinada con Tuberías y Filtros:
  generación de preguntas por plugins cargados por Reflexión y validados por
  un pipeline de cuatro filtros; origen de `modulo-microkernel`. Desde este
  taller el código se desarrolla directamente en este repositorio; en
  `ingenieria-software-2` solo queda una carpeta con su README y el enlace.
- **Taller 6** — API REST con Spring Boot y Spring Data JPA: microservicio con
  el CRUD de preguntas sobre la `Question` real del proyecto, con validación,
  manejo de errores y colección de Postman; origen de `modulo-api-rest`. Igual
  que el Taller 5, su carpeta en `ingenieria-software-2` solo deja el enlace.

## Documentación

La documentación de arquitectura (historias de usuario, prototipos, Sprint 1,
modificabilidad, C4 y UML, patrones de diseño y pruebas) está en la carpeta
[`docs/`](docs/README.md).

## Autores

- Edward Dávila — edwarddavila@unicauca.edu.co
- Laura Isabel Sánchez Fernández
- Kevin Yesid Castaño Herrera
