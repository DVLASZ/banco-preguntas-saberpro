# Historias de usuario de la primera iteración

Las cuatro historias de usuario (HU) de esta iteración corresponden a los cuatro
requisitos funcionales de alto valor que pide el enunciado del primer corte. Cada
una tiene su historia en el tablero de Jira (Sprint 1), con sus criterios de
aceptación y sus subtareas.

| HU | Rol | Requisito del enunciado | Jira |
|---|---|---|---|
| HU-01 | Autor de preguntas | 1. Crear preguntas de selección múltiple con única respuesta | SCRUM-7 |
| HU-02 | Autor de preguntas | 2. Cambiar el estado de "Borrador" a "Pendiente de revisión" | SCRUM-8 |
| HU-03 | Autor de preguntas | 3. Listar las preguntas creadas, con paginación y filtros | SCRUM-9 |
| HU-04 | Administrador | 4. Asignar al menos un revisor a las preguntas pendientes | SCRUM-24 |

> **Nota sobre la numeración.** Las HU-01 a HU-04 son las del backlog del equipo.
> En el documento del proyecto de curso, "HU03. Validación estructural" (RF-08 a
> RF-13) es otro conjunto de requisitos: la validación estructural se aplica al
> guardar una pregunta y por eso hace parte de los criterios de HU-01 y HU-02.

En todas, una **pregunta** tiene: nombre, contexto, pregunta directa, **cuatro
opciones (A a D)** de las cuales una es la respuesta correcta, justificación de la
respuesta, bibliografía, competencia, tema, subtema y nivel de dificultad.

## HU-01 — Crear pregunta de selección múltiple

**Como** autor de preguntas **quiero** crear una pregunta de selección múltiple con
única respuesta **para** alimentar el banco de preguntas Saber Pro.

**Criterios de aceptación**

1. Con todos los campos obligatorios diligenciados (contexto, pregunta, opciones,
   respuesta correcta, justificación, competencia, tema, nivel) y clic en Guardar,
   la pregunta queda en "Borrador" con mensaje de confirmación.
2. Si falta un campo obligatorio, se resaltan los campos vacíos.
3. Si no cumple una regla estructural (sin contexto, sin las cuatro opciones, más de
   una respuesta marcada, o usa "todas/ninguna de las anteriores"), no se guarda y se
   muestra el error.
4. Al cancelar se pide confirmación antes de descartar lo escrito.

**Reglas de validación estructural (RF-08 a RF-13).** Cada una es una clase
independiente (`ValidationRule`) y el `QuestionValidator` las ejecuta todas para
mostrarle al autor lo que debe corregir de una sola vez:

| Regla | Requisito | Clase |
|---|---|---|
| Campos obligatorios | HU-01 | `CamposObligatoriosRule` |
| Debe haber contexto | RF-08 | `ContextoObligatorioRule` |
| Una única pregunta directa (un solo signo de interrogación) | RF-09 | `PreguntaDirectaUnicaRule` |
| Las cuatro opciones, no vacías ni repetidas | RF-10 | `CuatroOpcionesRule` |
| Una única respuesta correcta (A, B, C o D) | RF-11 | `RespuestaCorrectaUnicaRule` |
| Sin "todas las anteriores" ni "ninguna de las anteriores" | RF-12 | `ExpresionesProhibidasRule` |
| Longitud (3 a 250 caracteres) y estructura de las opciones | RF-13 | `LongitudYEstructuraOpcionesRule` |

## HU-02 — Cambiar el estado de "Borrador" a "Pendiente de revisión"

**Como** autor de preguntas **quiero** cambiar el estado de mis preguntas a
"Pendiente de revisión" **para** que el administrador asigne un revisor.

**Criterios de aceptación**

1. Si la pregunta está en "Borrador" y pasa la validación estructural, al hacer clic
   en Enviar a revisión el estado cambia a "Pendiente de revisión" (color amarillo en
   el listado).
2. Si la pregunta no pasa la validación estructural, no se cambia el estado y se
   indica al autor qué corregir.
3. Antes de aplicar el cambio aparece un diálogo de confirmación.
4. Al cancelar el diálogo, la pregunta sigue en "Borrador".

Los estados se muestran con color en todo el sistema: Borrador (gris), Pendiente de
revisión (amarillo), En revisión (azul), Aprobada (verde azulado), Rechazada (rojo),
Publicada (verde) y Archivada (pizarra). Las transiciones válidas entre estados
están definidas en `EstadoPregunta.puedePasarA` (RF-14 y RF-15).

## HU-03 — Listar preguntas creadas

**Como** autor de preguntas **quiero** ver el listado de las preguntas que he creado
**para** poder verlas o editarlas más adelante.

**Criterios de aceptación**

1. Con al menos una pregunta creada, al entrar a "Mis preguntas" se muestra la tabla
   paginada de a 10 (también se puede ver de a 5 o de a 20).
2. Al seleccionar estado y/o competencia, o escribir un texto (nombre, tema, subtema o
   pregunta), y dar clic en Buscar, la tabla se actualiza según los filtros.
3. Si los filtros no coinciden con ninguna pregunta, se muestra "No se encontraron
   preguntas" y la opción de limpiar filtros.
4. Solo se puede editar una pregunta si está en "Borrador"; en otros estados solo se
   puede ver.

## HU-04 — Asignar revisor a preguntas pendientes de revisión

**Como** administrador **quiero** asignar al menos un revisor a una pregunta
"Pendiente de revisión" **para** que otro docente la revise. Una vez asignados los
revisores, el sistema envía un correo para notificarlos (simulado).

**Criterios de aceptación**

1. Al entrar a "Asignación de Revisores" se muestran las preguntas "Pendiente de
   revisión" con su autor.
2. Con una pregunta y al menos un revisor seleccionado, al hacer clic en Asignar se
   guarda el revisor, la pregunta pasa a "En revisión" y se notifica (simulado).
3. Si no se marcó ningún revisor, se muestra el mensaje "Debe seleccionar al menos un
   revisor".
4. El autor de la pregunta no se ofrece como revisor (no aparece en la lista de
   revisores disponibles).

## Trazabilidad: criterio → código → prueba

Cada criterio de aceptación está cubierto por pruebas unitarias automatizadas
(módulo `modulo-preguntas`, salvo que se indique otro).

| Criterio | Dónde se cumple | Prueba que lo verifica |
|---|---|---|
| HU-01 · 1 | `QuestionService.crearBorrador`, `RedaccionPreguntaController.guardarBorrador` | `RedaccionPreguntaControllerTest.guardarUnaPreguntaNueva_valida_creaElBorradorYLoAvisa` |
| HU-01 · 2 | `CamposObligatoriosRule`; la vista resalta los campos | `ReglasDeValidacionTest` (CamposObligatorios), `RedaccionPreguntaControllerTest.guardarUnaPreguntaInvalida_...` |
| HU-01 · 3 | Las siete reglas de validación estructural | `ReglasDeValidacionTest`, `QuestionValidatorTest`, `QuestionServiceTest` |
| HU-01 · 4 | `RedaccionPreguntaController.cancelar` | `RedaccionPreguntaControllerTest.cancelarConCambios_pideConfirmacionYSiDiceQueNoConservaLoEscrito` |
| HU-02 · 1 | `QuestionService.enviarARevision`, `EstadoPregunta.puedePasarA` | `RedaccionPreguntaControllerTest.enviarARevision_conConfirmacion_...`, `EstadoPreguntaTest` |
| HU-02 · 2 | La validación se aplica antes de cambiar el estado | `RedaccionPreguntaControllerTest.enviarARevision_siNoPasaLaValidacion_...` |
| HU-02 · 3 y 4 | `RedaccionPreguntaVista.confirmarEnvio` | `RedaccionPreguntaControllerTest.enviarARevision_sinConfirmacion_laPreguntaSigueEnBorrador` |
| HU-03 · 1 | `QuestionService.buscarDelAutor`, `MisPreguntasController` (10 por página) | `BusquedaPaginadaTest`, `MisPreguntasControllerTest`, `PaginaTest` |
| HU-03 · 2 | `FiltroPreguntas` | `FiltroPreguntasTest`, `BusquedaPaginadaTest`, `MisPreguntasControllerTest.filtrar_...` |
| HU-03 · 3 | `PanelMisPreguntas` (mensaje y "Limpiar filtros") | `BusquedaPaginadaTest.sinResultados_...`, `MisPreguntasControllerTest.limpiarFiltros_...` |
| HU-03 · 4 | `RedaccionPreguntaController.mostrar` | `RedaccionPreguntaControllerTest.abrirUnaPreguntaQueNoEsBorrador_laMuestraDeSoloLectura` |
| HU-04 · 1 | `AsignacionRevisionService.preguntasPendientes` | `AsignacionRevisionServiceTest` (módulo `modulo-revision`) |
| HU-04 · 2 | `AsignacionRevisionService.asignarRevisores`, `NotificadorCorreoSimulado` | `AsignacionRevisionServiceTest`, `NotificadorCorreoSimuladoTest` |
| HU-04 · 3 | `AsignacionRevisionService.asignarRevisores` | `AsignacionRevisionServiceTest` (criterio 3) |
| HU-04 · 4 | `AsignacionRevisionService.revisoresDisponibles` | `AsignacionRevisionServiceTest` (criterio 4) |

## Definición de terminado

Una historia se considera terminada cuando: sus criterios de aceptación se cumplen
al ejecutar la aplicación, tiene pruebas unitarias en verde, sus subtareas de Jira
están finalizadas y sus cambios están en el repositorio.
