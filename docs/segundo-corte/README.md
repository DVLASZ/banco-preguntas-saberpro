# Documentación del segundo corte

Documentación de la **segunda iteración** del Sistema de Banco de Preguntas Saber Pro: refactorización
del monolito del primer corte a una solución distribuida con **microservicios orientados a eventos**.
Proyecto de curso de Ingeniería de Software II, Universidad del Cauca, periodo 2026.2.

> Estado: borrador de trabajo en la rama `corte-2`. Los puntos marcados como **pendiente** esperan
> confirmación de los docentes y se resumen al final de este documento.

| Documento | Contenido |
|---|---|
| [01 · Historias de usuario](01-historias-de-usuario.md) | HU-01 a HU-05 de la iteración, con el ajuste de HU-04 y la nueva HU-05 |
| [02 · Contextos delimitados y eventos](02-contextos-y-eventos.md) | Contextos propuestos, servicios, catálogo de eventos y flujos |
| [03 · Arquitectura (C4 y UML)](03-arquitectura-c4-uml.md) | Modelo C4, secuencias, estados, modelo de datos por servicio y despliegue |
| [04 · Escalabilidad](04-escalabilidad.md) | Escenario de calidad y plan de prueba de carga |
| [05 · Patrones de diseño](05-patrones.md) | Los 6 patrones GoF y dónde se aplican |
| [06 · Prototipos](06-prototipos.md) | Prototipos web de alta fidelidad de las pantallas de la iteración |
| [07 · Planificación del Sprint 2](07-planificacion-sprint-2.md) | Historias y tareas del sprint en Jira |
| [Anexo · Consultas a los docentes](anexos/consultas-a-docentes.md) | Preguntas de alcance técnico y respuestas del profesor Mage |
| [Anexo · Plan de arranque](anexos/plan-de-arranque.md) | Estructura del repositorio, puertos, eventos, endpoints y orden de construcción |
| [Anexo · Notificaciones y correos](anexos/notificaciones-y-correos.md) | Quién recibe qué y plantillas de correo |
| [Anexo · Datos semilla](anexos/datos-semilla.md) | Usuarios y preguntas precargados para desarrollo y demostración |
| [Anexo · Plan de pruebas](anexos/plan-de-pruebas.md) | Qué se prueba en cada servicio |
| [Contratos de la API y de los eventos](contratos/) | OpenAPI de los servicios y esquema JSON de los eventos |

Los diagramas están escritos en [Mermaid](https://mermaid.js.org/), que GitHub muestra directamente, y además
exportados como imagen en [`img/diagramas`](img/diagramas/).

## Decisiones tomadas

| Tema | Decisión |
|---|---|
| Estilo | Microservicios con API Gateway y comunicación por eventos (RabbitMQ) |
| Lenguaje y marco | Java 21 con Spring Boot (obligatorio según el docente) |
| Datos | Una instancia de PostgreSQL por microservicio |
| Frontend | Aplicación web separada en Angular o React, que solo habla con el gateway |
| Seguridad (corte 2) | Autenticación mínima, usuarios precargados y autorización centralizada en el gateway; Keycloak llega en el corte 3 |
| Revisión | De 1 a 3 revisores, asigna el administrador, aprobación por unanimidad |
| Notificaciones | Correo real por SMTP al administrador, los revisores y el autor |
| Monolito | Se conserva en la rama `corte-1`; el trabajo nuevo va en `corte-2` |

## Pendiente de confirmar

| Tema | Por qué importa |
|---|---|
| Quién publica y archiva una pregunta | Completa la máquina de estados |
| Si el primer rechazo cierra la revisión o se espera a todos | Regla de `review-service` |
| Angular o React | Elección del equipo |
| Cómo se demuestra la escalabilidad (réplicas, herramienta) | Plan de la prueba de carga |
| Si el correo real es obligatorio o puede simularse | Esfuerzo de `notification-service` |
