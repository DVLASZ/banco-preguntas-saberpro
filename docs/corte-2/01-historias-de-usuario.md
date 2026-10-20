# 01 · Historias de usuario de la segunda iteración

La iteración mantiene HU-01 a HU-03 del primer corte, ajusta HU-04 y agrega HU-05. Además se adelantó HU-06, la
historia nueva del tercer corte (el autor consulta el historial de revisiones). Las épicas son las de Jira: HE-01
(gestión del banco), HE-02 (administración del proceso de revisión) y HE-04 (evaluación por revisores).

## Resumen

| Historia | Rol | Épica | Cambio en esta iteración |
|---|---|---|---|
| HU-01 Crear pregunta | Autor | HE-01 | Se migra a `question-service` |
| HU-02 Enviar a revisión | Autor | HE-01 | Publica el evento `question.submitted` |
| HU-03 Listar preguntas | Autor | HE-01 | Se migra; mantiene filtros y paginación |
| HU-04 Asignar revisores | Administrador | HE-02 | **Ajuste:** entre 1 y 3 revisores, con notificación por correo |
| HU-05 Evaluar preguntas | Revisor | HE-04 | **Nueva** |
| HU-06 Historial de revisiones | Autor | HE-04 | **Adelantada** del tercer corte |

## HU-01 · Crear pregunta de selección múltiple

Como autor de preguntas quiero crear preguntas de selección múltiple con única respuesta, acordes al
Diseño Centrado en Evidencia, para alimentar el banco de preguntas Saber Pro.

Criterios de aceptación (sin cambios respecto al primer corte):

1. La pregunta tiene contexto, pregunta directa, cuatro distractores, respuesta correcta, justificación,
   bibliografía, competencia, tema, subtema y nivel de dificultad.
2. Al grabar se aplica la validación estructural: contexto presente, una única pregunta directa,
   exactamente cuatro distractores, una única respuesta correcta, sin "todas/ninguna de las anteriores"
   y distractores con longitud y estructura razonables.
3. Si la validación falla, no se guarda y se informan las violaciones.
4. Al guardar, la pregunta queda en estado **Borrador**.

## HU-02 · Enviar a revisión

Como autor quiero cambiar el estado de mis preguntas de "Borrador" a "Pendiente de revisión" para que el
administrador les asigne revisores. Los estados se muestran con colores.

1. Solo las preguntas en Borrador pueden enviarse.
2. El envío pide confirmación («¿Enviar la pregunta a revisión? Después de enviarla no podrás editarla.») y deja
   la pregunta en "Pendiente de revisión".
3. El cambio queda registrado en el historial de estados.
4. **Nuevo:** `lifecycle-service` publica el evento `question.submitted` para que `assignment-service` y `evaluation-service` la tengan disponible.

## HU-03 · Listar preguntas creadas

Como autor quiero listar las preguntas que he creado para verlas y editarlas.

1. El listado es paginado y se filtra por estado, competencia y tema.
2. Cada estado se muestra con su color.
3. Solo se pueden editar las preguntas en Borrador; las demás se muestran en solo lectura.
4. Sin resultados, se muestra un mensaje claro.

## HU-04 · Asignar revisores (ajustada)

Como administrador quiero asignar entre 1 y 3 revisores a las preguntas en estado "Pendiente de revisión",
para que otros docentes las revisen.

1. Solo se listan las preguntas en estado "Pendiente de revisión".
2. Se pueden asignar mínimo 1 y máximo 3 revisores; con 0 o más de 3 no se guarda y se muestra el motivo.
3. El autor de la pregunta nunca aparece entre los revisores disponibles y no se repite un revisor.
4. Al asignar, la pregunta pasa automáticamente a "En revisión".
5. Cada revisor asignado recibe una notificación en su bandeja y un correo (simulado), y el autor recibe el
   aviso de que su pregunta entró en revisión.

## HU-05 · Evaluar preguntas asignadas (nueva)

Como revisor quiero listar las preguntas que me asignó el administrador para leerlas, dejar la evaluación
y cambiar el estado a Aprobada o Rechazada.

1. El revisor ve solo las preguntas que tiene asignadas y pendientes de evaluar.
2. Puede abrir una pregunta, leerla completa y registrar su evaluación: decisión (aprobar o rechazar) y
   observaciones.
3. Las observaciones de todos los revisores de la misma pregunta son visibles entre ellos, para llegar a
   un consenso. No hay puntaje rígido; se aplica el criterio libre del comité.
4. La pregunta se aprueba solo si **todos** los revisores asignados aprueban (unanimidad). Con un solo
   rechazo la ronda se cierra y la pregunta queda en estado Rechazada (decisión de los docentes, consulta 29); las
   observaciones que lleguen después se guardan marcadas como tardías.
5. El autor puede leer las observaciones de los revisores en el detalle de su pregunta.
6. Se notifica a los implicados (bandeja y correo simulado): al revisor, su asignación; al administrador que
   asignó, cada evaluación recibida; al autor, el resultado con las observaciones; y al administrador y a los
   revisores que evaluaron, el resultado de la revisión. Detalle en
   [notificaciones y correos](anexos/notificaciones-y-correos.md).
7. Una vez enviada, la evaluación queda en el historial y no se borra físicamente.

## HU-06 · Historial de revisiones (adelantada del tercer corte)

Como autor quiero listar y ver el historial de revisiones y observaciones de mis preguntas, para sacar una versión
mejorada de la pregunta y cambiar su estado.

1. El autor ve las rondas de revisión de su pregunta con la decisión y las observaciones de cada revisor.
2. Ve la línea de tiempo de la pregunta: envíos, reaperturas, asignaciones, evaluaciones, resultados y publicación.
3. Una pregunta rechazada se puede reabrir: vuelve a Borrador con una versión nueva, se corrige y se envía de nuevo,
   lo que abre una ronda nueva con los mismos revisores.
4. Recibe una notificación por cada cambio relevante (resultado, observación tardía, publicación).
5. Nada se borra físicamente: cada ronda y cada evaluación quedan en el historial.
