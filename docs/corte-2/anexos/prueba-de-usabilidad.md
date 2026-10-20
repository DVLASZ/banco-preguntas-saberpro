# Prueba de usabilidad (pensando en voz alta)

Guion y plantillas para aplicar la prueba de usabilidad del proyecto. El enunciado la pide como parte de los
prototipos y respalda el requisito **RNF-03** (un usuario nuevo aprende a usar el sistema en 30 minutos o menos). Este
documento es el material de la prueba; los **resultados** se registran en la sección «Resultados» de
[06 · Prototipos](../06-prototipos.md) una vez aplicada.

> Estado: material listo, prueba **pendiente de aplicar**. Debe hacerla alguien del equipo con personas que no conozcan el
> sistema; los datos que se registren aquí deben ser reales.

## 1. Objetivo

Comprobar que personas que no conocen el sistema pueden completar sin ayuda las tareas principales de cada rol, y detectar
dónde dudan, se equivocan o no entienden los mensajes, para corregirlo antes de la sustentación.

Preguntas que debe responder la prueba:

1. ¿Pueden crear y enviar una pregunta sin ayuda y entienden los errores de la validación estructural?
2. ¿Entienden los estados de la pregunta (colores y nombres)?
3. ¿El administrador encuentra cómo asignar revisores y entiende el límite de 1 a 3?
4. ¿El revisor entiende que su evaluación es definitiva y cómo dejar una observación?
5. ¿El autor encuentra las observaciones y sabe cómo corregir una pregunta rechazada?
6. ¿El estudiante crea y responde un simulacro y entiende su progreso? ¿El docente encuentra al estudiante que necesita apoyo?
7. ¿Cuánto tarda un usuario nuevo en completar su flujo (meta: menos de 30 minutos en total)?

## 2. Método

Prueba **pensando en voz alta** (*thinking aloud*): la persona realiza las tareas en la aplicación real y dice en voz alta lo
que piensa, lo que busca y lo que no entiende. El moderador **no ayuda**: solo recuerda que hable en voz alta y toma notas.
Si la persona se bloquea más de 3 minutos, se anota «no completó» y se le indica la salida para seguir con la tarea
siguiente.

| Elemento | Detalle |
|---|---|
| Participantes | Entre 3 y 5 personas que no hayan visto el sistema. Idealmente: 2 con perfil de autor o docente, 1 o 2 con perfil de revisor y 1 sin experiencia en el tema |
| Duración | 25 a 30 minutos por persona |
| Lugar | Un equipo con el sistema levantado (ver «Preparación») y la grabación de pantalla y audio si la persona lo autoriza |
| Roles del equipo | **Moderador** (habla con la persona) y **observador** (toma notas y tiempos; puede ser la misma persona si no hay otra) |
| Qué se mide | Éxito por tarea, tiempo, errores, comentarios y puntaje SUS |

## 3. Preparación

1. Levantar la pila: `cd microservicios` y `docker compose up -d --build`; esperar a que los contenedores estén *healthy*.
2. Abrir <http://localhost:4200>. Las cuentas de prueba están en [datos semilla](datos-semilla.md) (`autor1`, `revisor1`,
   `revisor2`, `admin`); la clave de desarrollo está en el archivo del realm.
3. Para que cada persona parta del mismo punto, reiniciar los datos entre sesiones con `docker compose down -v` seguido de
   `docker compose up -d`, o usar una cuenta de autor distinta por persona (`autor1`, `autor2`).
4. Imprimir esta guía, las tareas (sección 5) y una hoja de observación (sección 6) por participante. Tener a mano el
   cuestionario SUS (sección 7).
5. Tener lista una pregunta de ejemplo para copiar (la persona no debe inventar el contenido):

   - **Nombre:** Idea principal de un texto
   - **Contexto:** La energía solar se ha vuelto una alternativa limpia y cada vez más barata frente a los combustibles fósiles.
   - **Pregunta:** ¿Cuál es la idea principal del texto?
   - **Opciones:** A. La energía solar es costosa · B. La energía solar es una alternativa limpia y económica · C. Los combustibles fósiles son renovables · D. La energía solar solo sirve de día
   - **Respuesta correcta:** B · **Competencia:** Lectura crítica · **Tema:** Idea principal · **Subtema:** Textos argumentativos · **Dificultad:** Básica
   - **Justificación:** El texto presenta la energía solar como alternativa limpia y barata. · **Bibliografía:** Texto de ejemplo, 2026

## 4. Guion del moderador

**Antes (2 minutos).**

> «Gracias por ayudarnos. Estamos probando un sistema para gestionar un banco de preguntas de las pruebas Saber Pro. Lo que
> evaluamos es el sistema, **no a usted**: si algo no se entiende, el problema es nuestro. Le voy a pedir que haga algunas
> tareas y que vaya diciendo en voz alta lo que piensa: qué busca, qué espera que pase, qué no entiende. No le puedo ayudar
> mientras hace la tarea, pero puede preguntar lo que quiera: lo anoto y lo hablamos al final. Si quiere parar, puede hacerlo
> en cualquier momento. Con su permiso grabo la pantalla y el audio solo para analizar la prueba; no se publicará.»

Pedir el consentimiento verbal (o con la hoja de la sección 8) y anotar el perfil: rol, experiencia con aplicaciones web,
experiencia con bancos de preguntas.

**Durante.** Entregar una tarea a la vez, leída en voz alta. Frases permitidas: «¿Qué está pensando?», «¿Qué esperaba que
pasara?», «Siga hablando, por favor». Frases que **no** se dicen: dónde está un botón, qué significa un mensaje, «casi lo
logra».

**Después (5 minutos).** Aplicar el cuestionario SUS y hacer estas preguntas:

1. ¿Qué fue lo más fácil? ¿Qué fue lo más difícil o confuso?
2. ¿Hubo algún mensaje o palabra que no entendió?
3. ¿Cambiaría algo en el orden o el nombre de las opciones?
4. ¿Le parece que podría usar el sistema sin explicación? ¿Cuánto cree que tardaría en aprenderlo?

## 5. Tareas

Cada tarea tiene un criterio de éxito y un tiempo de referencia. Se cuentan los errores (clic en un lugar equivocado, mensaje
de error del sistema, volver atrás).

| # | Rol / sesión | Tarea que se lee a la persona | Éxito si… | Tiempo de referencia |
|---|---|---|---|---|
| T1 | Autor | «Entre al sistema con el usuario que le dé el moderador y diga en qué pantalla está y qué puede hacer» | Entra y describe las opciones del menú | 2 min |
| T2 | Autor | «Cree una pregunta nueva con los datos de esta hoja y guárdela como borrador» | La pregunta queda en Borrador | 6 min |
| T3 | Autor | «Esta vez deje vacío el contexto e intente enviarla a revisión. Diga qué le dice el sistema y corríjalo» | Entiende el error, lo corrige y la envía confirmando | 3 min |
| T4 | Autor | «Busque en su lista la pregunta que acaba de enviar y diga en qué estado está» | Ubica la pregunta y nombra el estado | 2 min |
| T5 | Administrador | «Esta pregunta está esperando revisores. Asígnele dos» | Asigna 2 revisores distintos del autor | 3 min |
| T6 | Administrador | «Intente asignarle cuatro revisores a otra pregunta y cuénteme qué pasa» | Entiende que el máximo es 3 | 2 min |
| T7 | Revisor | «Encuentre la pregunta que le asignaron, léala y recházela dejando una observación» | Rechaza con observación y entiende que es definitiva | 4 min |
| T8 | Autor | «Su pregunta fue rechazada. Averigüe por qué, corríjala y envíela otra vez» | Lee la observación, reabre, corrige y reenvía | 5 min |
| T9 | Cualquiera | «Revise sus notificaciones y diga cuál le avisa de lo que acaba de pasar» | Encuentra la notificación correspondiente | 2 min |
| T10 | Administrador | «Publique una pregunta aprobada» (con una aprobada preparada de antemano) | La pregunta queda Publicada | 2 min |
| T11 | Estudiante | «Entre como estudiante y cree un simulacro con 5 preguntas» | Crea el simulacro y ve la primera pregunta | 3 min |
| T12 | Estudiante | «Responda el simulacro, entréguelo y diga cuántas acertó y cuánto tardó» | Entrega y lee el puntaje y el tiempo | 4 min |
| T13 | Estudiante | «Vaya a su progreso y diga en qué competencia le va mejor y en cuál peor» | Identifica fortalezas y debilidades | 3 min |
| T14 | Docente | «Entre como docente, abra el reporte del grupo y diga qué estudiante necesita más apoyo» | Ubica al estudiante con menor desempeño y abre su detalle | 4 min |

Cada participante hace las tareas de **su rol**; si hay pocas personas, una persona puede hacer más de un rol, en ese
orden. El flujo completo de T1 a T10 debe poder recorrerse con 3 personas (autor, administrador y revisor); T11 a T13 las hace una persona
con rol de estudiante (`estudiante1`) y T14 una con rol de docente (`docente1`). Antes de T11 debe haber preguntas publicadas.

## 6. Hoja de observación (una por participante)

**Participante:** ______ **Fecha:** ______ **Rol de la sesión:** ______ **Experiencia web (1 a 5):** ___ **Modera / observa:** ______

| Tarea | ¿Completó? (sí / con ayuda / no) | Tiempo (min:seg) | Errores | Lo que dijo o dudó (frases textuales) | Problema detectado |
|---|---|---|---|---|---|
| T1 | | | | | |
| T2 | | | | | |
| T3 | | | | | |
| T4 | | | | | |
| T5 | | | | | |
| T6 | | | | | |
| T7 | | | | | |
| T8 | | | | | |
| T9 | | | | | |
| T10 | | | | | |
| T11 | | | | | |
| T12 | | | | | |
| T13 | | | | | |
| T14 | | | | | |

**Respuestas finales:** (lo más fácil / lo más difícil / mensajes que no entendió / sugerencias / cuánto cree que tardaría en aprender)

## 7. Cuestionario SUS (System Usability Scale)

La persona califica cada afirmación de 1 (totalmente en desacuerdo) a 5 (totalmente de acuerdo).

| # | Afirmación | 1 a 5 |
|---|---|---|
| 1 | Creo que me gustaría usar este sistema con frecuencia | |
| 2 | Encontré el sistema innecesariamente complejo | |
| 3 | Pensé que el sistema era fácil de usar | |
| 4 | Creo que necesitaría el apoyo de una persona técnica para poder usarlo | |
| 5 | Encontré que las diversas funciones del sistema estaban bien integradas | |
| 6 | Pensé que había demasiada inconsistencia en el sistema | |
| 7 | Imagino que la mayoría de las personas aprenderían a usar este sistema muy rápidamente | |
| 8 | Encontré el sistema muy incómodo de usar | |
| 9 | Me sentí muy seguro(a) usando el sistema | |
| 10 | Necesité aprender muchas cosas antes de poder usar el sistema | |

**Cómo se calcula:** a las preguntas impares (1, 3, 5, 7, 9) se les resta 1 al puntaje; a las pares (2, 4, 6, 8, 10) se les
resta el puntaje a 5. Se suman los diez valores y se multiplica por 2,5: resulta un número de 0 a 100. Referencia habitual: por
encima de 68 se considera usabilidad aceptable; por encima de 80, buena.

## 8. Consentimiento

> Acepto participar en la prueba de usabilidad del Sistema Banco de Preguntas Saber Pro de la asignatura Ingeniería de
> Software II (Universidad del Cauca). Entiendo que se evalúa el sistema y no a mí, que puedo parar cuando quiera y que mis
> datos y la grabación se usarán solo para el análisis de la prueba.
>
> Nombre: ______________ Firma o aceptación verbal: ______________ Fecha: ______

## 9. Análisis de los resultados

Después de todas las sesiones:

1. **Tabla de éxito por tarea:** porcentaje de participantes que completó cada tarea sin ayuda, tiempo promedio y número
   total de errores.
2. **Puntaje SUS** de cada persona y promedio.
3. **Lista de problemas** con severidad: *crítico* (impide completar la tarea), *grave* (la completan con mucha dificultad),
   *menor* (confunde pero no impide) y *sugerencia*. Agrupar los problemas repetidos y contar cuántas personas los
   tuvieron.
4. **Tiempo total** de aprendizaje: suma de los tiempos de las tareas de cada persona; comprobar la meta de RNF-03 (menos
   de 30 minutos).
5. **Acciones:** por cada problema crítico o grave, la corrección propuesta y la historia o tarea de Jira que la recoge.

Plantilla de la lista de problemas:

| # | Problema | Tarea | Severidad | Personas afectadas | Corrección propuesta | Jira |
|---|---|---|---|---|---|---|
| 1 | | | | | | |

Con esto se completa la sección de resultados de [06 · Prototipos](../06-prototipos.md): participantes (sin nombres si no
autorizan), tabla de éxito, puntaje SUS, problemas encontrados y cambios hechos a la interfaz a partir de la prueba.
