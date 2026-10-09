# Patrones de diseño, principios SOLID y decisiones de diseño

## 1. Patrones de diseño implementados

| Patrón | Dónde | Contexto del problema y cómo se aplicó |
|---|---|---|
| **MVC** (micropatrón) | `GUIQuestions` + `RedaccionPreguntaController`; `PanelMisPreguntas` + `MisPreguntasController`; `GUIRevisor` + `RevisionController` | La lógica de cada ventana estaba mezclada con Swing y no se podía probar. La vista solo pinta y le avisa al controlador lo que hace el usuario; el controlador decide y usa el modelo (`QuestionService`). La vista se declara como interfaz (`RedaccionPreguntaVista`, `MisPreguntasVista`, `RevisionVista`), así el controlador se prueba con una vista falsa. |
| **Observer** | `Subject` / `Observer`; `QuestionService` como sujeto; `GUIObserver1` (estadísticas) y `GUIObserver2` (gráfica) como observadores | Las vistas de estadísticas deben actualizarse cada vez que cambia una pregunta, sin que el servicio conozca las ventanas. El servicio notifica y cada vista se refresca sola. |
| **Builder** | `Question.builder()` | Una pregunta tiene 14 atributos; un constructor con tantos parámetros es ilegible y propenso a errores de orden. El constructor mantiene las invariantes mínimas y el builder permite armar la pregunta campo a campo. |
| **Strategy** | `ValidationRule` y sus siete reglas; `IMenuProvider` y un menú por rol; `IPasswordHasher` y `IPasswordPolicy`; `FuenteDePreguntasParaRevisar` | Cada regla de validación estructural (RF-08 a RF-13) es una estrategia intercambiable que `QuestionValidator` ejecuta sin conocerlas; los menús se eligen según el rol; el Revisor recibe su lista de preguntas de una fuente que se puede cambiar. |
| **Factory Method y Singleton** | `UserRepositoryFactory`, `PasswordHasherFactory`, `PasswordPolicyFactory` | El módulo de usuarios entrega su repositorio, su cifrado y su política de contraseñas por defecto a través de una fábrica única, sin que el resto conozca las clases concretas. |
| **Template Method** | `BaseQuestionPlugin.generate` (final) y `construirEnunciado` (abstracto) | Todos los plugins de generación de preguntas validan y construyen la pregunta igual; solo cambia cómo se arma el enunciado. |
| **Adapter** (puertos y adaptadores) | `QuestionJpaAdapter` (JPA); `DirectorioRevisoresDeUsuarios`; `QuestionImplRepository` | Un adaptador conecta una tecnología o módulo externo con la interfaz que espera el dominio: JPA con `QuestionRepository`, y el módulo de usuarios con `DirectorioRevisores`. |
| **Repository** | `QuestionRepository`, `SimulacroRepository`, `AsignacionRevisionRepository`, `IUserRepository` | El dominio pide y guarda entidades sin saber si están en memoria, en SQLite o en una base JPA. |
| **Pipes and Filters** (arquitectónico) | `QuestionPipeline` y cuatro filtros de validación | Las solicitudes de generación de preguntas pasan por filtros encadenados; cada filtro valida una cosa (Taller 5). |
| **Microkernel** (arquitectónico) | `QuestionMicrokernel` y los plugins de `plugins.properties` | Nuevos tipos de pregunta se agregan como plugins cargados por reflexión, sin tocar el núcleo (Taller 5). |
| **Máquina de estados** | `EstadoPregunta.puedePasarA` | Las transiciones válidas del ciclo de vida (RF-14, RF-15) viven en un solo lugar y `QuestionService` las aplica. |

## 2. Principios SOLID

| Principio | Cómo se aplica |
|---|---|
| **S** — Responsabilidad única | Cada regla de validación es una clase; `QuestionValidator` solo ejecuta reglas; `QuestionService` coordina el caso de uso; la vista solo pinta y el controlador solo decide. |
| **O** — Abierto/cerrado | Agregar una regla de validación, un plugin, un menú de un rol nuevo o un notificador no modifica las clases que los usan (ver el escenario de modificabilidad). |
| **L** — Sustitución de Liskov | `QuestionImplRepository` (memoria) y `QuestionJpaAdapter` (JPA) son intercambiables donde se espera un `QuestionRepository`; las pruebas del servicio usan dobles y las del microservicio usan JPA. |
| **I** — Segregación de interfaces | Interfaces pequeñas y específicas: `NotificadorAsignacion` (un método), `DirectorioRevisores`, `FuenteDePreguntasParaRevisar`, `MisPreguntasVista`; ningún cliente depende de métodos que no usa. |
| **D** — Inversión de dependencias | Los servicios dependen de interfaces (`QuestionRepository`, `NotificadorAsignacion`, `DirectorioRevisores`) y no de implementaciones; `MainApp` es el único lugar que las une. |

## 3. Decisiones de diseño

| # | Decisión | Motivo |
|---|---|---|
| D1 | Monolito modular con módulos Maven | La docente pidió una arquitectura que permita migrar a microservicios: los límites entre módulos ya están declarados y el grafo de dependencias no tiene ciclos. |
| D2 | La pregunta tiene **cuatro opciones (A a D)**, una de ellas correcta | Es el formato de las preguntas Saber Pro. Las opciones incorrectas son los distractores. |
| D3 | La validación estructural vive fuera de la entidad, en `QuestionValidator` | La entidad solo protege sus invariantes mínimas; las reglas de negocio se reúnen en un validador que devuelve **todas** las violaciones a la vez y que reutilizan la aplicación de escritorio y el servicio REST. |
| D4 | Solo el autor puede modificar su pregunta y solo mientras esté en Borrador | Cumple RF-06 y evita que una pregunta en revisión cambie mientras alguien la evalúa. |
| D5 | El repositorio de preguntas es en memoria detrás de una interfaz | Permite avanzar con la lógica de negocio; cambiar a una base de datos es reemplazar la implementación (ya se hizo en el Taller 6 con JPA). |
| D6 | Las vistas se acceden desde el controlador por una interfaz | Permite probar la lógica de las ventanas sin abrir ninguna ventana. |
| D7 | El correo de asignación de revisores es simulado | El proyecto permite simular las integraciones externas; el servicio depende de `NotificadorAsignacion`, de modo que se puede cambiar por SMTP sin tocarlo. |
| D8 | `MainApp` arma las dependencias a mano | Con pocos módulos no se justifica un framework de inyección; el composition root deja explícito cómo se conecta todo. |
| D9 | La búsqueda y la paginación del listado se hacen en el servicio de dominio | Con repositorio en memoria es lo más simple; con una base de datos se traslada a la consulta sin cambiar el controlador ni la vista. |
| D10 | Los estados de una pregunta se muestran con color en toda la aplicación | Lo pide la historia HU-02 y ayuda a reconocer el estado de un vistazo (`EstadoColores` es la única fuente de la paleta). |
