# 09 · Trazabilidad de requisitos

Revisión, requisito por requisito, de los documentos de la materia contra lo que está construido:
**Proyecto de clase 2026.2**, **Entregables del segundo corte** y **Entregables del tercer corte** (este último como
adelanto, porque el trabajo ya lo cubre en buena parte). Fecha de la revisión: 16 de octubre de 2026.

Estados: **Cumple** · **Parcial** (hay algo por completar) · **Pendiente** (entregable que se hace al final o falta
hacer) · **N/A** (fuera del alcance de los cortes, con la razón).

## 1. Cómo se verificó

| Verificación | Resultado |
|---|---|
| Pila completa con `docker compose up -d --build` | 30 servicios en el compose: 13 PostgreSQL, RabbitMQ, Keycloak, Eureka, 14 servicios, gateway, `edge`, frontend y la política de mensajes muertos (todos *healthy*, verificado el 19 de octubre de 2026) |
| Recorrido funcional contra Docker (`pruebas-funcionales`), con tokens reales de Keycloak | 73 comprobaciones de HU-01 a HU-06 y RF-21 a RF-28 (incluido el cierre por tiempo agotado), todas correctas |
| Colecciones de Postman con newman, una por servicio | 11 colecciones, 200 peticiones y 314 verificaciones, todas correctas |
| Pruebas automatizadas del backend (`./mvnw test`) | 1040 pruebas, todas correctas (reactor completo: 10 min 29 s) |
| Pruebas del frontend (`npm test`, Vitest) | 183 pruebas, todas correctas |
| Pruebas del dominio | Todas las clases del dominio de los 14 servicios tienen pruebas |
| Swagger | La interfaz del gateway y el OpenAPI de cada servicio responden; los contratos están exportados en [`api/openapi`](api/openapi/) |
| Escalabilidad | Prueba de carga k6 del 8 de octubre (documento 04) |

## 2. Segundo corte

### 2.1 Requisitos técnicos

| # | Requisito | Dónde se cumple | Estado |
|---|---|---|---|
| 1 | Refactorizar la iteración 1 con una solución distribuida: microservicios orientados a eventos, para garantizar la escalabilidad | Siete servicios de negocio, gateway, Eureka y RabbitMQ; documentos 02, 03 y 08 | Cumple |
| 2 | La Figura 1 (contextos delimitados) es una aproximación; cada grupo puede proponer los suyos | Documento 02: 14 servicios implementados y los demás contextos documentados | Cumple |
| 3 | Al menos 6 patrones GoF: 2 creacionales, 2 estructurales y 2 de comportamiento | Documento 05. **Creacionales:** Builder (`Question.Builder`) y Factory Method (`CreadorDeAviso` y sus 8 creadores). **Estructurales:** Adapter (puertos y adaptadores) y Facade (`api-gateway`). **Comportamiento:** Strategy (`PoliticaDeDecision`) y Observer (eventos por RabbitMQ). Además State (`FaseDePregunta`), Template Method (`ReglaPorOpcion`) y Decorator (`EmailConReintentos`, `EmailConRegistro`) | Cumple |
| 4 | Nueva historia de usuario (en azul): evaluar preguntas asignadas | HU-05, implementada y probada de punta a punta | Cumple |
| 5 | Frontend y backend separados y comunicados por una API REST | Angular + nginx; el gateway expone la API | Cumple |

### 2.2 Requisitos funcionales (HU-01 a HU-05)

Comprobados con el recorrido funcional contra la pila de Docker.

| HU | Requisito | Evidencia | Estado |
|---|---|---|---|
| HU-01 | Crear pregunta con contexto, pregunta directa, cuatro opciones, respuesta correcta, justificación, bibliografía, competencia, tema, subtema y dificultad; validación estructural al grabar | `POST /api/questions` responde 201 en Borrador; rechaza dos preguntas directas, «todas las anteriores», falta de contexto y opciones repetidas | Cumple |
| HU-02 | Pasar de Borrador a Pendiente de revisión, con confirmación; los estados se ven con colores | El formulario pide confirmar el envío; `POST /submit`; un segundo envío se rechaza; insignias de color por estado | Cumple |
| HU-03 | Listar las preguntas propias, con paginación y filtros | `GET /api/questions?estado=&page=&size=` devuelve la página | Cumple |
| HU-04 | El administrador asigna de 1 a 3 revisores a preguntas pendientes; se notifica | 0 y 4 revisores y el autor como revisor se rechazan; con 2 revisores la pregunta pasa a En revisión; cada revisor y el autor reciben su aviso (correo simulado) | Cumple |
| HU-05 | El revisor lista sus preguntas, evalúa y aprueba o rechaza; deja observaciones que lee el autor; se notifica a los implicados | Lista de asignadas; rechazar exige observación; una aprobación no basta (unanimidad); con todas queda Aprobada; un rechazo deja Rechazada; el autor lee las observaciones; el administrador recibe cada evaluación y el resultado, los revisores y el autor reciben el resultado | Cumple |
| (extra) | Publicar, corregir y reabrir | El administrador publica; el autor reabre una rechazada (Borrador, versión 2) y la reenvía, lo que abre una ronda nueva | Cumple |
| — | Notificaciones por correo | Los docentes decidieron correo **simulado** por eventos (consulta 16): puerto `EmailSender`, adaptador `SimulatedEmailSender` y bandeja en el sistema; ver [notificaciones](anexos/notificaciones-y-correos.md) | Cumple (simulado, por decisión del docente) |

### 2.3 Rúbrica

| Criterio | Peso | Estado | Evidencia y observaciones |
|---|---|---|---|
| Requisitos funcionales | 30 % | Cumple | Sección 2.2 |
| Requisitos no funcionales de escalabilidad y modificabilidad (SOLID, patrones, microservicios) | 30 % | Cumple | Microservicios, escalabilidad medida (04), 6 patrones (05), SOLID y decisiones ([10](10-decisiones-y-solid.md)) |
| Documentación de arquitectura y repositorio | 20 % | Parcial | Documentos 01 a 10, anexos, OpenAPI, Postman y diagramas, todos alineados con el código. Falta publicar el repositorio completo (se publica por partes) y armar el PDF formal |
| Pruebas unitarias automatizadas a todas las clases del dominio | 20 % | Cumple | 1040 pruebas de backend; todas las clases del dominio cubiertas ([plan de pruebas](anexos/plan-de-pruebas.md)) |

### 2.4 Protocolo de sustentación (video de YouTube)

| Parte | Tiempo | Material listo |
|---|---|---|
| Historias de usuario de la segunda iteración | 1 min | Documento 01 |
| Escenario de calidad de escalabilidad | 1 min | Documento 04 |
| Arquitectura y diseño con C4 y UML, frontend y backend separados por REST | 2 min | Documento 03 y diagramas de `img/diagramas` |
| Los patrones GoF implementados (6 de la rúbrica y 3 más) | 2 min | Documento 05 |
| Diagrama de contextos delimitados | 1 min | Documento 02, `contextos-delimitados.png` |
| Pruebas unitarias automatizadas | 1 min | `./mvnw test` |
| Repositorio con los commits de todos los integrantes y tablero del sprint | 1 min | Serie de commits con autoría repartida y tablero de Jira |
| API REST generada por Swagger | 1 min | Swagger UI del gateway |
| Software funcional y aspectos clave del código | 5 min | Pila de Docker |
| Grabar el video (todos los integrantes, sin pasarse del tiempo) | — | **Pendiente** (se hace al final) |

### 2.5 Documento de arquitectura

| Parte requerida | Dónde está | Estado |
|---|---|---|
| Portada | — | Pendiente (se arma en el documento formal) |
| Introducción breve | `docs/corte-2/README.md` | Pendiente de redactar para el documento formal |
| Historias de usuario con criterios de aceptación | Documento 01 | Cumple |
| Pantallazo de planificación del Sprint 2 (Jira) | Documento 07 y `img/jira-sprint-2-*.png` | Cumple |
| Escenario de calidad de escalabilidad (contexto, estímulo, respuesta, medida, resultado) | Documento 04 | Cumple |
| Arquitectura y diseño con C4 | Documento 03: niveles 1, 2 y 3, estados, secuencias, modelo de datos y despliegue | Cumple |
| Lista de patrones con su descripción | Documento 05 | Cumple |
| URL del video de YouTube | — | Pendiente |
| URL del repositorio | `https://github.com/DVLASZ/banco-preguntas-saberpro` | Cumple |

## 3. Proyecto de clase 2026.2

### 3.1 Requisitos funcionales

| RF | Requisito | Dónde se cumple | Estado |
|---|---|---|---|
| RF-01 | Registrar usuarios | Usuarios precargados en el realm de Keycloak y sincronizados por `user-service`; lo confirmó el docente ([datos semilla](anexos/datos-semilla.md)) | Cumple (decisión) |
| RF-02 | Autenticación con usuario y contraseña | Keycloak, flujo con PKCE en el frontend; JWT validado en gateway y servicios | Cumple |
| RF-03 | Roles: administrador, autor, revisor, docente y estudiante | Los cinco roles existen en Keycloak y los exigen el gateway y los servicios (`ADMIN`, `AUTHOR`, `REVIEWER`, `TEACHER`, `STUDENT`) | Cumple |
| RF-04 a RF-07 | Crear, campos, modificar en Borrador y consultar con filtros | `question-service` (contenido) y `lifecycle-service` (estados); una rechazada vuelve a Borrador para corregirse | Cumple |
| RF-08 a RF-13 | Validación estructural | `domain/validation`: una regla por requisito | Cumple |
| RF-14 y RF-15 | Siete estados y transiciones válidas | `EstadoPregunta.puedePasarA` y pruebas | Cumple |
| RF-16 a RF-20 | Asignar revisores, formato de evaluación, observaciones, historial y aprobar o rechazar | `assignment-service` (asignación) y `evaluation-service` (evaluación): la evaluación es una decisión con observación (sin puntaje rígido, según el docente); historial por rondas | Cumple |
| RF-21 a RF-24 | Simulacros: generarlos con preguntas publicadas, aleatorios por competencia, tema o dificultad, con tiempo máximo y calificación automática | `catalog-service` y `simulation-service` ([documento](../corte-3/simulacros-y-seguimiento.md)): verificado con el recorrido funcional (incluido el cierre por tiempo agotado) y Postman con los 14 servicios (19 de octubre de 2026). Los docentes los ubicaron en el corte 3; se construyeron por adelantado en el backend | Cumple (backend y pantallas del estudiante) |
| RF-25 a RF-28 | Seguimiento: historial por estudiante, estadísticas individuales, fortalezas y debilidades por competencia, reportes para docentes y coordinadores | `statistics-service` y `report-service` (mismo documento): historial, estadísticas con tendencia, fortalezas y debilidades, reporte de grupo en JSON y CSV | Cumple (backend y pantallas del estudiante y del docente) |
| RF-29 y RF-30 | Trazabilidad con fecha y usuario | `history-service` (línea de tiempo) y el historial de estados de cada pregunta en `lifecycle-service` | Cumple |

### 3.2 Requisitos no funcionales

| RNF | Requisito | Evidencia | Estado |
|---|---|---|---|
| RNF-01 y 02 | Interfaz intuitiva y consistente; navegadores modernos | Angular con componentes propios (documento 06) | Cumple |
| RNF-03 | Curva de aprendizaje de 30 minutos o menos | Requiere la prueba de usabilidad con usuarios; el guion y las plantillas están listos ([anexo](anexos/prueba-de-usabilidad.md)) | **Pendiente de aplicar** |
| RNF-04 y 05 | Consulta en menos de 3 s; al menos 100 usuarios concurrentes | k6 con 600 usuarios: p95 de 18 ms (ritmo humano) y 692 ms (estrés), 0 % de error | Cumple |
| RNF-06 y 07 | Autenticación y acceso por rol | Sin token 401; un autor en un endpoint de administrador 403 | Cumple |
| RNF-08 | Contraseñas con hash seguro | Keycloak las guarda con hash; los servicios no guardan contraseñas | Cumple |
| RNF-09 | Auditoría de inicios de sesión, cambios de estado y acciones de revisión | `audit-service` y `GET /api/audit` | Cumple |
| RNF-10 | SOLID y buenas prácticas | Documento [10](10-decisiones-y-solid.md): cada principio con las clases reales | Cumple |
| RNF-11 a 14 | Mantenimiento, módulos nuevos, separación de capas, bajo acoplamiento | Dominio sin dependencias de Spring (ArchUnit), `PoliticaDeDecision`, `CreadorDeAviso`, lista de reglas de validación, un servicio por contexto | Cumple |
| RNF-15 | Integridad de la información | Flyway, transacciones, Outbox transaccional y consumidores idempotentes | Cumple |
| RNF-16 | Ninguna pregunta se borra físicamente | No existe ningún endpoint de borrado; cualquier estado puede pasar a Archivada | Cumple |
| RNF-17 y 18 | 500 usuarios concurrentes en menos de 3 s; escalar con instancias sin tocar la lógica | k6 con 600 usuarios; réplicas con `--scale` (documento 04) | Cumple |

### 3.3 Restricciones y trabajo en equipo

| Requisito | Estado | Observación |
|---|---|---|
| Java; interfaz en Swing o JavaFX; MariaDB o PostgreSQL | Cumple | Java 21 y PostgreSQL. La interfaz del corte 1 es Swing; desde el corte 2 el frontend es web (Angular), porque el segundo corte pide separar frontend y backend por REST |
| Justificar técnicamente la elección tecnológica (ventajas, desventajas y razones) | Cumple | Documento [10](10-decisiones-y-solid.md), sección 3 |
| Integraciones externas no obligatorias; pueden simularse justificando | Cumple | Correo simulado por decisión del docente |
| Grupo de 3 a 5 personas, con herramienta de coordinación | Cumple | Tres integrantes; Jira con sprints |

### 3.4 Entregables de cada corte (formato del documento en PDF)

| Parte | Estado | Observación |
|---|---|---|
| Portada y resumen | Pendiente | Se arman en el documento formal |
| Requisitos funcionales como épicas, historias y criterios; referencia a la hoja de cálculo de la plantilla de historias | Cumple | Las historias con criterios están en el documento 01 y en Jira (épicas, historias y subtareas). No se entregó plantilla de hoja de cálculo; Jira junto con la documentación cumple esa función |
| Prototipos y prueba de usabilidad | Parcial | Prototipos de alta fidelidad e interfaz implementada (documento 06); la prueba tiene material listo y falta aplicarla |
| Requisitos no funcionales como escenarios de calidad | Cumple | Escalabilidad (04); modificabilidad en el primer corte y en el documento 10 |
| Diagrama entidad-relación | Cumple | `img/diagramas/er-*.png` (un diagrama por base de datos) |
| JPA para la persistencia y DTO en la API (requerimiento del 20 de octubre de 2026) | Cumple | Los 13 servicios con datos usan JPA y todos los controladores usan DTO ([anexo](anexos/jpa-y-dto.md)); el monolito solo en `modulo-api-rest` |
| Arquitectura con C4 y vistas 4+1 relevantes | Cumple | Documento 03 (C4, secuencias, estados, despliegue) |
| Decisiones de arquitectura y su justificación (tipo de aplicación, estilo, tecnologías) | Cumple | Documento 10 |
| URL del repositorio | Cumple | |
| Prototipo funcional que compile, ejecute y pase las pruebas | Cumple | Verificado con Docker y con las pruebas |

## 4. Tercer corte (adelanto)

| Requisito | Estado | Observación |
|---|---|---|
| HU-01 a HU-05 | Cumple | Sección 2.2 |
| Nueva historia: el autor lista y ve el historial de revisiones y observaciones para mejorar la pregunta y cambiar de estado, con notificaciones | Cumple (adelantada) | HU-06 en el documento 01; verificada en el recorrido funcional |
| Autenticación y autorización con JWT y un sistema externo como Keycloak | Cumple | Implementado y documentado (08 y 10) |
| Arquitectura hexagonal con DDD en un microservicio con reglas de dominio | Parcial | Todos los servicios separan `domain`, `application` (puertos) e `infrastructure` (adaptadores) y lo vigila ArchUnit. Falta el modelado formal en DDD de un servicio (agregados, objetos de valor, servicios de dominio) y su documento |
| Dockerizar los microservicios con Docker Compose | Cumple | Verificado |
| Documento final en PDF y README del repositorio como carta de presentación | Parcial | El README raíz ya presenta el proyecto; falta el PDF final |
| Documento: historias de las tres iteraciones, planificación de los Sprint 1, 2 y 3, escenarios de modificabilidad y escalabilidad, C4 de la 2.ª y 3.ª iteración con seguridad, patrones, URL del repositorio | Parcial | Falta la captura del Sprint 3 y el escenario formal de modificabilidad |
| Sustentación presencial, como empresarios, con preguntas técnicas | Pendiente | |

## 5. Estado de las brechas encontradas

| # | Brecha | Estado |
|---|---|---|
| 1 | Solo había un patrón creacional real (Builder) | **Cerrada:** Factory Method implementado en `notification-service` (`CreadorDeAviso`), con pruebas y documentado |
| 2 | Faltaban SOLID y las decisiones de arquitectura del segundo corte | **Cerrada:** documento [10](10-decisiones-y-solid.md) |
| 3 | Documentación desactualizada respecto del código (nombres de clases, ER, SMTP, plantillas, README) | **Cerrada:** documentos 01, 02, 03, 05, 06, 08 y anexos reescritos contra el código; diagramas regenerados |
| 4 | Las notificaciones no llegaban al administrador ni al autor al asignar | **Cerrada:** nuevos avisos (`PREGUNTA_EN_REVISION`, `EVALUACION_RECIBIDA`, `RESULTADO_DE_REVISION`); el administrador y los revisores reciben el resultado |
| 5 | El envío a revisión no pedía confirmación (HU-02) | **Cerrada:** el formulario pide confirmar |
| 6 | El máximo de revisores por defecto era 5 y la historia pide 3 | **Cerrada:** por defecto 3 (configurable) |
| 7 | `.env` sin ignorar; Postman solo para un servicio; sin OpenAPI de `history-service`; contratos de diseño desactualizados | **Cerrada:** `.gitignore`, once colecciones, OpenAPI exportado de los 14 servicios, esquema de eventos actualizado |
| 8 | Prueba de usabilidad | **Material listo**; falta aplicarla con 3 a 5 personas |
| 9 | Hoja de cálculo de la plantilla de historias | **Resuelta:** no se entregó; Jira y la documentación la sustituyen |
| 10 | RF-21 a RF-28 (simulacros y seguimiento) | **Cerrada:** `catalog-service`, `simulation-service`, `statistics-service` y `report-service`, con roles Estudiante y Docente, pruebas, documentación y las pantallas del frontend (simulacros, presentación, progreso y reporte del grupo) |
| 11 | Entregables finales: PDF formal con portada e introducción, video de YouTube, sprint 3, DDD formal en un servicio, sustentación presencial | **Pendientes**, previstos para el cierre |
