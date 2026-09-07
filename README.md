# Sistema de Banco de Preguntas Saber Pro

Proyecto de curso — Ingeniería de Software II, Universidad del Cauca
(Periodo 2-2026).

**Sistema para la Gestión, Validación y Administración de un Banco de
Preguntas para la Preparación de las Pruebas Saber Pro.** Aplicación de
escritorio en Java SE (Swing) que permite: redactar y revisar preguntas
por pares, y generar/presentar simulacros a partir de preguntas ya
publicadas.

Este repositorio es la continuación, ya como proyecto único, de los
talleres sueltos de la materia (login/roles del Taller 2, capas/MVC/
Observer del Taller 4, modelo C4 del Taller 3) — ver la sección
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
├── modulo-preguntas/                Banco de preguntas: redacción y ciclo de vida RF-14 (depende de: nada)
├── modulo-simulacros/               Generación y presentación de simulacros HU-12 a HU-14 (depende de: modulo-preguntas, modulo-usuarios)
└── app/                             Composition root: arma los módulos y arranca la app (depende de: los 3 anteriores)
```

Grafo de dependencias entre módulos (siempre en una sola dirección, sin
ciclos):

```
app  →  modulo-simulacros  →  modulo-preguntas
 │                          ↘
 │                            modulo-usuarios
 └────────────────────────────────↗
```

`modulo-preguntas` y `modulo-usuarios` no dependen de ningún otro módulo
del proyecto — son la base. `modulo-simulacros` depende de ambos (necesita
preguntas publicadas y necesita saber qué usuario presenta el simulacro).
`app` es el único módulo que conoce a los cuatro: arma la inyección de
dependencias manualmente y define `SesionRouter`, que decide qué ventana
abrir según el rol autenticado.

Cada módulo, a su vez, aplica internamente arquitectura en capas
(dominio / acceso a datos / presentación) y el micropatrón MVC — ver el
README de cada módulo para el detalle de sus capas.

## Cómo compilar y ejecutar

Requiere Java 17+ y Maven.

```bash
mvn test      # ejecuta las 73 pruebas de los 3 módulos con lógica de negocio
mvn package   # genera app/target/banco-preguntas-saberpro.jar (con todas las dependencias)
java -jar app/target/banco-preguntas-saberpro.jar
```

Al iniciar se muestra el login. Usuarios de prueba (contraseña
`Saber2026!` para todos, se siembran solos la primera vez que se corre):

| Usuario | Rol | Ventana que abre |
|---|---|---|
| `autor1` | Autor de preguntas | Redactar/crear preguntas |
| `revisor1` | Revisor | Evaluar y decidir (Aprobar/Rechazar) |
| `docente1` | Docente | Generar simulacros |
| `estudiante1` | Estudiante | Presentar simulacros |
| `admin1` | Administrador | Tablero + estadísticas/gráfica del banco de preguntas |

## Estado actual del proyecto

**Implementado:**
- Login, registro y roles (RF-01 a RF-03), con SQLite y hash Argon2id.
- Redacción de preguntas por el Autor, con competencia/tema/dificultad y
  ciclo de vida RF-14 completo (7 estados).
- Revisión por un Revisor: aprobar/rechazar (RF-15/RF-16, versión con un
  solo revisor por pregunta).
- Generación de simulacros por el Docente filtrando por competencia,
  tema y dificultad (HU-12), usando solo preguntas publicadas.
- Presentación de un simulacro por el Estudiante con cronómetro y
  calificación automática al finalizar (HU-13/HU-14).
- Vista de estadísticas y gráfica de pastel del banco de preguntas para
  el Administrador (patrón Observer).

**Pendiente** (ver el desglose completo de qué falta y en qué orden
convendría abordarlo en el historial de la conversación/planeación del
proyecto — o pídele a Claude que te lo resuma de nuevo):
- Persistencia real de preguntas y simulacros (hoy son en memoria; solo
  usuarios usa SQLite).
- Asignación de uno o más revisores por pregunta (HU-09) e historial de
  revisiones con observaciones (HU-10/HU-11) — hoy el Revisor decide sin
  dejar un registro de sus observaciones más allá del estado.
- Validaciones estructurales automáticas al crear una pregunta (HU-06,
  HU-07): contexto obligatorio, 4 distractores, prohibir frases como
  "todas las anteriores", reglas de longitud/gramática.
- Búsqueda/filtro de preguntas por competencia, tema, dificultad o
  estado para todos los roles (HU-05) — hoy solo existe internamente
  (`QuestionService.buscarPublicadas`) para armar simulacros.
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

## Autores

- Edward Dávila — edwarddavila@unicauca.edu.co
- Laura Isabel Sánchez Fernández
