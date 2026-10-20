# Documentación del segundo corte

Documentación de la **segunda iteración** del Sistema de Banco de Preguntas Saber Pro: refactorización
del monolito del primer corte a una solución distribuida con **microservicios orientados a eventos**.
Proyecto de curso de Ingeniería de Software II, Universidad del Cauca, periodo 2026.2.

> Estado: la iteración está implementada y verificada (14 servicios pequeños, gateway, Keycloak, frontend Angular con pantallas de autor, revisor, administrador, estudiante
> y docente, Docker Compose, pruebas de carga y Outbox) y se publica por partes en la rama `corte-2`. Quedan para el corte 3 el rediseño hexagonal con
> DDD de un servicio, el documento final y la sustentación; la historia nueva del corte 3 (HU-06, historial del autor) ya está
> adelantada.

> **Estado del repositorio (octubre de 2026).** Esta documentación describe la arquitectura de **14 microservicios** que se está
> implementando. El código publicado hoy en el repositorio corresponde a la primera distribución, de **7 servicios**; la nueva
> distribución divide varios de ellos para que cada uno tenga una sola responsabilidad (ver la tabla del [README de los servicios](../../microservicios/servicios/README.md)). Ya está implementada y verificada de punta a punta en local y el código se irá publicando por
> partes; mientras tanto, lo que no está en el repositorio aparece en esta documentación como texto, sin enlace.

| Documento | Contenido |
|---|---|
| [01 · Historias de usuario](01-historias-de-usuario.md) | HU-01 a HU-05 de la iteración, con el ajuste de HU-04 y la nueva HU-05, más HU-06 adelantada del tercer corte |
| [02 · Contextos delimitados y eventos](02-contextos-y-eventos.md) | Contextos propuestos, servicios, catálogo de eventos y flujos |
| [03 · Arquitectura (C4 y UML)](03-arquitectura-c4-uml.md) | Modelo C4, secuencias, estados, modelo de datos por servicio y despliegue |
| [04 · Escalabilidad](04-escalabilidad.md) | Escenario de calidad y plan de prueba de carga |
| [05 · Patrones de diseño](05-patrones.md) | Los patrones GoF (Builder, Factory Method, Adapter, Facade, Decorator, Strategy, Observer, State y Template Method) y dónde se aplican |
| [06 · Prototipos](06-prototipos.md) | Prototipos web de alta fidelidad de las pantallas de la iteración |
| [07 · Planificación del Sprint 2](07-planificacion-sprint-2.md) | Historias y tareas del sprint en Jira |
| [08 · Implementación, seguridad y resiliencia](08-implementacion-seguridad-y-resiliencia.md) | Topología desplegada, Keycloak y roles, circuit breaker, Outbox, mensajes muertos y pruebas |
| [09 · Trazabilidad de requisitos](09-trazabilidad-de-requisitos.md) | Cada requisito de los enunciados contra lo construido, con su evidencia |
| [10 · Decisiones de arquitectura y SOLID](10-decisiones-y-solid.md) | Por qué cada decisión, alternativas descartadas y SOLID en el código |
| [Anexo · Estructura y reglas](anexos/plan-de-arranque.md) | Estructura del repositorio, puertos, roles, reglas de negocio y orden de construcción |
| [Anexo · Prueba de usabilidad](anexos/prueba-de-usabilidad.md) | Guion, tareas, hojas de observación y cuestionario SUS (pendiente de aplicar) |
| [Anexo · Notificaciones y correos](anexos/notificaciones-y-correos.md) | Quién recibe qué, cómo se redactan los avisos y el correo simulado |
| [Anexo · Datos semilla](anexos/datos-semilla.md) | Usuarios y roles de demostración en Keycloak |
| [Anexo · Plan de pruebas](anexos/plan-de-pruebas.md) | Qué se prueba en cada servicio y cuántas pruebas hay |
| [API: contratos y colecciones](api/README.md) | OpenAPI de los servicios, esquema JSON de los eventos y colecciones de Postman |

Documentos que aplican a todo el proyecto, en [`../general`](../general/README.md): las
[consultas a los docentes](../general/consultas-a-docentes.md) con sus respuestas, la
[reutilización del monolito](../general/reutilizacion-del-monolito.md) y los enunciados de la materia.

Los diagramas están escritos en [Mermaid](https://mermaid.js.org/), que GitHub muestra directamente, y además
exportados como imagen en [`img/diagramas`](img/diagramas/).

## Decisiones tomadas

| Tema | Decisión |
|---|---|
| Estilo | 14 microservicios pequeños con API Gateway y comunicación por eventos (RabbitMQ); solo tres llamadas REST síncronas entre servicios ([decisiones](10-decisiones-y-solid.md)) |
| Lenguaje y marco | Java 21 con Spring Boot (obligatorio según Mage) |
| Datos | Una instancia de PostgreSQL por microservicio |
| Frontend | Aplicación web separada en Angular, que solo habla con el gateway. Libardo aceptó también Swing o JavaFX, pero el foco es el backend |
| Seguridad | Keycloak (OAuth2 con PKCE, JWT) con usuarios precargados; el token se valida en el gateway y otra vez en cada servicio, con autorización por rol en ambos. Los docentes lo ubicaban en el corte 3; se adelantó |
| Revisión | De 1 a 3 revisores, asigna el administrador, aprobación por unanimidad |
| Notificaciones | Correo simulado por eventos (`notification-service` publica el aviso y `email-service` lo envía) más bandeja en el sistema, para el administrador, los revisores y el autor. Libardo rectificó la indicación de correo real |
| Historial | Un microservicio (`history-service`) lleva el historial de revisiones con fecha y responsable. La auditoría de accesos (`audit-service`) no es requisito de los docentes, pero se construyó (inicios de sesión de Keycloak) como valor agregado |
| Publicación | La pregunta aprobada la publica el administrador |
| Hexagonal y DDD | Todos los servicios ya separan dominio, aplicación (puertos) e infraestructura (adaptadores). El rediseño formal con DDD (agregados, objetos de valor, servicios de dominio) se hace en un solo servicio, en el corte 3 |
| Monolito | Se conserva en la carpeta `monolito/` y en la rama `corte-1`; el trabajo nuevo va en `corte-2` |

## Pendiente

| Tema | Estado |
|---|---|
| Estado Archivada: quién la archiva y cuándo | Sin respuesta de los docentes; el estado existe en el modelo pero ninguna operación de la API archiva |
| Prueba de usabilidad | Material listo; falta aplicarla con usuarios ([anexo](anexos/prueba-de-usabilidad.md)) |
| Video y documento formal en PDF | Se hacen al cierre de la iteración |
| Escalabilidad y tolerancia a fallos | Fuera de alcance según Libardo; se miden como valor agregado |

El detalle de lo que respondió cada docente, y qué decidimos cuando difieren, está en [consultas a los docentes](../general/consultas-a-docentes.md).
