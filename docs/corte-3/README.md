# Documentación del tercer corte

Tercera iteración del Sistema de Banco de Preguntas Saber Pro. Los requisitos oficiales están en
[`EntregablesTercerCorte2026.2-IngSoft2.docx`](../general/enunciados/EntregablesTercerCorte2026.2-IngSoft2.docx).

## Alcance del corte

| Requisito | Qué pide | Estado |
|---|---|---|
| HU-06 | El autor lista y ve el historial de revisiones y sus observaciones para sacar una versión mejorada | Hecha. El autor ve las rondas con las observaciones de cada revisor y la línea de tiempo de la pregunta. Puede reabrir la rechazada, corregirla y reenviarla |
| Autenticación y autorización con JWT | Sistema externo (Keycloak) y roles | Hecho. Ver [seguridad](seguridad.md) |
| Arquitectura hexagonal con DDD | En un microservicio con reglas de dominio | Hecho en `evaluation-service`. Ver [hexagonal y DDD](hexagonal-y-ddd.md) |
| Dockerización | Todo el sistema con Docker Compose | Hecho. Un solo comando levanta bases, broker, Keycloak, servicios, gateway y frontend |
| Pruebas unitarias del dominio | Pruebas a todas las clases del dominio | Hecho. Ver [cobertura de pruebas](cobertura-de-pruebas.md) |
| Simulacros y seguimiento | RF-21 a RF-28, con los roles Estudiante y Docente | Hecho, con pantallas. Ver [simulacros y seguimiento](simulacros-y-seguimiento.md) |
| Documento de arquitectura | Documento final y repositorio como carta de presentación | Hecho. [Documento final de arquitectura](Documento-Arquitectura-Final.md), con C4 de la segunda y la tercera iteración, Docker Compose con volúmenes, seguridad y el avance en Git y Jira. El README raíz presenta el proyecto |
| Prueba de usabilidad | Thinking aloud con personas reales | El enunciado del 12 de noviembre ya no la pide. El material sigue en [el anexo del corte 2](../corte-2/anexos/prueba-de-usabilidad.md) |
| Sustentación | Presencial: 10 minutos de demostración (seguridad, escalabilidad y modificabilidad) y 5 de preguntas técnicas | El guion se prepara aparte del repositorio. Falta ensayar |

## Lo que se decidió con los docentes para este corte

Está en [consultas a los docentes](../general/consultas-a-docentes.md): hexagonal y DDD en un solo servicio, no se
piden patrones GoF adicionales, el gateway actúa como redireccionador sencillo y no se exige auditoría.
El enunciado del 12 de noviembre sí pide hablar de escalabilidad en la sustentación.

## Documentos de este corte

| Documento | Contenido |
|---|---|
| [Hexagonal y DDD](hexagonal-y-ddd.md) | Diseño de `evaluation-service`: lenguaje ubicuo, agregado, objetos de valor, puertos y adaptadores |
| [Seguridad](seguridad.md) | Inicio de sesión con Keycloak, roles y las dos barreras de autorización |
| [Simulacros y seguimiento](simulacros-y-seguimiento.md) | Los dos servicios nuevos, los eventos que los conectan y sus pantallas |
| [Cobertura de pruebas](cobertura-de-pruebas.md) | Cómo medir la cobertura con JaCoCo y qué resultado dio por servicio |
| [Modificabilidad](modificabilidad.md) | Seis escenarios de calidad con su fuente, estímulo, respuesta y medida |
| [Patrones y principios](patrones-y-principios.md) | Qué cambió en patrones y SOLID, y los patrones de arquitectura y dominio |
| [Documento final de arquitectura](Documento-Arquitectura-Final.md) | Las tres iteraciones, C4 con iconografía, Docker Compose con volúmenes, seguridad, escenarios de calidad y el avance en Git y Jira |
