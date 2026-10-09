# Documento de Arquitectura

> Documento de entrega del primer corte, en formato Markdown para lectura
> directa en GitHub. Es la conversión fiel del documento Word entregado
> ([`Documento-Arquitectura-PrimerCorte.docx`](Documento-Arquitectura-PrimerCorte.docx)
> en esta misma carpeta); algunos elementos propios de Word (numeración
> automática de la tabla de contenido, listas de figuras/tablas con
> hipervínculos internos) no tienen equivalente en Markdown y aquí quedan
> como texto plano.

**Edward Esteban Dávila Salazar**

**Laura Isabel Sanchez Fernandez**

**Kevin Yesid Castaño Herrera**

**Proyecto 1**

**Ingeniería de Software 2**

**Profesor:**

**Wilson Libardo Pantoja Yepez**

**Universidad del Cauca**

**Facultad de Ingeniería Electrónica y Telecomunicaciones**

**Departamento de Sistemas**

**Ingenieria de Software 2**

**Popayán, 09 2026**

**Edward Esteban Dávila Salazar**

**Laura Isabel Sanchez Fernandez**

**DOCUMENTO DE ARQUITECTURA**

Proyecto 1 presentado en el curso de Ingeniería de Software 2

Estudiantes del:

Programa de Ingeniería de Sistemas

Profesor:

Wilson Libardo Pantoja Yepez

Popayán

2026

Contenido

[Lista de Figuras 2](#lista-de-figuras)

[Lista de Tablas 3](#lista-de-tablas)

[Introducción 5](#introducción)

[1\. Historias de usuario de la primera iteración 5](#1-historias-de-usuario-de-la-primera-iteración)

[HU-01 — Crear pregunta de selección múltiple 6](#hu-01-crear-pregunta-de-selección-múltiple)

[HU-02 — Cambiar el estado de "Borrador" a "Pendiente de revisión" 7](#hu-02-cambiar-el-estado-de-borrador-a-pendiente-de-revisión)

[HU-03 — Listar preguntas creadas 7](#hu-03-listar-preguntas-creadas)

[HU-04 — Asignar revisor a preguntas pendientes de revisión 8](#hu-04-asignar-revisor-a-preguntas-pendientes-de-revisión)

[Trazabilidad: criterio → código → prueba 8](#trazabilidad-criterio-código-prueba)

[Definición de terminado 10](#definición-de-terminado)

[2\. Prototipos de la interfaz y evaluación de usabilidad 10](#2-prototipos-de-la-interfaz-y-evaluación-de-usabilidad)

[2.1 Prototipos de la interfaz de usuario 10](#21-prototipos-de-la-interfaz-de-usuario)

[2.2 Pantallas implementadas 11](#22-pantallas-implementadas)

[HU-01 · Crear pregunta 11](#hu-01-crear-pregunta)

[HU-02 · Enviar a revisión 15](#hu-02-enviar-a-revisión)

[HU-03 · Mis preguntas 18](#hu-03-mis-preguntas)

[2.3 Evaluación de usabilidad 22](#23-evaluación-de-usabilidad)

[2.3.1 Evaluación heurística (heurísticas de Nielsen) 23](#231-evaluación-heurística-heurísticas-de-nielsen)

[2.3.2 Test de usabilidad con usuarios 24](#232-test-de-usabilidad-con-usuarios)

[3\. Planificación de tareas del Sprint 1 26](#3-planificación-de-tareas-del-sprint-1)

[Historias del sprint 27](#historias-del-sprint)

[4\. Atributos de calidad y escenario de modificabilidad 28](#4-atributos-de-calidad-y-escenario-de-modificabilidad)

[4.1 Atributos de calidad relevantes para esta iteración 28](#41-atributos-de-calidad-relevantes-para-esta-iteración)

[4.2 Escenario de calidad de modificabilidad 28](#42-escenario-de-calidad-de-modificabilidad)

[4.3 Otros escenarios que la arquitectura también soporta 30](#43-otros-escenarios-que-la-arquitectura-también-soporta)

[4.4 Tácticas de modificabilidad aplicadas 31](#44-tácticas-de-modificabilidad-aplicadas)

[5\. Arquitectura y diseño de software (modelo C4 y UML) 31](#5-arquitectura-y-diseño-de-software-modelo-c4-y-uml)

[5.1 Estilo arquitectónico 31](#51-estilo-arquitectónico)

[Módulos y dependencias 32](#módulos-y-dependencias)

[5.2 Modelo C4 33](#52-modelo-c4)

[Nivel 1 — Contexto 33](#nivel-1-contexto)

[Nivel 2 — Contenedores 34](#nivel-2-contenedores)

[Nivel 3 — Componentes 35](#nivel-3-componentes)

[Nivel 4 — Clases (UML) 37](#nivel-4-clases-uml)

[Dominio de preguntas y validación estructural 38](#dominio-de-preguntas-y-validación-estructural)

[MVC en las ventanas del Autor y del Revisor 39](#mvc-en-las-ventanas-del-autor-y-del-revisor)

[Asignación de revisores (HU-04) 40](#asignación-de-revisores-hu-04)

[Ciclo de vida de una pregunta (RF-14 y RF-15) 41](#ciclo-de-vida-de-una-pregunta-rf-14-y-rf-15)

[Secuencia: enviar una pregunta a revisión (HU-02) 42](#secuencia-enviar-una-pregunta-a-revisión-hu-02)

[Congruencia entre los diagramas C4 y la implementación 42](#congruencia-entre-los-diagramas-c4-y-la-implementación)

[5.3 Vista de ejecución 45](#53-vista-de-ejecución)

[5.4 Tecnologías 45](#54-tecnologías)

[6\. Patrones de diseño, principios SOLID y decisiones de diseño 45](#6-patrones-de-diseño-principios-solid-y-decisiones-de-diseño)

[6.1 Patrones de diseño implementados 45](#61-patrones-de-diseño-implementados)

[6.2 Principios SOLID 48](#62-principios-solid)

[6.3 Decisiones de diseño 49](#63-decisiones-de-diseño)

[7\. Pruebas unitarias y organización del repositorio 50](#7-pruebas-unitarias-y-organización-del-repositorio)

[7.1 Pruebas unitarias automatizadas 50](#71-pruebas-unitarias-automatizadas)

[7.2 Organización del repositorio 51](#72-organización-del-repositorio)

[7.3 Trabajo en equipo 52](#73-trabajo-en-equipo)

[8\. Enlaces 52](#8-enlaces)

# Lista de Figuras

[Figura 1 Formulario con todos los campos de la pregunta (criterio 1). 11](#figura-1)

[Figura 2 Al guardar una pregunta que incumple una regla, no se guarda y se explica qué corregir (criterio 3). 12](#figura-2)

[Figura 3 Los campos que incumplen quedan resaltados en rojo (criterio 2). 13](#figura-3)

[Figura 4 Cancelar con cambios sin guardar pide confirmación (criterio 4). 14](#figura-4)

[Figura 5 Antes de cambiar el estado aparece un diálogo de confirmación (criterio 3); si se elige "No", la pregunta sigue en Borrador (criterio 4). 15](#figura-5)

[Figura 6 Confirmación de que la pregunta quedó "Pendiente de revisión". 16](#figura-6)

[Figura 7 La pregunta aparece en amarillo en el listado y el formulario queda de solo lectura (criterio 1). 17](#figura-7)

[Figura 8 Tabla paginada de a 10, con el estado de cada pregunta en color (criterio 1). 18](#figura-8)

[Figura 9 También se puede ver de a 5 o de a 20; aquí, la página 2 de 2. 19](#figura-9)

[Figura 10 Filtro por estado "Borrador" (criterio 2). 20](#figura-10)

[Figura 11 Sin coincidencias: mensaje "No se encontraron preguntas" y botón "Limpiar filtros" (criterio 3). 21](#figura-11)

[Figura 12 Una pregunta que no está en Borrador se abre de solo lectura (criterio 4). 22](#figura-12)

[Figura 13 Tablero SCRUM Sprint 1 en Jira, con todas las historias y subtareas en "Finalizada". 27](#figura-13)

[Figura 14 Esbozo de una regla de validación nueva (1 clase) y su registro en QuestionValidator. 29](#figura-14)

[Figura 15 Módulos del proyecto y sus dependencias. 32](#figura-15)

[Figura 16 C4-1. Contexto del sistema y sus cinco tipos de usuario. 33](#figura-16)

[Figura 17 C4-2. Contenedores del sistema. 34](#figura-17)

[Figura 18 C4-3. Componentes de la aplicación monolítica en sus capas. 35](#figura-18)

[Figura 19 C4-4. Diagrama de clases de diseño de la gestión de preguntas. 37](#figura-19)

[Figura 20 C4-5. Diagrama de clases: dominio de preguntas y validación estructural. 38](#figura-20)

[Figura 21 C4-6. Diagrama de clases: MVC en las ventanas del Autor y del Revisor. 39](#figura-21)

[Figura 22 C4-7. Diagrama de clases: asignación de revisores (HU-04). 40](#figura-22)

[Figura 23 C4-8. Diagrama de estados: ciclo de vida de una pregunta (RF-14 y RF-15). 41](#figura-23)

[Figura 24 C4-9. Diagrama de secuencia: enviar una pregunta a revisión (HU-02). 42](#figura-24)

# Lista de Tablas

[Tabla 1 Historias de usuario del primer corte y su historia en Jira. 5](#tabla-1)

[Tabla 2 Reglas de validación estructural (RF-08 a RF-13). 6](#tabla-2)

[Tabla 3 Trazabilidad de los criterios de aceptación al código y las pruebas. 8](#tabla-3)

[Tabla 4 Prototipos base en Figma (Taller 4). 10](#tabla-4)

[Tabla 5 Prototipos detallados de la iteración en Figma. 10](#tabla-5)

[Tabla 6 Heurísticas de Nielsen que cumple la aplicación. 23](#tabla-6)

[Tabla 7 Problemas encontrados en la evaluación heurística y mejoras propuestas. 23](#tabla-7)

[Tabla 8 Tareas del test de usabilidad con usuarios. 24](#tabla-8)

[Tabla 9 Resultados del test de usabilidad y puntaje SUS. 25](#tabla-9)

[Tabla 10 Datos generales del Sprint 1. 26](#tabla-10)

[Tabla 11 Historias del Sprint 1, puntos y responsables. 27](#tabla-11)

[Tabla 12 Atributos de calidad relevantes de la iteración. 28](#tabla-12)

[Tabla 13 Escenario de calidad de modificabilidad. 28](#tabla-13)

[Tabla 14 Otros escenarios de modificabilidad soportados por la arquitectura. 30](#tabla-14)

[Tabla 15 Tácticas de modificabilidad aplicadas. 31](#tabla-15)

[Tabla 16 Capas de cada módulo y su responsabilidad. 32](#tabla-16)

[Tabla 17 Módulos del proyecto, su contenido e historias asociadas. 32](#tabla-17)

[Tabla 18 Personas que usan el sistema (nivel 1 de C4). 34](#tabla-18)

[Tabla 19 Correspondencia entre los componentes del diagrama y las clases implementadas. 35](#tabla-19)

[Tabla 20 Congruencia entre los diagramas C4 y la implementación. 42](#tabla-20)

[Tabla 21 Ventanas de la aplicación por rol. 45](#tabla-21)

[Tabla 22 Patrones de diseño implementados. 45](#tabla-22)

[Tabla 23 Aplicación de los principios SOLID. 48](#tabla-23)

[Tabla 24 Decisiones de diseño y su motivo. 49](#tabla-24)

[Tabla 25 Pruebas unitarias automatizadas por módulo. 50](#tabla-25)

[Tabla 26 Enlaces del proyecto (video, repositorio, Jira, Figma, C4). 52](#tabla-26)

# Introducción

Este documento describe la arquitectura y el diseño de la primera iteración del Sistema de Banco de Preguntas Saber Pro, una aplicación de escritorio en Java (Swing) que permite redactar, revisar y publicar preguntas para la preparación de las pruebas Saber Pro, y generar simulacros a partir de las preguntas publicadas.

La primera iteración implementa cuatro historias de usuario de alto valor: crear una pregunta con validación estructural, enviarla a revisión, listar las preguntas creadas con paginación y filtros, y asignar revisores a las preguntas pendientes. El sistema sigue una arquitectura monolítica en tres capas, organizada en módulos, con el micropatrón MVC, principios de diseño SOLID y patrones de diseño GoF para garantizar su modificabilidad.

El documento presenta, en este orden: las historias de usuario con sus criterios de aceptación, los prototipos de la interfaz y la evaluación de usabilidad, la planificación del Sprint 1, el escenario de calidad de modificabilidad, la arquitectura con el modelo C4 y UML, los patrones de diseño y las decisiones tomadas, y las pruebas y el repositorio. Al final se incluyen los enlaces al video y al repositorio.

# 1\. Historias de usuario de la primera iteración

Las cuatro historias de usuario (HU) de esta iteración corresponden a los cuatro requisitos funcionales de alto valor que pide el enunciado del primer corte. Cada una tiene su historia en el tablero de Jira (Sprint 1), con sus criterios de aceptación y sus subtareas.

<a id="tabla-1"></a>
Tabla 1 Historias de usuario del primer corte y su historia en Jira.

<table><tbody><tr><td><p><strong>HU</strong></p></td><td><p><strong>Rol</strong></p></td><td><p><strong>Requisito del enunciado</strong></p></td><td><p><strong>Jira</strong></p></td></tr><tr><td><p>HU-01</p></td><td><p>Autor de preguntas</p></td><td><p>1. Crear preguntas de selección múltiple con única respuesta</p></td><td><p>SCRUM-7</p></td></tr><tr><td><p>HU-02</p></td><td><p>Autor de preguntas</p></td><td><p>2. Cambiar el estado de "Borrador" a "Pendiente de revisión"</p></td><td><p>SCRUM-8</p></td></tr><tr><td><p>HU-03</p></td><td><p>Autor de preguntas</p></td><td><p>3. Listar las preguntas creadas, con paginación y filtros</p></td><td><p>SCRUM-9</p></td></tr><tr><td><p>HU-04</p></td><td><p>Administrador</p></td><td><p>4. Asignar al menos un revisor a las preguntas pendientes</p></td><td><p>SCRUM-24</p></td></tr></tbody></table>

Nota sobre la numeración. Las HU-01 a HU-04 son las del backlog del equipo. En el documento del proyecto de curso, "HU03. Validación estructural" (RF-08 a RF-13) es otro conjunto de requisitos: la validación estructural se aplica al guardar una pregunta y por eso hace parte de los criterios de HU-01 y HU-02.

En todas, una pregunta tiene: nombre, contexto, pregunta directa, cuatro opciones (A a D) de las cuales una es la respuesta correcta, justificación de la respuesta, bibliografía, competencia, tema, subtema y nivel de dificultad.

## HU-01 — Crear pregunta de selección múltiple

Como autor de preguntas quiero crear una pregunta de selección múltiple con única respuesta para alimentar el banco de preguntas Saber Pro.

Criterios de aceptación

1.  Con todos los campos obligatorios diligenciados (contexto, pregunta, opciones, respuesta correcta, justificación, competencia, tema, nivel) y clic en Guardar, la pregunta queda en "Borrador" con mensaje de confirmación.
2.  Si falta un campo obligatorio, se resaltan los campos vacíos.
3.  Si no cumple una regla estructural (sin contexto, sin las cuatro opciones, más de una respuesta marcada, o usa "todas/ninguna de las anteriores"), no se guarda y se muestra el error.
4.  Al cancelar se pide confirmación antes de descartar lo escrito.

Reglas de validación estructural (RF-08 a RF-13). Cada una es una clase independiente (ValidationRule) y el QuestionValidator las ejecuta todas para mostrarle al autor lo que debe corregir de una sola vez:

<a id="tabla-2"></a>
Tabla 2 Reglas de validación estructural (RF-08 a RF-13).

<table><tbody><tr><td><p><strong>Regla</strong></p></td><td><p><strong>Requisito</strong></p></td><td><p><strong>Clase</strong></p></td></tr><tr><td><p>Campos obligatorios</p></td><td><p>HU-01</p></td><td><p>CamposObligatoriosRule</p></td></tr><tr><td><p>Debe haber contexto</p></td><td><p>RF-08</p></td><td><p>ContextoObligatorioRule</p></td></tr><tr><td><p>Una única pregunta directa (un solo signo de interrogación)</p></td><td><p>RF-09</p></td><td><p>PreguntaDirectaUnicaRule</p></td></tr><tr><td><p>Las cuatro opciones, no vacías ni repetidas</p></td><td><p>RF-10</p></td><td><p>CuatroOpcionesRule</p></td></tr><tr><td><p>Una única respuesta correcta (A, B, C o D)</p></td><td><p>RF-11</p></td><td><p>RespuestaCorrectaUnicaRule</p></td></tr><tr><td><p>Sin "todas las anteriores" ni "ninguna de las anteriores"</p></td><td><p>RF-12</p></td><td><p>ExpresionesProhibidasRule</p></td></tr><tr><td><p>Longitud (3 a 250 caracteres) y estructura de las opciones</p></td><td><p>RF-13</p></td><td><p>LongitudYEstructuraOpcionesRule</p></td></tr></tbody></table>

## HU-02 — Cambiar el estado de "Borrador" a "Pendiente de revisión"

Como autor de preguntas quiero cambiar el estado de mis preguntas a "Pendiente de revisión" para que el administrador asigne un revisor.

Criterios de aceptación

1.  Si la pregunta está en "Borrador" y pasa la validación estructural, al hacer clic en Enviar a revisión el estado cambia a "Pendiente de revisión" (color amarillo en el listado).
2.  Si la pregunta no pasa la validación estructural, no se cambia el estado y se indica al autor qué corregir.
3.  Antes de aplicar el cambio aparece un diálogo de confirmación.
4.  Al cancelar el diálogo, la pregunta sigue en "Borrador".

Los estados se muestran con color en todo el sistema: Borrador (gris), Pendiente de revisión (amarillo), En revisión (azul), Aprobada (verde azulado), Rechazada (rojo), Publicada (verde) y Archivada (pizarra). Las transiciones válidas entre estados están definidas en EstadoPregunta.puedePasarA (RF-14 y RF-15).

## HU-03 — Listar preguntas creadas

Como autor de preguntas quiero ver el listado de las preguntas que he creado para poder verlas o editarlas más adelante.

Criterios de aceptación

1.  Con al menos una pregunta creada, al entrar a "Mis preguntas" se muestra la tabla paginada de a 10 (también se puede ver de a 5 o de a 20).
2.  Al seleccionar estado y/o competencia, o escribir un texto (nombre, tema, subtema o pregunta), y dar clic en Buscar, la tabla se actualiza según los filtros.
3.  Si los filtros no coinciden con ninguna pregunta, se muestra "No se encontraron preguntas" y la opción de limpiar filtros.
4.  Solo se puede editar una pregunta si está en "Borrador"; en otros estados solo se puede ver.

## HU-04 — Asignar revisor a preguntas pendientes de revisión

Como administrador quiero asignar al menos un revisor a una pregunta "Pendiente de revisión" para que otro docente la revise. Una vez asignados los revisores, el sistema envía un correo para notificarlos (simulado).

Criterios de aceptación

1.  Al entrar a "Asignación de Revisores" se muestran las preguntas "Pendiente de revisión" con su autor.
2.  Con una pregunta y al menos un revisor seleccionado, al hacer clic en Asignar se guarda el revisor, la pregunta pasa a "En revisión" y se notifica (simulado).
3.  Si no se marcó ningún revisor, se muestra el mensaje "Debe seleccionar al menos un revisor".
4.  El autor de la pregunta no se ofrece como revisor (no aparece en la lista de revisores disponibles).

## Trazabilidad: criterio → código → prueba

Cada criterio de aceptación está cubierto por pruebas unitarias automatizadas (módulo modulo-preguntas, salvo que se indique otro).

<a id="tabla-3"></a>
Tabla 3 Trazabilidad de los criterios de aceptación al código y las pruebas.

<table><tbody><tr><td><p><strong>Criterio</strong></p></td><td><p><strong>Dónde se cumple</strong></p></td><td><p><strong>Prueba que lo verifica</strong></p></td></tr><tr><td><p>HU-01 · 1</p></td><td><p>QuestionService.crearBorrador</p><p>,</p><p>RedaccionPreguntaController.guardarBorrador</p></td><td><p>RedaccionPreguntaControllerTest.guardarUnaPreguntaNueva_valida_creaElBorradorYLoAvisa</p></td></tr><tr><td><p>HU-01 · 2</p></td><td><p>CamposObligatoriosRule</p><p>; la vista resalta los campos</p></td><td><p>ReglasDeValidacionTest</p><p>(CamposObligatorios),</p><p>RedaccionPreguntaControllerTest.guardarUnaPreguntaInvalida_...</p></td></tr><tr><td><p>HU-01 · 3</p></td><td><p>Las siete reglas de validación estructural</p></td><td><p>ReglasDeValidacionTest</p><p>,</p><p>QuestionValidatorTest</p><p>,</p><p>QuestionServiceTest</p></td></tr><tr><td><p>HU-01 · 4</p></td><td><p>RedaccionPreguntaController.cancelar</p></td><td><p>RedaccionPreguntaControllerTest.cancelarConCambios_pideConfirmacionYSiDiceQueNoConservaLoEscrito</p></td></tr><tr><td><p>HU-02 · 1</p></td><td><p>QuestionService.enviarARevision</p><p>,</p><p>EstadoPregunta.puedePasarA</p></td><td><p>RedaccionPreguntaControllerTest.enviarARevision_conConfirmacion_...</p><p>,</p><p>EstadoPreguntaTest</p></td></tr><tr><td><p>HU-02 · 2</p></td><td><p>La validación se aplica antes de cambiar el estado</p></td><td><p>RedaccionPreguntaControllerTest.enviarARevision_siNoPasaLaValidacion_...</p></td></tr><tr><td><p>HU-02 · 3 y 4</p></td><td><p>RedaccionPreguntaVista.confirmarEnvio</p></td><td><p>RedaccionPreguntaControllerTest.enviarARevision_sinConfirmacion_laPreguntaSigueEnBorrador</p></td></tr><tr><td><p>HU-03 · 1</p></td><td><p>QuestionService.buscarDelAutor</p><p>,</p><p>MisPreguntasController</p><p>(10 por página)</p></td><td><p>BusquedaPaginadaTest</p><p>,</p><p>MisPreguntasControllerTest</p><p>,</p><p>PaginaTest</p></td></tr><tr><td><p>HU-03 · 2</p></td><td><p>FiltroPreguntas</p></td><td><p>FiltroPreguntasTest</p><p>,</p><p>BusquedaPaginadaTest</p><p>,</p><p>MisPreguntasControllerTest.filtrar_...</p></td></tr><tr><td><p>HU-03 · 3</p></td><td><p>PanelMisPreguntas</p><p>(mensaje y "Limpiar filtros")</p></td><td><p>BusquedaPaginadaTest.sinResultados_...</p><p>,</p><p>MisPreguntasControllerTest.limpiarFiltros_...</p></td></tr><tr><td><p>HU-03 · 4</p></td><td><p>RedaccionPreguntaController.mostrar</p></td><td><p>RedaccionPreguntaControllerTest.abrirUnaPreguntaQueNoEsBorrador_laMuestraDeSoloLectura</p></td></tr><tr><td><p>HU-04 · 1</p></td><td><p>AsignacionRevisionService.preguntasPendientes</p></td><td><p>AsignacionRevisionServiceTest</p><p>(módulo</p><p>modulo-revision</p><p>)</p></td></tr><tr><td><p>HU-04 · 2</p></td><td><p>AsignacionRevisionService.asignarRevisores</p><p>,</p><p>NotificadorCorreoSimulado</p></td><td><p>AsignacionRevisionServiceTest</p><p>,</p><p>NotificadorCorreoSimuladoTest</p></td></tr><tr><td><p>HU-04 · 3</p></td><td><p>AsignacionRevisionService.asignarRevisores</p></td><td><p>AsignacionRevisionServiceTest</p><p>(criterio 3)</p></td></tr><tr><td><p>HU-04 · 4</p></td><td><p>AsignacionRevisionService.revisoresDisponibles</p></td><td><p>AsignacionRevisionServiceTest</p><p>(criterio 4)</p></td></tr></tbody></table>

## Definición de terminado

Una historia se considera terminada cuando: sus criterios de aceptación se cumplen al ejecutar la aplicación, tiene pruebas unitarias en verde, sus subtareas de Jira están finalizadas y sus cambios están en el repositorio.

# 2\. Prototipos de la interfaz y evaluación de usabilidad

## 2.1 Prototipos de la interfaz de usuario

Los prototipos están en Figma, en el archivo Banco de Preguntas Saber Pro - Prototipos.

<a id="tabla-4"></a>
Tabla 4 Prototipos base en Figma (Taller 4).

<table><tbody><tr><td><p><strong>Prototipo (marco de Figma)</strong></p></td><td><p><strong>Página del archivo</strong></p></td><td><p><strong>Historia de usuario</strong></p></td></tr><tr><td><p>Autor de Preguntas - Formulario</p></td><td><p>Page 1</p></td><td><p>Base de HU-01 y HU-02</p></td></tr><tr><td><p>Buscar Preguntas - Filtros</p></td><td><p>Page 1</p></td><td><p>Base de HU-03</p></td></tr><tr><td><p>Revisor - Revisar Pregunta</p></td><td><p>Page 1</p></td><td><p>Flujo posterior a HU-04</p></td></tr><tr><td><p>Login - Inicio de Sesion, Registro - Registro de Usuario, Tablero generico</p></td><td><p>Page 1</p></td><td><p>Acceso al sistema</p></td></tr><tr><td><p>Vista de Estadisticas, Vista Grafica</p></td><td><p>Page 1</p></td><td><p>Reporte del Administrador (Observer)</p></td></tr></tbody></table>

Los prototipos detallados de las cuatro historias de la iteración, ya alineados con la interfaz implementada, están en la página "Primer corte - Prototipos HU-01 a HU-04" del mismo archivo:

<a id="tabla-5"></a>
Tabla 5 Prototipos detallados de la iteración en Figma.

<table><tbody><tr><td><p><strong>Prototipo (marco de Figma)</strong></p></td><td><p><strong>Historia de usuario</strong></p></td></tr><tr><td><p>HU-01 · Crear pregunta (formulario completo)</p></td><td><p>HU-01 (criterio 1)</p></td></tr><tr><td><p>HU-01 · Validación estructural: campos en rojo (criterios 2 y 3)</p></td><td><p>HU-01 (criterios 2 y 3)</p></td></tr><tr><td><p>HU-03 · Mis preguntas (listado, filtros y paginación)</p></td><td><p>HU-03</p></td></tr><tr><td><p>HU-02 · Enviar a revisión (confirmación)</p></td><td><p>HU-02</p></td></tr><tr><td><p>Revisor · Revisar pregunta (Aprobar/Rechazar)</p></td><td><p>Flujo posterior a HU-04</p></td></tr><tr><td><p>HU-04 · Asignación de Revisores (Administrador)</p></td><td><p>HU-04 (los 4 criterios)</p></td></tr><tr><td><p>Login · Inicio de sesión, Registro · Registro de usuario</p></td><td><p>Acceso al sistema</p></td></tr><tr><td><p>Dashboard · Tablero (Administrador)</p></td><td><p>Acceso a HU-04 y a los reportes desde el menú</p></td></tr><tr><td><p>Observer1 · Vista de Estadísticas, Observer2 · Vista Gráfica</p></td><td><p>Reporte del Administrador (Observer)</p></td></tr><tr><td><p>Microkernel · Generador de preguntas, Docente · Generar Simulacro, Estudiante · Elegir/Presentar simulacro</p></td><td><p>Fuera del primer corte (Talleres 5 y 6)</p></td></tr></tbody></table>

## 2.2 Pantallas implementadas

Capturas de la aplicación en ejecución (usuario autor1).

### HU-01 · Crear pregunta

<a id="figura-1"></a>
![](img/hu01-formulario.png)

Figura 1 Formulario con todos los campos de la pregunta (criterio 1).

<a id="figura-2"></a>
![](img/hu01-validacion-dialogo.png)

Figura 2 Al guardar una pregunta que incumple una regla, no se guarda y se explica qué corregir (criterio 3).

<a id="figura-3"></a>
![](img/hu01-validacion-campos-en-rojo.png)

Figura 3 Los campos que incumplen quedan resaltados en rojo (criterio 2).

<a id="figura-4"></a>
![](img/hu01-cancelar-confirmacion.png)

Figura 4 Cancelar con cambios sin guardar pide confirmación (criterio 4).

### HU-02 · Enviar a revisión

<a id="figura-5"></a>
![](img/hu02-confirmacion.png)

Figura 5 Antes de cambiar el estado aparece un diálogo de confirmación (criterio 3); si se elige "No", la pregunta sigue en Borrador (criterio 4).

<a id="figura-6"></a>
![](img/hu02-enviada.png)

Figura 6 Confirmación de que la pregunta quedó "Pendiente de revisión".

<a id="figura-7"></a>
![](img/hu02-listado-pendiente.png)

Figura 7 La pregunta aparece en amarillo en el listado y el formulario queda de solo lectura (criterio 1).

### HU-03 · Mis preguntas

<a id="figura-8"></a>
![](img/hu03-listado.png)

Figura 8 Tabla paginada de a 10, con el estado de cada pregunta en color (criterio 1).

<a id="figura-9"></a>
![](img/hu03-paginacion.png)

Figura 9 También se puede ver de a 5 o de a 20; aquí, la página 2 de 2.

<a id="figura-10"></a>
![](img/hu03-filtro-borrador.png)

Figura 10 Filtro por estado "Borrador" (criterio 2).

<a id="figura-11"></a>
![](img/hu03-sin-resultados.png)

Figura 11 Sin coincidencias: mensaje "No se encontraron preguntas" y botón "Limpiar filtros" (criterio 3).

<a id="figura-12"></a>
![](img/hu03-solo-lectura.png)

Figura 12 Una pregunta que no está en Borrador se abre de solo lectura (criterio 4).

## 2.3 Evaluación de usabilidad

Se aplican dos técnicas complementarias: una evaluación heurística hecha por el equipo sobre la aplicación de esta iteración, y un test de usabilidad con usuarios con tareas y cuestionario SUS.

### 2.3.1 Evaluación heurística (heurísticas de Nielsen)

Severidad: 0 = no es un problema, 1 = cosmético, 2 = menor, 3 = mayor, 4 = catastrófico.

Lo que cumple

<a id="tabla-6"></a>
Tabla 6 Heurísticas de Nielsen que cumple la aplicación.

<table><tbody><tr><td><p><strong>Heurística</strong></p></td><td><p><strong>Evidencia en la aplicación</strong></p></td></tr><tr><td><p>1. Visibilidad del estado del sistema</p></td><td><p>El aviso al pie del formulario ("Puede modificarla mientras esté en Borrador" / "Solo lectura: la pregunta está Pendiente de revisión"), el estado en color y "Página 1 de 1 · 6 preguntas".</p></td></tr><tr><td><p>3. Control y libertad del usuario</p></td><td><p>Cancelar con confirmación (Figura 4), "Limpiar filtros", diálogo de confirmación antes de enviar a revisión (Figura 5).</p></td></tr><tr><td><p>4. Consistencia y estándares</p></td><td><p>Los mismos colores de estado en el listado, la ventana del Revisor y las vistas de estadísticas; misma apariencia en todas las ventanas.</p></td></tr><tr><td><p>5. Prevención de errores</p></td><td><p>Solo se edita en Borrador; los botones se deshabilitan según el estado; competencia, dificultad y respuesta se eligen de una lista.</p></td></tr><tr><td><p>9. Ayudar a reconocer y corregir errores</p></td><td><p>Un mensaje por cada regla incumplida y los campos afectados en rojo (Figuras 2 y 3).</p></td></tr></tbody></table>

Problemas encontrados y mejoras propuestas

<a id="tabla-7"></a>
Tabla 7 Problemas encontrados en la evaluación heurística y mejoras propuestas.

<table><tbody><tr><td><p><strong>#</strong></p></td><td><p><strong>Problema</strong></p></td><td><p><strong>Heurística</strong></p></td><td><p><strong>Severidad</strong></p></td><td><p><strong>Mejora propuesta</strong></p></td></tr><tr><td><p>P1</p></td><td><p>El nombre largo de una pregunta se corta en la tabla ("Principio de Responsabilidad Únic…")</p></td><td><p>6. Reconocer antes que recordar</p></td><td><p>1</p></td><td><p>Ensanchar la columna o mostrar el nombre completo al pasar el cursor</p></td></tr><tr><td><p>P2</p></td><td><p>Los campos del formulario no indican cuáles son obligatorios hasta guardar</p></td><td><p>5. Prevención de errores</p></td><td><p>2</p></td><td><p>Marcar los campos obligatorios con un asterisco</p></td></tr><tr><td><p>P3</p></td><td><p>El botón "Cancelar" descarta los cambios, pero su rótulo no lo dice</p></td><td><p>2. Relación con el mundo real</p></td><td><p>1</p></td><td><p>Renombrarlo "Descartar cambios"</p></td></tr><tr><td><p>P4</p></td><td><p>Con un filtro aplicado no hay un indicador de que la lista está filtrada, más allá de los controles</p></td><td><p>1. Visibilidad del estado</p></td><td><p>2</p></td><td><p>Mostrar "Filtros activos" junto al total de preguntas</p></td></tr><tr><td><p>P5</p></td><td><p>La ventana del Autor es alta (940 px) y en pantallas pequeñas obliga a desplazarse</p></td><td><p>8. Diseño minimalista</p></td><td><p>2</p></td><td><p>Reducir el alto o dividir el formulario en pestañas</p></td></tr></tbody></table>

### 2.3.2 Test de usabilidad con usuarios

Objetivo. Comprobar que un autor de preguntas puede crear, enviar a revisión y encontrar sus preguntas sin ayuda, y que un administrador puede asignar revisores.

Participantes. De 3 a 5 personas con el perfil del sistema: docentes o estudiantes de últimos semestres que no participaron en el desarrollo.

Preparación. Ejecutar la aplicación (java -jar app/target/banco-preguntas-saberpro.jar). Usuarios de prueba (contraseña Saber2026!): autor1 (Autor), admin1 (Administrador). La persona evaluadora no explica cómo se usa el sistema: solo lee la tarea y observa.

Tareas

<a id="tabla-8"></a>
Tabla 8 Tareas del test de usabilidad con usuarios.

<table><tbody><tr><td><p><strong>Tarea</strong></p></td><td><p><strong>Enunciado que se le lee al participante</strong></p></td><td><p><strong>Éxito si…</strong></p></td></tr><tr><td><p>T1</p></td><td><p>"Ingrese como</p><p>autor1</p><p>y redacte una pregunta nueva sobre un tema que usted elija."</p></td><td><p>Guarda un borrador sin ayuda</p></td></tr><tr><td><p>T2</p></td><td><p>"Envíe esa pregunta a revisión."</p></td><td><p>La pregunta queda "Pendiente de revisión"</p></td></tr><tr><td><p>T3</p></td><td><p>"Encuentre todas sus preguntas que estén en Borrador."</p></td><td><p>Usa el filtro por estado</p></td></tr><tr><td><p>T4</p></td><td><p>"Abra una pregunta ya enviada y diga si puede modificarla."</p></td><td><p>Reconoce que es de solo lectura</p></td></tr><tr><td><p>T5</p></td><td><p>"Ingrese como</p><p>admin1</p><p>y asigne un revisor a una pregunta pendiente."</p></td><td><p>Asigna al menos un revisor</p></td></tr></tbody></table>

Métricas por tarea: completó sin ayuda (sí/no), tiempo en segundos, número de errores y comentarios.

Cuestionario SUS (escala de 1 = totalmente en desacuerdo a 5 = totalmente de acuerdo):

1.  Creo que me gustaría usar este sistema con frecuencia.
2.  Encontré el sistema innecesariamente complejo.
3.  Pensé que el sistema era fácil de usar.
4.  Creo que necesitaría el apoyo de una persona técnica para usarlo.
5.  Encontré que las funciones estaban bien integradas.
6.  Pensé que había demasiada inconsistencia en el sistema.
7.  Imagino que la mayoría de las personas aprendería a usarlo muy rápido.
8.  Encontré el sistema muy incómodo de usar.
9.  Me sentí muy seguro/a usando el sistema.
10.  Necesité aprender muchas cosas antes de poder usarlo.

Cálculo del puntaje SUS. Para las preguntas impares se resta 1 a la respuesta; para las pares se resta la respuesta de 5. Se suman los diez valores y se multiplican por 2,5: el resultado va de 0 a 100 (68 se considera el promedio; por encima de 80, muy bueno).

Resultados

<a id="tabla-9"></a>
Tabla 9 Resultados del test de usabilidad y puntaje SUS.

<table><tbody><tr><td><p><strong>Participante</strong></p></td><td><p><strong>T1</strong></p></td><td><p><strong>T2</strong></p></td><td><p><strong>T3</strong></p></td><td><p><strong>T4</strong></p></td><td><p><strong>T5</strong></p></td><td><p><strong>Puntaje SUS</strong></p></td><td><p><strong>Observaciones</strong></p></td></tr><tr><td><p>P1</p></td><td><p>Sí (52s)</p></td><td><p>Sí (15s)</p></td><td><p>Sí (20s)</p></td><td><p>No (50s)</p></td><td><p>Sí (40s)</p></td><td><p>77,5</p></td><td><p>1 error en T1 (dejó la bibliografía vacía; corrigió solo al ver el aviso). En T4 pensó que la app se había bloqueado al no poder editar los campos y necesitó una pista para entender que la pregunta quedaba protegida por estar enviada. Sugirió un aviso visual de "Solo lectura" en vez de solo deshabilitar los campos.</p></td></tr><tr><td><p>P2</p></td><td><p>Sí (30s)</p></td><td><p>Sí (10s)</p></td><td><p>Sí (10s)</p></td><td><p>Sí (15s)</p></td><td><p>Sí (20s)</p></td><td><p>97,5</p></td><td><p>Completó las 5 tareas sin dudar ni pedir ayuda. Reconoció de inmediato el estado de solo lectura por el comportamiento de los campos. Sugirió agregar atajos de teclado (p. ej. Ctrl+S) para agilizar el diligenciamiento del formulario.</p></td></tr><tr><td><p>P3</p></td><td><p>Sí (80s)</p></td><td><p>Sí (25s)</p></td><td><p>Sí (35s)</p></td><td><p>Sí (30s)</p></td><td><table><tbody><tr><td></td><td><p>No (70s)</p></td></tr></tbody></table></td><td><p>62,5</p></td><td><p>En T1 dudó varias veces por la cantidad de campos del formulario. En T3 no usó el filtro por estado: bajó manualmente por la tabla hasta encontrar los borradores. En T5 no encontró el botón de cerrar sesión y necesitó una pista para cambiar de usuario. Sugirió dividir el formulario de creación en pasos y hacer más visible el botón de cerrar sesión.</p></td></tr><tr><td><p><strong>Promedio / % de éxito</strong></p></td><td><p>100%</p></td><td><p>100%</p></td><td><p>100%</p></td><td><p>67%</p></td><td><p>67%</p></td><td><p>79,2</p></td><td><p>T3 se completó siempre, pero 1 de 3 no usó el filtro previsto. T4 y T5 tuvieron 1 caso cada uno que necesitó una pista del evaluador (visibilidad del estado de solo lectura y ubicación del botón de cerrar sesión).</p></td></tr></tbody></table>

Participantes:

-   P1 = Sebastián Ruiz Segura
-   P2 = Jhoiner Alberto Puentes Figueroa
-   P3 = José David Ospina

Cómo se usan los resultados. Los problemas que aparezcan en dos o más participantes se agregan a la tabla de problemas de la evaluación heurística con su severidad y se priorizan para la siguiente iteración.

# 3\. Planificación de tareas del Sprint 1

La planificación se lleva en Jira, en el tablero SCRUM Sprint 1: [abrir el tablero.](https://dvlasz.atlassian.net/jira/software/projects/SCRUM/boards/1)

<a id="tabla-10"></a>
Tabla 10 Datos generales del Sprint 1.

<table><tbody><tr><td><p><strong>Dato</strong></p></td><td><p><strong>Valor</strong></p></td></tr><tr><td><p>Sprint</p></td><td><p>SCRUM Sprint 1</p></td></tr><tr><td><p>Inicio</p></td><td><p>17 de septiembre de 2026</p></td></tr><tr><td><p>Fin</p></td><td><p>26 de septiembre de 2026</p></td></tr><tr><td><p>Objetivo</p></td><td><p>Continuar la épica HE-01 (HU-01, HU-02, HU-03) y empezar HE-02 (HU-04) para la sustentación del primer corte</p></td></tr></tbody></table>

Pantallazo del tablero Jira, Sprint 1 (todas las historias y subtareas en estado "Finalizada" al cierre del primer corte):

<a id="figura-13"></a>
![](img/jira-sprint-1.png)

Figura 13 Tablero SCRUM Sprint 1 en Jira, con todas las historias y subtareas en "Finalizada".

## Historias del sprint

<a id="tabla-11"></a>
Tabla 11 Historias del Sprint 1, puntos y responsables.

<table><tbody><tr><td><p><strong>Historia</strong></p></td><td><p><strong>Descripción</strong></p></td><td><p><strong>Puntos</strong></p></td><td><p><strong>Responsable</strong></p></td></tr><tr><td><p>HU-01 (SCRUM-7)</p></td><td><p>Crear pregunta de selección múltiple</p></td><td><p>5</p></td><td><p>Edward Esteban Dávila Salazar</p></td></tr><tr><td><p>HU-02 (SCRUM-8)</p></td><td><p>Cambiar el estado de "Borrador" a "Pendiente de revisión"</p></td><td><p>3</p></td><td><p>Edward Esteban Dávila Salazar</p></td></tr><tr><td><p>HU-03 (SCRUM-9)</p></td><td><p>Listar preguntas creadas</p></td><td><p>5</p></td><td><p>Edward Esteban Dávila Salazar</p></td></tr><tr><td><p>HU-04 (SCRUM-24)</p></td><td><p>Asignar revisor a preguntas pendientes de revisión</p></td><td><p>5</p></td><td><p>Kevin Yesid Castaño Herrera</p></td></tr><tr><td><p>HU-T6 (SCRUM-30)</p></td><td><p>Exponer el CRUD de preguntas como microservicio REST (Taller 6)</p></td><td><p>5</p></td><td><p>Laura Isabel Sánchez Fernández</p></td></tr></tbody></table>

Cada historia se divide en subtareas técnicas (diseñar la pantalla, implementar la validación, implementar la lógica, escribir las pruebas y verificar manualmente los criterios de aceptación).

# 4\. Atributos de calidad y escenario de modificabilidad

## 4.1 Atributos de calidad relevantes para esta iteración

<a id="tabla-12"></a>
Tabla 12 Atributos de calidad relevantes de la iteración.

<table><tbody><tr><td><p><strong>Atributo</strong></p></td><td><p><strong>Por qué importa en el banco de preguntas</strong></p></td><td><p><strong>Cómo se atiende</strong></p></td></tr><tr><td><p><strong>Modificabilidad</strong></p><p>(principal)</p></td><td><p>Las reglas de validación, los estados de una pregunta y las formas de notificar cambian con el uso; además el proyecto migrará a microservicios</p></td><td><p>Módulos Maven con dependencias en una sola dirección; interfaces (puertos) entre capas; una regla de validación por clase; inyección de dependencias en un único punto (</p><p>MainApp</p><p>)</p></td></tr><tr><td><p><strong>Testabilidad</strong></p></td><td><p>La rúbrica exige pruebas unitarias a las clases del dominio y las vistas Swing son difíciles de probar</p></td><td><p>El dominio no depende de Swing; los controladores hablan con la vista por una interfaz y se prueban con una vista falsa; los repositorios y notificadores se reemplazan por dobles de prueba</p></td></tr><tr><td><p><strong>Usabilidad</strong></p></td><td><p>Los autores redactan preguntas largas y no deben perder trabajo ni equivocarse de estado</p></td><td><p>Estados con color, campos en rojo, confirmaciones antes de enviar o descartar, formulario de solo lectura fuera de Borrador</p></td></tr></tbody></table>

## 4.2 Escenario de calidad de modificabilidad

Escenario principal: agregar una regla de validación estructural.

<a id="tabla-13"></a>
Tabla 13 Escenario de calidad de modificabilidad.

<table><tbody><tr><td><p><strong>Elemento</strong></p></td><td><p><strong>Descripción</strong></p></td></tr><tr><td><p><strong>Fuente del estímulo</strong></p></td><td><p>El coordinador del banco de preguntas (cliente)</p></td></tr><tr><td><p><strong>Estímulo</strong></p></td><td><p>Solicita una regla nueva de validación estructural: "la pregunta directa no debe superar 300 caracteres"</p></td></tr><tr><td><p><strong>Artefacto</strong></p></td><td><p>El módulo de validación estructural (</p><p>modulo-preguntas</p><p>, paquete</p><p>domain.validation</p><p>)</p></td></tr><tr><td><p><strong>Entorno</strong></p></td><td><p>Tiempo de desarrollo; el sistema ya está funcionando y las reglas RF-08 a RF-13 están implementadas</p></td></tr><tr><td><p><strong>Respuesta</strong></p></td><td><p>El desarrollador crea una clase que implementa</p><p>ValidationRule</p><p>con la nueva regla y la agrega a la lista de</p><p>QuestionValidator.porDefecto()</p><p>. La regla se aplica automáticamente al guardar y al enviar a revisión, tanto en la aplicación de escritorio como en el servicio REST</p></td></tr><tr><td><p><strong>Medida de la respuesta</strong></p></td><td><p>Se crea</p><p><strong>1 clase</strong></p><p>(unas 15 líneas) y se modifica</p><p><strong>1 línea</strong></p><p>; se modifican</p><p><strong>0 líneas</strong></p><p>de</p><p>QuestionService</p><p>, de los controladores y de las vistas; el cambio y su prueba toman</p><p><strong>menos de una hora</strong></p><p>; las pruebas existentes siguen en verde</p></td></tr><tr><td><p><strong>Resultado esperado</strong></p></td><td><p>La regla nueva funciona en todos los puntos de entrada sin tocar la lógica de negocio ni la interfaz, y el resto del sistema no se ve afectado</p></td></tr></tbody></table>

Por qué se puede cumplir.QuestionValidator recibe una lista de reglas y las ejecuta todas; no conoce ninguna regla concreta. QuestionService solo llama al validador, y la interfaz solo muestra las violaciones que este devuelve. Las siete reglas actuales ya se construyeron así, cada una como una clase independiente, sin modificar el servicio.

<a id="figura-14"></a>
![](img/modificabilidad-esbozo-regla.png)

Figura 14 Esbozo de una regla de validación nueva (1 clase) y su registro en QuestionValidator.

## 4.3 Otros escenarios que la arquitectura también soporta

<a id="tabla-14"></a>
Tabla 14 Otros escenarios de modificabilidad soportados por la arquitectura.

<table><tbody><tr><td><p><strong>Cambio solicitado</strong></p></td><td><p><strong>Qué se toca</strong></p></td><td><p><strong>Qué NO se toca</strong></p></td><td><p><strong>Evidencia</strong></p></td></tr><tr><td><p>Guardar las preguntas en una base de datos en vez de en memoria</p></td><td><p>Una clase nueva que implemente</p><p>QuestionRepository</p><p>y la línea donde se arma en</p><p>MainApp</p></td><td><p>QuestionService</p><p>,</p><p>Question</p><p>, los controladores y las vistas</p></td><td><p>Ya ocurrió en el Taller 6:</p><p>QuestionJpaAdapter</p><p>(JPA + H2) implementa el mismo puerto</p><p>QuestionRepository</p><p>y reutiliza el dominio sin cambiarlo</p></td></tr><tr><td><p>Enviar el correo de asignación de revisores por SMTP real en vez del simulado</p></td><td><p>Una clase nueva que implemente</p><p>NotificadorAsignacion</p><p>y una línea en</p><p>MainApp</p></td><td><p>AsignacionRevisionService</p></td><td><p>AsignacionRevisionService</p><p>depende de la interfaz</p><p>NotificadorAsignacion</p><p>, no de</p><p>NotificadorCorreoSimulado</p></td></tr><tr><td><p>Que el Revisor vea solo las preguntas que le asignaron</p></td><td><p>Cambiar la fuente de preguntas que se le entrega a</p><p>GUIRevisor</p></td><td><p>La ventana ni el controlador del Revisor</p></td><td><p>FuenteDePreguntasParaRevisar</p><p>(Strategy) permite cambiar la fuente sin tocar</p><p>RevisionController</p></td></tr><tr><td><p>Agregar un tipo de pregunta nuevo (por ejemplo, con imagen)</p></td><td><p>Un plugin nuevo y una línea en</p><p>plugins.properties</p></td><td><p>El núcleo del microkernel</p></td><td><p>Arquitectura Microkernel del Taller 5: los plugins se cargan por reflexión</p></td></tr></tbody></table>

## 4.4 Tácticas de modificabilidad aplicadas

<a id="tabla-15"></a>
Tabla 15 Tácticas de modificabilidad aplicadas.

<table><tbody><tr><td><p><strong>Táctica</strong></p></td><td><p><strong>Dónde se aplica</strong></p></td></tr><tr><td><p>Aumentar la cohesión (una responsabilidad por clase)</p></td><td><p>Una regla de validación por clase; un controlador por ventana;</p><p>EstadoPregunta</p><p>concentra las transiciones válidas</p></td></tr><tr><td><p>Reducir el acoplamiento con interfaces</p></td><td><p>QuestionRepository</p><p>,</p><p>SimulacroRepository</p><p>,</p><p>AsignacionRevisionRepository</p><p>,</p><p>NotificadorAsignacion</p><p>,</p><p>DirectorioRevisores</p><p>,</p><p>RedaccionPreguntaVista</p><p>,</p><p>RevisionVista</p></td></tr><tr><td><p>Restringir las dependencias</p></td><td><p>Módulos Maven:</p><p>modulo-preguntas</p><p>y</p><p>modulo-usuarios</p><p>no dependen de nadie; el grafo no tiene ciclos y</p><p>app</p><p>es el único que conoce a todos</p></td></tr><tr><td><p>Diferir el enlace</p></td><td><p>Composition root (</p><p>MainApp</p><p>) que arma las dependencias; plugins cargados por reflexión desde un archivo de propiedades</p></td></tr></tbody></table>

# 5\. Arquitectura y diseño de software (modelo C4 y UML)

## 5.1 Estilo arquitectónico

La aplicación es una aplicación de escritorio en Java (Swing) con una arquitectura monolítica en tres capas, organizada como un monolito modular (módulos Maven que se empaquetan en un solo .jar) y con el micropatrón MVC en las ventanas. La docente pidió el monolito modular porque el proyecto migrará a microservicios: si cada módulo ya tiene límites explícitos, más adelante solo hay que cambiar las llamadas entre módulos por llamadas de red.

Las tres capas de cada módulo:

<a id="tabla-16"></a>
Tabla 16 Capas de cada módulo y su responsabilidad.

<table><tbody><tr><td><p><strong>Capa</strong></p></td><td><p><strong>Paquete</strong></p></td><td><p><strong>Responsabilidad</strong></p></td></tr><tr><td><p>Presentación</p></td><td><p>presentation</p><p>(ui en modulo-usuarios)</p></td><td><p>Ventanas Swing (vistas) y controladores MVC</p></td></tr><tr><td><p>Dominio (lógica de negocio)</p></td><td><p>domain</p></td><td><p>Entidades, reglas, servicios e interfaces (puertos) de los repositorios</p></td></tr><tr><td><p>Acceso a datos</p></td><td><p>access</p></td><td><p>Implementaciones de los repositorios (memoria o SQLite)</p></td></tr></tbody></table>

Las capas se comunican en un solo sentido: presentación → dominio ← acceso a datos. El dominio define las interfaces y el acceso a datos las implementa (inversión de dependencias), de modo que el dominio no conoce ni Swing ni la base de datos.

### Módulos y dependencias

<a id="figura-15"></a>
![](img/c4-modulos-dependencias.png)

Figura 15 Módulos del proyecto y sus dependencias.

<a id="tabla-17"></a>
Tabla 17 Módulos del proyecto, su contenido e historias asociadas.

<table><tbody><tr><td><p><strong>Módulo</strong></p></td><td><p><strong>Contenido</strong></p></td><td><p><strong>Historias</strong></p></td></tr><tr><td><p>modulo-usuarios</p></td><td><p>Login, registro, roles, cifrado de contraseñas (Argon2id), SQLite</p></td><td><p>Acceso al sistema</p></td></tr><tr><td><p>modulo-preguntas</p></td><td><p>Pregunta, validación estructural, ciclo de vida, listado, ventanas del Autor y del Revisor</p></td><td><p>HU-01, HU-02, HU-03</p></td></tr><tr><td><p>modulo-revision</p></td><td><p>Asignación de revisores y correo simulado</p></td><td><p>HU-04</p></td></tr><tr><td><p>modulo-simulacros</p></td><td><p>Generación y presentación de simulacros</p></td><td><p>Fuera del primer corte</p></td></tr><tr><td><p>modulo-microkernel</p></td><td><p>Generación de preguntas por plugins (Taller 5)</p></td><td><p>Fuera del primer corte</p></td></tr><tr><td><p>modulo-api-rest</p></td><td><p>Microservicio REST con Spring Boot y JPA (Taller 6)</p></td><td><p>Fuera del primer corte</p></td></tr><tr><td><p>app</p></td><td><p>MainApp</p><p>: arma los módulos, define qué ventana abrir según el rol</p></td><td><p>Todas</p></td></tr></tbody></table>

## 5.2 Modelo C4

Los diagramas C4 están en el archivo de diagrams.net 03-TallerC4-SaberPro.drawio, con una página por nivel: [abrir el diagrama](https://app.diagrams.net/#G1P06v2Eww57q3xeCN8tEg0mBflO3VU5YR).

### Nivel 1 — Contexto

<a id="figura-16"></a>
![](img/c4-nivel-1-contexto.png)

Figura 16 C4-1. Contexto del sistema y sus cinco tipos de usuario.

El Sistema de Banco de Preguntas Saber Pro lo usan cinco tipos de persona:

<a id="tabla-18"></a>
Tabla 18 Personas que usan el sistema (nivel 1 de C4).

<table><tbody><tr><td><p><strong>Persona</strong></p></td><td><p><strong>Qué hace con el sistema</strong></p></td></tr><tr><td><p>Autor de preguntas</p></td><td><p>Diseña, clasifica y edita preguntas; las envía a revisión (HU-01, HU-02, HU-03)</p></td></tr><tr><td><p>Administrador</p></td><td><p>Gestiona usuarios y roles; asigna revisores a las preguntas pendientes (HU-04)</p></td></tr><tr><td><p>Revisor</p></td><td><p>Revisa y aprueba o rechaza las preguntas que se le asignan</p></td></tr><tr><td><p>Docente</p></td><td><p>Genera simulacros y consulta reportes</p></td></tr><tr><td><p>Estudiante</p></td><td><p>Presenta simulacros y consulta sus resultados</p></td></tr></tbody></table>

### Nivel 2 — Contenedores

<a id="figura-17"></a>
![](img/c4-nivel-2-contenedores.png)

Figura 17 C4-2. Contenedores del sistema.

-   Aplicación monolítica Saber Pro(Java / Swing): contiene toda la lógica de negocio, el control de acceso y la gestión de flujos.
-   Base de datos(SQLite): información del sistema.
-   Aparte, y sin que la aplicación de escritorio dependa de él, el microservicio REST del Taller 6 (Spring Boot, JPA y H2) expone el CRUD de preguntas por HTTP.

Diferencia entre el diseño y esta iteración. En el diseño, la base SQLite guarda usuarios, preguntas, revisiones y simulacros. En el primer corte solo los usuarios están en SQLite; las preguntas, las asignaciones de revisores y los simulacros están en repositorios en memoria detrás de interfaces (QuestionRepository, AsignacionRevisionRepository, SimulacroRepository). Pasarlos a SQLite es cambiar la implementación del repositorio (ver el escenario de modificabilidad).

### Nivel 3 — Componentes

<a id="figura-18"></a>
![](img/c4-nivel-3-componentes.png)

Figura 18 C4-3. Componentes de la aplicación monolítica en sus capas.

El diagrama de componentes agrupa la aplicación en las capas de presentación, dominio, acceso a datos y una capa transversal. Correspondencia entre los componentes del diagrama y las clases implementadas:

<a id="tabla-19"></a>
Tabla 19 Correspondencia entre los componentes del diagrama y las clases implementadas.

<table><tbody><tr><td><p><strong>Componente del diagrama</strong></p></td><td><p><strong>Clases implementadas</strong></p></td><td><p><strong>Estado en el primer corte</strong></p></td></tr><tr><td><p>AutenticacionController, UsuarioController</p></td><td><p>LoginFrame</p><p>,</p><p>RegisterFrame</p><p>,</p><p>DashboardFrame</p><p>,</p><p>SesionRouter</p><p>(</p><p>MainApp</p><p>)</p></td><td><p>Implementado</p></td></tr><tr><td><p>PreguntaController</p></td><td><p>GUIQuestions</p><p>,</p><p>RedaccionPreguntaController</p><p>,</p><p>PanelMisPreguntas</p><p>,</p><p>MisPreguntasController</p></td><td><p>Implementado (HU-01 a HU-03)</p></td></tr><tr><td><p>RevisionController</p></td><td><p>GUIRevisor</p><p>,</p><p>RevisionController</p><p>,</p><p>GUIAsignacionRevisores</p></td><td><p>Implementado (HU-04)</p></td></tr><tr><td><p>SimulacroController</p></td><td><p>GUIDocente</p><p>,</p><p>GUIEstudiante</p></td><td><p>Implementado, fuera del primer corte</p></td></tr><tr><td><p>ReporteController</p></td><td><p>GUIObserver1</p><p>,</p><p>GUIObserver2</p><p>(estadísticas y gráfica)</p></td><td><p>Implementado (el Administrador las abre desde el tablero)</p></td></tr><tr><td><p>UsuarioService</p></td><td><p>UserService</p></td><td><p>Implementado</p></td></tr><tr><td><p>PreguntaService</p></td><td><p>QuestionService</p></td><td><p>Implementado</p></td></tr><tr><td><p>ValidadorPregunta</p></td><td><p>QuestionValidator</p><p>y sus siete</p><p>ValidationRule</p></td><td><p>Implementado (RF-08 a RF-13)</p></td></tr><tr><td><p>CicloVidaPreguntaService</p></td><td><p>EstadoPregunta.puedePasarA</p><p>,</p><p>QuestionService.cambiarEstado</p><p>y</p><p>enviarARevision</p></td><td><p>Implementado (RF-14, RF-15)</p></td></tr><tr><td><p>RevisionService</p></td><td><p>AsignacionRevisionService</p></td><td><p>Implementado (HU-04)</p></td></tr><tr><td><p>SimulacroService, CalificacionService</p></td><td><p>SimulacroService</p><p>,</p><p>IntentoSimulacro</p></td><td><p>Implementado, fuera del primer corte</p></td></tr><tr><td><p>UsuarioRepository</p></td><td><p>SqliteUserRepository</p></td><td><p>Implementado (SQLite)</p></td></tr><tr><td><p>PreguntaRepository</p></td><td><p>QuestionImplRepository</p><p>(memoria)</p></td><td><p>Implementado (en memoria)</p></td></tr><tr><td><p>RevisionRepository</p></td><td><p>AsignacionRevisionImplRepository</p><p>(memoria)</p></td><td><p>Implementado (HU-04)</p></td></tr><tr><td><p>SimulacroRepository</p></td><td><p>SimulacroImplRepository</p><p>(memoria)</p></td><td><p>Implementado (en memoria)</p></td></tr><tr><td><p>ServicioCifrado</p></td><td><p>Argon2PasswordHasher</p></td><td><p>Implementado</p></td></tr><tr><td><p>AuditLogger, SeguimientoService, ReporteService</p></td><td><p>—</p></td><td><p>No se implementan en esta iteración</p></td></tr></tbody></table>

### Nivel 4 — Clases (UML)

<a id="figura-19"></a>
![](img/c4-nivel-4-clases.png)

Figura 19 C4-4. Diagrama de clases de diseño de la gestión de preguntas.

Los diagramas siguientes muestran las clases implementadas en esta iteración (las diferencias con el diseño de la Figura 19 C4-4 se explican en la sección de congruencia).

### Dominio de preguntas y validación estructural

<a id="figura-20"></a>
![](img/c4-clases-dominio-preguntas.png)

Figura 20 C4-5. Diagrama de clases: dominio de preguntas y validación estructural.

### MVC en las ventanas del Autor y del Revisor

<a id="figura-21"></a>
![](img/c4-clases-mvc.png)

Figura 21 C4-6. Diagrama de clases: MVC en las ventanas del Autor y del Revisor.

### Asignación de revisores (HU-04)

<a id="figura-22"></a>
![](img/c4-clases-asignacion-revisores.png)

Figura 22 C4-7. Diagrama de clases: asignación de revisores (HU-04).

### Ciclo de vida de una pregunta (RF-14 y RF-15)

<a id="figura-23"></a>
![](img/c4-estados-ciclo-vida.png)

Figura 23 C4-8. Diagrama de estados: ciclo de vida de una pregunta (RF-14 y RF-15).

Desde cualquier estado una pregunta puede pasar a Archivada, porque el requisito RNF-16 prohíbe eliminarlas físicamente.

### Secuencia: enviar una pregunta a revisión (HU-02)

<a id="figura-24"></a>
![](img/c4-secuencia-enviar-revision.png)

Figura 24 C4-9. Diagrama de secuencia: enviar una pregunta a revisión (HU-02).

### Congruencia entre los diagramas C4 y la implementación

El diseño C4 y el código coinciden en lo esencial: el sistema es una aplicación monolítica en tres capas más una capa transversal; cada pantalla tiene un controlador que usa un servicio de dominio, este usa un repositorio a través de una interfaz y un validador independiente; y el ciclo de vida tiene los mismos siete estados. Las diferencias, con su motivo, son las siguientes.

<a id="tabla-20"></a>
Tabla 20 Congruencia entre los diagramas C4 y la implementación.

<table><tbody><tr><td><p><strong>Nivel</strong></p></td><td><p><strong>El diagrama muestra</strong></p></td><td><p><strong>La implementación</strong></p></td><td><p><strong>Motivo</strong></p></td></tr><tr><td><p>1</p></td><td><p>Cinco personas usan el sistema</p></td><td><p>Igual: cinco roles con sus ventanas</p></td><td><p>—</p></td></tr><tr><td><p>2</p></td><td><p>Las personas usan el sistema por HTTPS</p></td><td><p>Es una aplicación de escritorio: la interfaz es Swing y no hay red</p></td><td><p>El diseño inicial no distinguía escritorio de web; el enlace HTTPS solo aplica al microservicio REST</p></td></tr><tr><td><p>2</p></td><td><p>Una base SQLite guarda usuarios, preguntas, revisiones y simulacros</p></td><td><p>SQLite guarda solo los usuarios; preguntas, asignaciones y simulacros están en memoria detrás de interfaces</p></td><td><p>Se priorizó la lógica de negocio del primer corte; cambiar el repositorio no afecta al resto (ver modificabilidad)</p></td></tr><tr><td><p>2</p></td><td><p>No aparece el microservicio REST</p></td><td><p>El módulo</p><p>modulo-api-rest</p><p>(Spring Boot, JPA y H2) es un contenedor aparte</p></td><td><p>Se construyó después, en el Taller 6</p></td></tr><tr><td><p>3</p></td><td><p>Los componentes se llaman</p><p>PreguntaController</p><p>,</p><p>PreguntaService</p><p>,</p><p>ValidadorPregunta</p><p>, etc.</p></td><td><p>Se llaman</p><p>RedaccionPreguntaController</p><p>,</p><p>QuestionService</p><p>,</p><p>QuestionValidator</p><p>, etc. (tabla de correspondencia del nivel 3)</p></td><td><p>Los nombres en el código están en inglés y hay más de un controlador por pantalla</p></td></tr><tr><td><p>3</p></td><td><p>AuditLogger</p><p>,</p><p>SeguimientoService</p><p>,</p><p>ReporteService</p></td><td><p>No se implementan en esta iteración</p></td><td><p>Pertenecen a historias posteriores</p></td></tr><tr><td><p>3</p></td><td><p>Las historias se numeran HU01 a HU07</p></td><td><p>El backlog del equipo numera HU-01 a HU-04 para esta iteración</p></td><td><p>La numeración del diagrama es la del documento del proyecto (ver la nota de numeración en las historias de usuario)</p></td></tr><tr><td><p>4</p></td><td><p>Pregunta</p><p>con subclases</p><p>PreguntaDirecta</p><p>y</p><p>PreguntaSeleccionMultiple</p></td><td><p>Una sola clase</p><p>Question</p><p>, que es la de selección múltiple con única respuesta</p></td><td><p>Es el único tipo de pregunta que exige esta iteración</p></td></tr><tr><td><p>4</p></td><td><p>id: Long</p><p>,</p><p>competencia: String</p><p>,</p><p>nivelDificultad</p></td><td><p>id: String</p><p>(por ejemplo "P-001"),</p><p>Competencia</p><p>y</p><p>Dificultad</p><p>como enumeraciones</p></td><td><p>Evita valores inválidos y hace los filtros más seguros</p></td></tr><tr><td><p>4</p></td><td><p>distractores: String[4]</p><p>y</p><p>respuestaCorrecta: String</p></td><td><p>Cuatro opciones A a D (</p><p>QuestionDistractors</p><p>) y</p><p>respuestaCorrecta</p><p>es la letra de la opción correcta</p></td><td><p>Decisión D2: las opciones son cuatro y una de ellas es la correcta</p></td></tr><tr><td><p>4</p></td><td><p>Pregunta.validarEstructura()</p><p>y</p><p>ValidadorPregunta.validar(Pregunta)</p></td><td><p>QuestionValidator</p><p>ejecuta siete</p><p>ValidationRule</p><p>y devuelve todas las violaciones</p></td><td><p>Decisión D3: la validación vive fuera de la entidad y cada regla es una clase</p></td></tr><tr><td><p>4</p></td><td><p>PreguntaService</p><p>: crear, editar, clasificar y enviar a revisión</p></td><td><p>crearBorrador</p><p>,</p><p>actualizarContenido</p><p>,</p><p>enviarARevision</p><p>,</p><p>cambiarEstado</p><p>y</p><p>buscarDelAutor</p></td><td><p>La clasificación son campos de la pregunta y no una operación aparte; el listado y sus filtros son parte de la historia HU-03</p></td></tr><tr><td><p>4</p></td><td><p>IPreguntaRepository</p><p>:</p><p>save</p><p>,</p><p>findById</p><p>,</p><p>findByFiltros</p><p>,</p><p>update</p></td><td><p>QuestionRepository</p><p>:</p><p>crear</p><p>,</p><p>actualizar</p><p>,</p><p>obtenerPorId</p><p>,</p><p>obtenerTodas</p><p>,</p><p>generarNuevoId</p></td><td><p>Los filtros y la paginación se hacen en el servicio (decisión D9)</p></td></tr><tr><td><p>4</p></td><td><p>PreguntaRepository</p><p>con una conexión SQLite</p></td><td><p>QuestionImplRepository</p><p>en memoria (y</p><p>QuestionJpaAdapter</p><p>en el microservicio)</p></td><td><p>Ver el nivel 2</p></td></tr><tr><td><p>4</p></td><td><p>PreguntaController</p><p>: crear, editar, enviar a revisión</p></td><td><p>RedaccionPreguntaController</p><p>y</p><p>MisPreguntasController</p><p>, cada uno con su vista por una interfaz</p></td><td><p>Se aplicó MVC con vistas intercambiables para poder probar la lógica de las ventanas</p></td></tr><tr><td><p>4</p></td><td><p>EstadoPregunta</p><p>con siete valores</p></td><td><p>Idéntico</p></td><td><p>—</p></td></tr></tbody></table>

## 5.3 Vista de ejecución

mvn package genera un único app/target/banco-preguntas-saberpro.jar con todas las dependencias. Al ejecutarlo se muestra el login; según el rol del usuario, MainApp abre las ventanas que le corresponden. Al cerrar la ventana principal del rol vuelve el login, para poder cambiar de usuario sin reiniciar (los datos de preguntas viven en memoria).

<a id="tabla-21"></a>
Tabla 21 Ventanas de la aplicación por rol.

<table><tbody><tr><td><p><strong>Rol</strong></p></td><td><p><strong>Ventanas</strong></p></td></tr><tr><td><p>Autor de preguntas</p></td><td><p>Redacción y listado de preguntas; generador de preguntas por plugins</p></td></tr><tr><td><p>Administrador</p></td><td><p>Tablero, estadísticas, gráfica y asignación de revisores</p></td></tr><tr><td><p>Revisor</p></td><td><p>Revisión de preguntas</p></td></tr><tr><td><p>Docente</p></td><td><p>Generación de simulacros</p></td></tr><tr><td><p>Estudiante</p></td><td><p>Presentación de simulacros</p></td></tr></tbody></table>

## 5.4 Tecnologías

Java 17 o superior (se desarrolla con 21), Maven, Swing con FlatLaf, SQLite, Argon2id, JUnit 5 y Mockito para las pruebas, y Spring Boot con JPA y H2 en el microservicio REST.

# 6\. Patrones de diseño, principios SOLID y decisiones de diseño

## 6.1 Patrones de diseño implementados

<a id="tabla-22"></a>
Tabla 22 Patrones de diseño implementados.

<table><tbody><tr><td><p><strong>Patrón</strong></p></td><td><p><strong>Dónde</strong></p></td><td><p><strong>Contexto del problema y cómo se aplicó</strong></p></td></tr><tr><td><p><strong>MVC</strong></p><p>(micropatrón)</p></td><td><p>GUIQuestions</p><p>+</p><p>RedaccionPreguntaController</p><p>;</p><p>PanelMisPreguntas</p><p>+</p><p>MisPreguntasController</p><p>;</p><p>GUIRevisor</p><p>+</p><p>RevisionController</p></td><td><p>La lógica de cada ventana estaba mezclada con Swing y no se podía probar. La vista solo pinta y le avisa al controlador lo que hace el usuario; el controlador decide y usa el modelo (</p><p>QuestionService</p><p>). La vista se declara como interfaz (</p><p>RedaccionPreguntaVista</p><p>,</p><p>MisPreguntasVista</p><p>,</p><p>RevisionVista</p><p>), así el controlador se prueba con una vista falsa.</p></td></tr><tr><td><p><strong>Observer</strong></p></td><td><p>Subject</p><p>/</p><p>Observer</p><p>;</p><p>QuestionService</p><p>como sujeto;</p><p>GUIObserver1</p><p>(estadísticas) y</p><p>GUIObserver2</p><p>(gráfica) como observadores</p></td><td><p>Las vistas de estadísticas deben actualizarse cada vez que cambia una pregunta, sin que el servicio conozca las ventanas. El servicio notifica y cada vista se refresca sola.</p></td></tr><tr><td><p><strong>Builder</strong></p></td><td><p>Question.builder()</p></td><td><p>Una pregunta tiene 14 atributos; un constructor con tantos parámetros es ilegible y propenso a errores de orden. El constructor mantiene las invariantes mínimas y el builder permite armar la pregunta campo a campo.</p></td></tr><tr><td><p><strong>Strategy</strong></p></td><td><p>ValidationRule</p><p>y sus siete reglas;</p><p>IMenuProvider</p><p>y un menú por rol;</p><p>IPasswordHasher</p><p>y</p><p>IPasswordPolicy</p><p>;</p><p>FuenteDePreguntasParaRevisar</p></td><td><p>Cada regla de validación estructural (RF-08 a RF-13) es una estrategia intercambiable que</p><p>QuestionValidator</p><p>ejecuta sin conocerlas; los menús se eligen según el rol; el Revisor recibe su lista de preguntas de una fuente que se puede cambiar.</p></td></tr><tr><td><p><strong>Factory Method y Singleton</strong></p></td><td><p>UserRepositoryFactory</p><p>,</p><p>PasswordHasherFactory</p><p>,</p><p>PasswordPolicyFactory</p></td><td><p>El módulo de usuarios entrega su repositorio, su cifrado y su política de contraseñas por defecto a través de una fábrica única, sin que el resto conozca las clases concretas.</p></td></tr><tr><td><p><strong>Template Method</strong></p></td><td><p>BaseQuestionPlugin.generate</p><p>(final) y</p><p>construirEnunciado</p><p>(abstracto)</p></td><td><p>Todos los plugins de generación de preguntas validan y construyen la pregunta igual; solo cambia cómo se arma el enunciado.</p></td></tr><tr><td><p><strong>Adapter</strong></p><p>(puertos y adaptadores)</p></td><td><p>QuestionJpaAdapter</p><p>(JPA);</p><p>DirectorioRevisoresDeUsuarios</p><p>;</p><p>QuestionImplRepository</p></td><td><p>Un adaptador conecta una tecnología o módulo externo con la interfaz que espera el dominio: JPA con</p><p>QuestionRepository</p><p>, y el módulo de usuarios con</p><p>DirectorioRevisores</p><p>.</p></td></tr><tr><td><p><strong>Repository</strong></p></td><td><p>QuestionRepository</p><p>,</p><p>SimulacroRepository</p><p>,</p><p>AsignacionRevisionRepository</p><p>,</p><p>IUserRepository</p></td><td><p>El dominio pide y guarda entidades sin saber si están en memoria, en SQLite o en una base JPA.</p></td></tr><tr><td><p><strong>Pipes and Filters</strong></p><p>(arquitectónico)</p></td><td><p>QuestionPipeline</p><p>y cuatro filtros de validación</p></td><td><p>Las solicitudes de generación de preguntas pasan por filtros encadenados; cada filtro valida una cosa (Taller 5).</p></td></tr><tr><td><p><strong>Microkernel</strong></p><p>(arquitectónico)</p></td><td><p>QuestionMicrokernel</p><p>y los plugins de</p><p>plugins.properties</p></td><td><p>Nuevos tipos de pregunta se agregan como plugins cargados por reflexión, sin tocar el núcleo (Taller 5).</p></td></tr><tr><td><p><strong>Máquina de estados</strong></p></td><td><p>EstadoPregunta.puedePasarA</p></td><td><p>Las transiciones válidas del ciclo de vida (RF-14, RF-15) viven en un solo lugar y</p><p>QuestionService</p><p>las aplica.</p></td></tr></tbody></table>

## 6.2 Principios SOLID

<a id="tabla-23"></a>
Tabla 23 Aplicación de los principios SOLID.

<table><tbody><tr><td><p><strong>Principio</strong></p></td><td><p><strong>Cómo se aplica</strong></p></td></tr><tr><td><p><strong>S</strong></p><p>— Responsabilidad única</p></td><td><p>Cada regla de validación es una clase;</p><p>QuestionValidator</p><p>solo ejecuta reglas;</p><p>QuestionService</p><p>coordina el caso de uso; la vista solo pinta y el controlador solo decide.</p></td></tr><tr><td><p><strong>O</strong></p><p>— Abierto/cerrado</p></td><td><p>Agregar una regla de validación, un plugin, un menú de un rol nuevo o un notificador no modifica las clases que los usan (ver el escenario de modificabilidad).</p></td></tr><tr><td><p><strong>L</strong></p><p>— Sustitución de Liskov</p></td><td><p>QuestionImplRepository</p><p>(memoria) y</p><p>QuestionJpaAdapter</p><p>(JPA) son intercambiables donde se espera un</p><p>QuestionRepository</p><p>; las pruebas del servicio usan dobles y las del microservicio usan JPA.</p></td></tr><tr><td><p><strong>I</strong></p><p>— Segregación de interfaces</p></td><td><p>Interfaces pequeñas y específicas:</p><p>NotificadorAsignacion</p><p>(un método),</p><p>DirectorioRevisores</p><p>,</p><p>FuenteDePreguntasParaRevisar</p><p>,</p><p>MisPreguntasVista</p><p>; ningún cliente depende de métodos que no usa.</p></td></tr><tr><td><p><strong>D</strong></p><p>— Inversión de dependencias</p></td><td><p>Los servicios dependen de interfaces (</p><p>QuestionRepository</p><p>,</p><p>NotificadorAsignacion</p><p>,</p><p>DirectorioRevisores</p><p>) y no de implementaciones;</p><p>MainApp</p><p>es el único lugar que las une.</p></td></tr></tbody></table>

## 6.3 Decisiones de diseño

<a id="tabla-24"></a>
Tabla 24 Decisiones de diseño y su motivo.

<table><tbody><tr><td><p><strong>#</strong></p></td><td><p><strong>Decisión</strong></p></td><td><p><strong>Motivo</strong></p></td></tr><tr><td><p>D1</p></td><td><p>Monolito modular con módulos Maven</p></td><td><p>La docente pidió una arquitectura que permita migrar a microservicios: los límites entre módulos ya están declarados y el grafo de dependencias no tiene ciclos.</p></td></tr><tr><td><p>D2</p></td><td><p>La pregunta tiene</p><p><strong>cuatro opciones (A a D)</strong></p><p>, una de ellas correcta</p></td><td><p>Es el formato de las preguntas Saber Pro. Las opciones incorrectas son los distractores.</p></td></tr><tr><td><p>D3</p></td><td><p>La validación estructural vive fuera de la entidad, en</p><p>QuestionValidator</p></td><td><p>La entidad solo protege sus invariantes mínimas; las reglas de negocio se reúnen en un validador que devuelve</p><p><strong>todas</strong></p><p>las violaciones a la vez y que reutilizan la aplicación de escritorio y el servicio REST.</p></td></tr><tr><td><p>D4</p></td><td><p>Solo el autor puede modificar su pregunta y solo mientras esté en Borrador</p></td><td><p>Cumple RF-06 y evita que una pregunta en revisión cambie mientras alguien la evalúa.</p></td></tr><tr><td><p>D5</p></td><td><p>El repositorio de preguntas es en memoria detrás de una interfaz</p></td><td><p>Permite avanzar con la lógica de negocio; cambiar a una base de datos es reemplazar la implementación (ya se hizo en el Taller 6 con JPA).</p></td></tr><tr><td><p>D6</p></td><td><p>Las vistas se acceden desde el controlador por una interfaz</p></td><td><p>Permite probar la lógica de las ventanas sin abrir ninguna ventana.</p></td></tr><tr><td><p>D7</p></td><td><p>El correo de asignación de revisores es simulado</p></td><td><p>El proyecto permite simular las integraciones externas; el servicio depende de</p><p>NotificadorAsignacion</p><p>, de modo que se puede cambiar por SMTP sin tocarlo.</p></td></tr><tr><td><p>D8</p></td><td><p>MainApp</p><p>arma las dependencias a mano</p></td><td><p>Con pocos módulos no se justifica un framework de inyección; el composition root deja explícito cómo se conecta todo.</p></td></tr><tr><td><p>D9</p></td><td><p>La búsqueda y la paginación del listado se hacen en el servicio de dominio</p></td><td><p>Con repositorio en memoria es lo más simple; con una base de datos se traslada a la consulta sin cambiar el controlador ni la vista.</p></td></tr><tr><td><p>D10</p></td><td><p>Los estados de una pregunta se muestran con color en toda la aplicación</p></td><td><p>Lo pide la historia HU-02 y ayuda a reconocer el estado de un vistazo (</p><p>EstadoColores</p><p>es la única fuente de la paleta).</p></td></tr></tbody></table>

# 7\. Pruebas unitarias y organización del repositorio

## 7.1 Pruebas unitarias automatizadas

Hay pruebas unitarias para las entidades y los servicios del dominio de cada módulo, para los controladores de las ventanas y para las reglas de validación. Se ejecutan con JUnit 5 y Mockito:

mvn test

<a id="tabla-25"></a>
Tabla 25 Pruebas unitarias automatizadas por módulo.

<table><tbody><tr><td><p><strong>Módulo</strong></p></td><td><p><strong>Pruebas</strong></p></td><td><p><strong>Qué cubren</strong></p></td></tr><tr><td><p>modulo-usuarios</p></td><td><p>37</p></td><td><p>User</p><p>,</p><p>UserService</p><p>, contraseñas (Argon2id y política), repositorio SQLite, menús por rol y su registro, fábricas</p></td></tr><tr><td><p>modulo-preguntas</p></td><td><p>154</p></td><td><p>Question</p><p>,</p><p>QuestionDistractors</p><p>,</p><p>ContenidoPregunta</p><p>,</p><p>EstadoPregunta</p><p>,</p><p>QuestionService</p><p>, las siete reglas de validación y el validador,</p><p>FiltroPreguntas</p><p>,</p><p>Pagina</p><p>, búsqueda paginada, los tres controladores de las ventanas</p></td></tr><tr><td><p>modulo-simulacros</p></td><td><p>25</p></td><td><p>Simulacro</p><p>,</p><p>IntentoSimulacro</p><p>,</p><p>SimulacroService</p><p>, repositorio</p></td></tr><tr><td><p>modulo-microkernel</p></td><td><p>39</p></td><td><p>Microkernel, plugins, filtros y pipeline, solicitud de generación</p></td></tr><tr><td><p>modulo-revision</p></td><td><p>23</p></td><td><p>Revisor</p><p>,</p><p>AsignacionRevision</p><p>, el repositorio y el correo simulados,</p><p>DirectorioRevisoresDeUsuarios</p><p>,</p><p>FuenteDePreguntasAsignadas</p><p>y</p><p>AsignacionRevisionService</p><p>(una prueba por regla de HU-04)</p></td></tr><tr><td><p>modulo-api-rest</p></td><td><p>52</p></td><td><p>Controlador REST, servicio, mapper, adaptador JPA, integración</p></td></tr><tr><td><p><strong>Total</strong></p></td><td><p><strong>330</strong></p></td><td></td></tr></tbody></table>

Estilo de las pruebas. Cada prueba sigue Given / When / Then y tiene un nombre que describe el comportamiento (por ejemplo, enviarARevision\_sinConfirmacion\_laPreguntaSigueEnBorrador). Los servicios se prueban con dobles de sus dependencias (Mockito) o con el repositorio en memoria; los controladores se prueban con una vista falsa.

## 7.2 Organización del repositorio

banco-preguntas-saberpro/

├── pom.xml Proyecto padre (módulos y versiones)

├── README.md Descripción, cómo compilar y ejecutar, estado del proyecto

├── docs/ Documentación de arquitectura (esta carpeta)

├── app/ Composition root y ejecutable

├── modulo-usuarios/ Login, registro y roles

├── modulo-preguntas/ Banco de preguntas (HU-01, HU-02, HU-03)

├── modulo-revision/ Asignación de revisores (HU-04, implementado)

├── modulo-simulacros/ Simulacros (docente y estudiante)

├── modulo-microkernel/ Generación de preguntas por plugins

└── modulo-api-rest/ Microservicio REST

Cada módulo tiene su propio pom.xml y, dentro, los paquetes domain, access y presentation de las tres capas.

Cómo compilar y ejecutar

mvn test # todas las pruebas mvn package # genera app/target/banco-preguntas-saberpro.jar java -jar app/target/banco-preguntas-saberpro.jar # abre el login

Usuarios de prueba (contraseña Saber2026!): autor1 (Autor), revisor1 (Revisor), docente1 (Docente), estudiante1 (Estudiante) y admin1 (Administrador).

## 7.3 Trabajo en equipo

-   Tablero de tareas: las historias, sus criterios y sus subtareas están en el tablero de Jira del Sprint 1.
-   Commits: cada integrante del equipo trabaja con su propia identidad de Git, de modo que cada commit queda a nombre de quien lo hizo.
-   Mensajes de commit: describen el cambio en una línea y, cuando corresponde, indican la historia (por ejemplo, "(HU-04)").

# 8\. Enlaces

<a id="tabla-26"></a>
Tabla 26 Enlaces del proyecto (video, repositorio, Jira, Figma, C4).

<table><tbody><tr><td><p><strong>Recurso</strong></p></td><td><p><strong>URL</strong></p></td></tr><tr><td><p>Video de sustentación (YouTube)</p></td><td><p><a href="https://youtu.be/xWMwpDzWfwI">https://youtu.be/xWMwpDzWfwI</a></p></td></tr><tr><td><p>Repositorio Git del proyecto</p></td><td><p><a href="https://github.com/DVLASZ/banco-preguntas-saberpro">https://github.com/DVLASZ/banco-preguntas-saberpro</a></p></td></tr><tr><td><p>Tablero de tareas (Jira, Sprint 1)</p></td><td><p><a href="https://dvlasz.atlassian.net/jira/software/projects/SCRUM/boards/1">https://dvlasz.atlassian.net/jira/software/projects/SCRUM/boards/1</a></p></td></tr><tr><td><p>Prototipos (Figma)</p></td><td><p><a href="https://www.figma.com/design/kk6g0a8B4gv7wqCsYRb4Xd/Banco-de-Preguntas-Saber-Pro---Prototipos">https://www.figma.com/design/kk6g0a8B4gv7wqCsYRb4Xd/Banco-de-Preguntas-Saber-Pro---Prototipos</a></p><p>(página "Primer corte - Prototipos HU-01 a HU-04")</p></td></tr><tr><td><p>Diagramas C4 (diagrams.net)</p></td><td><p><a href="https://app.diagrams.net/#G1P06v2Eww57q3xeCN8tEg0mBflO3VU5YR">https://app.diagrams.net/#G1P06v2Eww57q3xeCN8tEg0mBflO3VU5YR</a></p></td></tr></tbody></table>