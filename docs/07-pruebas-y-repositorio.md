# Pruebas unitarias y organización del repositorio

## 1. Pruebas unitarias automatizadas

Hay pruebas unitarias para las entidades y los servicios del dominio de cada módulo,
para los controladores de las ventanas y para las reglas de validación. Se ejecutan con
JUnit 5 y Mockito:

```bash
mvn test
```

| Módulo | Pruebas | Qué cubren |
|---|---|---|
| `modulo-usuarios` | 37 | `User`, `UserService`, contraseñas (Argon2id y política), repositorio SQLite, menús por rol y su registro, fábricas |
| `modulo-preguntas` | 154 | `Question`, `QuestionDistractors`, `ContenidoPregunta`, `EstadoPregunta`, `QuestionService`, las siete reglas de validación y el validador, `FiltroPreguntas`, `Pagina`, búsqueda paginada, los tres controladores de las ventanas |
| `modulo-simulacros` | 25 | `Simulacro`, `IntentoSimulacro`, `SimulacroService`, repositorio |
| `modulo-microkernel` | 39 | Microkernel, plugins, filtros y pipeline, solicitud de generación |
| `modulo-revision` | 3 | `Revisor` (las de HU-04 se suman al terminar la historia) |
| `modulo-api-rest` | 52 | Controlador REST, servicio, mapper, adaptador JPA, integración |
| **Total** | **310** | |

**Estilo de las pruebas.** Cada prueba sigue *Given / When / Then* y tiene un nombre que
describe el comportamiento (por ejemplo,
`enviarARevision_sinConfirmacion_laPreguntaSigueEnBorrador`). Los servicios se prueban
con dobles de sus dependencias (Mockito) o con el repositorio en memoria; los
controladores se prueban con una vista falsa.

## 2. Organización del repositorio

```
banco-preguntas-saberpro/
├── pom.xml                  Proyecto padre (módulos y versiones)
├── README.md                Descripción, cómo compilar y ejecutar, estado del proyecto
├── docs/                    Documentación de arquitectura (esta carpeta)
├── app/                     Composition root y ejecutable
├── modulo-usuarios/         Login, registro y roles
├── modulo-preguntas/        Banco de preguntas (HU-01, HU-02, HU-03)
├── modulo-revision/         Asignación de revisores (HU-04)
├── modulo-simulacros/       Simulacros (docente y estudiante)
├── modulo-microkernel/      Generación de preguntas por plugins
└── modulo-api-rest/         Microservicio REST
```

Cada módulo tiene su propio `pom.xml` y, dentro, los paquetes `domain`, `access` y
`presentation` de las tres capas.

**Cómo compilar y ejecutar**

```bash
mvn test                                            # todas las pruebas
mvn package                                         # genera app/target/banco-preguntas-saberpro.jar
java -jar app/target/banco-preguntas-saberpro.jar   # abre el login
```

Usuarios de prueba (contraseña `Saber2026!`): `autor1` (Autor), `revisor1` (Revisor),
`docente1` (Docente), `estudiante1` (Estudiante) y `admin1` (Administrador).

## 3. Trabajo en equipo

- **Tablero de tareas:** las historias, sus criterios y sus subtareas están en el
  tablero de Jira del Sprint 1.
- **Commits:** cada integrante del equipo trabaja con su propia identidad de Git, de
  modo que cada commit queda a nombre de quien lo hizo.
- **Mensajes de commit:** describen el cambio en una línea y, cuando corresponde,
  indican la historia (por ejemplo, "(HU-04)").
