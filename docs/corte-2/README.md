# Documentación del segundo corte

Documentación de la **segunda iteración** del Sistema de Banco de Preguntas Saber Pro: refactorización
del monolito del primer corte a una solución distribuida con **microservicios orientados a eventos**.
Proyecto de curso de Ingeniería de Software II, Universidad del Cauca, periodo 2026.2.

> Estado: documentación de diseño de la iteración, publicada en la rama `corte-2`. La implementación se publica por
> partes; ver el [README de microservicios](../../microservicios/README.md) para lo disponible hoy.

| Documento | Contenido |
|---|---|
| [01 · Historias de usuario](01-historias-de-usuario.md) | HU-01 a HU-05 de la iteración, con el ajuste de HU-04 y la nueva HU-05 |
| [02 · Contextos delimitados y eventos](02-contextos-y-eventos.md) | Contextos propuestos, servicios, catálogo de eventos y flujos |
| [03 · Arquitectura (C4 y UML)](03-arquitectura-c4-uml.md) | Modelo C4, secuencias, estados, modelo de datos por servicio y despliegue |
| [04 · Escalabilidad](04-escalabilidad.md) | Escenario de calidad y plan de prueba de carga |
| [05 · Patrones de diseño](05-patrones.md) | Los 6 patrones GoF y dónde se aplican |
| [06 · Prototipos](06-prototipos.md) | Prototipos web de alta fidelidad de las pantallas de la iteración |
| [07 · Planificación del Sprint 2](07-planificacion-sprint-2.md) | Historias y tareas del sprint en Jira |
| [Anexo · Plan de arranque](anexos/plan-de-arranque.md) | Estructura del repositorio, puertos, eventos, endpoints y orden de construcción |
| [Anexo · Notificaciones y correos](anexos/notificaciones-y-correos.md) | Quién recibe qué y plantillas de correo |
| [Anexo · Datos semilla](anexos/datos-semilla.md) | Usuarios y preguntas precargados para desarrollo y demostración |
| [Anexo · Plan de pruebas](anexos/plan-de-pruebas.md) | Qué se prueba en cada servicio |
| [API: contratos y colecciones](api/README.md) | OpenAPI de los servicios, esquema JSON de los eventos y colecciones de Postman |

Documentos que aplican a todo el proyecto, en [`../general`](../general/README.md): las
[consultas a los docentes](../general/consultas-a-docentes.md) con sus respuestas, la
[reutilización del monolito](../general/reutilizacion-del-monolito.md) y los enunciados de la materia.

Los diagramas están escritos en [Mermaid](https://mermaid.js.org/), que GitHub muestra directamente, y además
exportados como imagen en [`img/diagramas`](img/diagramas/).

## Decisiones tomadas

| Tema | Decisión |
|---|---|
| Estilo | Microservicios con API Gateway y comunicación por eventos (RabbitMQ) |
| Lenguaje y marco | Java 21 con Spring Boot (obligatorio según Mage) |
| Datos | Una instancia de PostgreSQL por microservicio |
| Frontend | Aplicación web separada en Angular, que solo habla con el gateway. Libardo aceptó también Swing o JavaFX, pero el foco es el backend |
| Seguridad | Usuarios precargados y autorización validada en el gateway y en cada servicio; Keycloak con pantalla de login en el frontend se completa en el corte 3 |
| Revisión | De 1 a 3 revisores, asigna el administrador, aprobación por unanimidad |
| Notificaciones | Correo simulado por eventos (publicador y suscriptor) más bandeja en el sistema, para el administrador, los revisores y el autor. Libardo rectificó la indicación de correo real |
| Historial | Un microservicio lleva el historial de revisiones con fecha y responsable; la auditoría de accesos no es requisito |
| Publicación | La pregunta aprobada la publica el administrador |
| Hexagonal y DDD | En un solo servicio, en el corte 3 |
| Monolito | Se conserva en la carpeta `monolito/` y en la rama `corte-1`; el trabajo nuevo va en `corte-2` |

## Pendiente

| Tema | Estado |
|---|---|
| Estado Archivada: quién la archiva y cuándo | Sin respuesta de los docentes; fuera del alcance por ahora |
| Escalabilidad y tolerancia a fallos | Fuera de alcance según Libardo; se miden como valor agregado |

El detalle de lo que respondió cada docente, y qué decidimos cuando difieren, está en [consultas a los docentes](../general/consultas-a-docentes.md).
