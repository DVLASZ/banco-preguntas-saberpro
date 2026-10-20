# Documentación del tercer corte

Tercera iteración del Sistema de Banco de Preguntas Saber Pro. Los requisitos oficiales están en
[`EntregablesTercerCorte2026.2-IngSoft2.docx`](../general/enunciados/EntregablesTercerCorte2026.2-IngSoft2.docx).
Esta carpeta se llena a medida que se avanza; hoy recoge el alcance y el estado.

> **Adelantado:** los simulacros y el seguimiento académico (RF-21 a RF-28), que los docentes ubicaron en este corte, ya
> están construidos en el backend: ver [simulacros y seguimiento](simulacros-y-seguimiento.md).

## Alcance del corte

| Requisito | Detalle | Estado |
|---|---|---|
| HU-06 | El autor lista y ve el historial de revisiones y sus observaciones para sacar una versión mejorada | Hecha: el autor ve las rondas con las observaciones de cada revisor, la línea de tiempo de la pregunta, puede reabrir la rechazada, corregirla y reenviarla (verificado de punta a punta en Docker) |
| Autenticación y autorización con JWT | Sistema externo (Keycloak); usuarios y roles pueden gestionarse por Postman | Keycloak y validación de JWT en el gateway y en cada servicio ya funcionan (adelantado desde el corte 2) |
| Arquitectura hexagonal con DDD | En **un** microservicio con reglas de dominio (revisión o gestión de preguntas): agregados, objetos de valor, servicios de dominio y repositorios | Los servicios ya separan dominio, puertos y adaptadores (vigilado con ArchUnit); falta el modelado formal en DDD de un servicio y su documento |
| Dockerización | Todo el sistema con Docker Compose | Construido: un solo comando levanta bases, broker, Keycloak, servicios, gateway y frontend |
| Documento de arquitectura | Documento final en PDF y repositorio que sirva de carta de presentación | Pendiente el PDF; el README raíz ya presenta el proyecto y se arma con los documentos de cada corte, incluidos [las decisiones y SOLID](../corte-2/10-decisiones-y-solid.md) |
| Sustentación | Presencial, de 10 minutos más preguntas, vendiendo el producto a los docentes | Pendiente |

## Lo que se decidió con los docentes para este corte

Está en [consultas a los docentes](../general/consultas-a-docentes.md): hexagonal y DDD en un solo servicio, no se
piden patrones GoF adicionales, el gateway actúa como redireccionador sencillo y no se exige auditoría ni
escalabilidad.

## Documentos previstos

| Documento | Contenido |
|---|---|
| Hexagonal y DDD | Diseño del servicio elegido: agregados, objetos de valor, puertos y adaptadores |
| Seguridad | Flujo de autenticación con Keycloak, roles y autorización redundante (hoy en el documento 08 del corte 2) |
| Documento de arquitectura final | Unión de los tres cortes, con C4, escenarios de calidad y patrones |
