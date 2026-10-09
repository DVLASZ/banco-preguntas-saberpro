# 04 · Escalabilidad

## Atributos de calidad relevantes

Para esta iteración el atributo principal es la **escalabilidad** (RNF-17 y RNF-18). La modificabilidad
del primer corte se mantiene gracias a la separación en servicios y a la comunicación por eventos.

## Escenario de calidad

| Elemento | Contenido |
|---|---|
| Fuente | Estudiantes, autores, revisores y administradores usando el sistema al mismo tiempo |
| Estímulo | 500 usuarios concurrentes (se llevará la prueba por encima de 500 hasta el límite de la máquina) consultando y creando preguntas, con evaluaciones simultáneas |
| Entorno | Operación normal bajo carga pico, sistema desplegado con Docker Compose |
| Artefacto | `api-gateway`, `question-service` y `review-service` |
| Respuesta | Se agregan réplicas del servicio saturado detrás del gateway sin modificar la lógica de negocio; las notificaciones se procesan de forma asíncrona y no bloquean al usuario |
| Medida | Tiempo de respuesta, tasa de error y peticiones por segundo |
| Resultado esperado | Con 500 usuarios concurrentes, tiempo de respuesta de las consultas menor a 3 s (RNF-17) y error menor al 1 %; el rendimiento aumenta al pasar de 1 a más réplicas |

**Por qué la arquitectura lo permite:** los servicios no guardan estado de sesión, cada uno tiene su base de
datos y el trabajo lento (correo, trazabilidad) sale del camino de la petición mediante eventos.

## Plan de la prueba de carga

Herramienta: **k6**, ejecutado con su imagen de Docker, sin instalación.

| Escenario | Qué hace | Servicio |
|---|---|---|
| Consulta | Inicia sesión y lista preguntas con filtros y paginación | question-service |
| Creación | Crea preguntas válidas | question-service |
| Evaluación | Revisores evalúan preguntas asignadas | review-service |

Perfil de carga por etapas: 50, 100, 250, 500, 750 y 1000 usuarios virtuales, manteniendo cada etapa un
tiempo fijo. Se registra el punto en que se rompen los umbrales (`p95 < 3 s`, error `< 1 %`).

| Corrida | Configuración | Objetivo |
|---|---|---|
| A | 1 réplica de `question-service` | Línea base |
| B | 3 réplicas detrás del gateway | Medir la ganancia |
| C | Carga creciente hasta saturar | Anotar el límite de la máquina, como pidió el docente |

Balanceo entre réplicas: opciones a decidir al implementar, entre DNS de Docker con reparto rotatorio o un
balanceador dedicado delante del servicio. *Pendiente: cómo espera el docente que se demuestre.*

Resultados de la prueba: se completarán cuando exista el sistema.

| Corrida | Usuarios concurrentes | p95 (ms) | Error (%) | Peticiones/s |
|---|---|---|---|---|
| A | — | — | — | — |
| B | — | — | — | — |
| C | — | — | — | — |
