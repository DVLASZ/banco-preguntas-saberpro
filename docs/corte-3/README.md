# Documentación del tercer corte

Tercera iteración del Sistema de Banco de Preguntas Saber Pro. Los requisitos oficiales están en
[`EntregablesTercerCorte2026.2-IngSoft2.docx`](../general/enunciados/EntregablesTercerCorte2026.2-IngSoft2.docx).
Esta carpeta se llena a medida que se avanza; hoy recoge el alcance.

## Alcance del corte

| Requisito | Detalle | Estado |
|---|---|---|
| HU-06 | El autor lista y ve el historial de revisiones y sus observaciones para sacar una versión mejorada | Pendiente |
| Autenticación y autorización con JWT | Sistema externo (Keycloak); usuarios y roles pueden gestionarse por Postman | Pendiente |
| Arquitectura hexagonal con DDD | En **un** microservicio con reglas de dominio (revisión o gestión de preguntas): agregados, objetos de valor, servicios de dominio y repositorios | Pendiente |
| Dockerización | Todo el sistema con Docker Compose | En curso: el compose ya levanta las bases, el broker y `question-service` |
| Documento de arquitectura | Documento final en PDF y repositorio que sirva de carta de presentación | Pendiente; se arma con los documentos de cada corte |
| Sustentación | Presencial, de 10 minutos más preguntas, vendiendo el producto a los docentes | Pendiente |

## Lo que se decidió con los docentes para este corte

Está en [consultas a los docentes](../general/consultas-a-docentes.md): hexagonal y DDD en un solo servicio, no se
piden patrones GoF adicionales, el gateway actúa como redireccionador sencillo y no se exige auditoría ni
escalabilidad.

## Documentos previstos

| Documento | Contenido |
|---|---|
| Hexagonal y DDD | Diseño del servicio elegido: agregados, objetos de valor, puertos y adaptadores |
| Seguridad | Flujo de autenticación con Keycloak, roles y autorización redundante |
| Documento de arquitectura final | Unión de los tres cortes, con C4, escenarios de calidad y patrones |
