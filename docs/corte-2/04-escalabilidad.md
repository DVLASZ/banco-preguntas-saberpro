# 04 · Escalabilidad

## Atributos de calidad relevantes

Para esta iteración el atributo principal es la **escalabilidad** (RNF-17 y RNF-18). La modificabilidad
del primer corte se mantiene gracias a la separación en servicios y a la comunicación por eventos.

## Escenario de calidad

| Elemento | Contenido |
|---|---|
| Fuente | Estudiantes, autores, revisores y administradores usando el sistema al mismo tiempo |
| Estímulo | 600 usuarios concurrentes (60 % autores, 25 % revisores, 15 % administradores) consultando y escribiendo a través del gateway |
| Entorno | Operación normal bajo carga pico, sistema completo desplegado con Docker Compose en una sola máquina (16 CPU, 11,4 GB para Docker) |
| Artefacto | `edge`, `api-gateway` y los servicios de negocio |
| Respuesta | Se agregan réplicas del servicio saturado detrás del gateway sin modificar la lógica de negocio; las notificaciones y la trazabilidad se procesan de forma asíncrona y no bloquean al usuario |
| Medida | Tiempo de respuesta (p95), tasa de error y peticiones por segundo |
| Resultado esperado | Con 500 usuarios concurrentes, p95 de las consultas menor a 3 s (RNF-17) y error menor al 1 % |

**Por qué la arquitectura lo permite:** los servicios no guardan estado de sesión (el token es autocontenido),
cada uno tiene su base de datos y el trabajo lento (correo, trazabilidad) sale del camino de la petición mediante
eventos. Las réplicas se registran solas en Eureka y el gateway las reparte.

## La prueba

Herramienta: **k6** en su imagen de Docker (`microservicios/pruebas-de-carga/escalabilidad.js`), con tokens reales de
Keycloak (se piden una vez al inicio, para medir a los servicios y no a la pantalla de login). Cada usuario virtual
sigue el recorrido de su rol: los autores listan sus preguntas, crean borradores, consultan el historial y las
notificaciones; los revisores consultan su bandeja; los administradores consultan pendientes, todas las preguntas,
revisores y auditoría. Umbrales: `p95 < 1500 ms`, error `< 1 %`, comprobaciones `> 99 %`.

```bash
docker run --rm -i -e PW=<clave de prueba> -e VUS=600 -e HOLD=1m \
  -e BASE=http://host.docker.internal:8080 -e KC=http://host.docker.internal:8180 \
  grafana/k6 run - < pruebas-de-carga/escalabilidad.js
```

`PAUSA` fija el tiempo de lectura entre acciones: `1` (por defecto) imita a una persona; `0.05` convierte la prueba
en una de estrés.

## Resultados (8 de octubre de 2026, una réplica de cada servicio)

| Corrida | Usuarios | Pausa | Peticiones | Peticiones/s | p95 | p99 | Error |
|---|---|---|---|---|---|---|---|
| Ritmo humano | 600 | 1 s | 33 156 | 223 | **18 ms** | 28 ms | 0 % |
| Estrés | 600 | 0,05 s | 159 028 | 1 128 | **692 ms** | 962 ms | 0 % |

Con 600 usuarios a ritmo humano se supera con mucho el objetivo (3 s). Con la pausa reducida 20 veces, el sistema
sostiene 1 128 peticiones por segundo sin un solo error y con p95 por debajo de 0,7 s, es decir: aún no se llega al
límite de la máquina con 600 usuarios; el cuello de botella pasa a ser el CPU compartido.

### Resultados con los 14 servicios (19 de octubre de 2026, una réplica de cada servicio)

Misma prueba, tras dividir el sistema en 14 servicios (la ruta de las consultas ahora pasa por `lifecycle`, `assignment`,
`email`, `audit` y los demás servicios pequeños).

| Corrida | Usuarios | Pausa | Peticiones | Peticiones/s | p95 | p99 | Error |
|---|---|---|---|---|---|---|---|
| Ritmo humano | 600 | 1 s | 32 956 | 223 | **52 ms** | 130 ms | 0 % |
| Estrés | 600 | 0,05 s | 140 474 | 998 | **752 ms** | 996 ms | 0 % |

Se cumplen los tres umbrales (p95 menor de 1500 ms, error menor del 1 % y comprobaciones por encima del 99 %; fallaron 3 de
32 948 comprobaciones del ritmo humano y 4 de 140 466 en estrés, todas en los primeros segundos del calentamiento). El p95 a ritmo
humano subió de 18 a 52 ms respecto a siete servicios, y el rendimiento en estrés bajó de 1 128 a 998 peticiones por segundo:
es el costo esperado de tener 14 JVM compartiendo la misma máquina y de las tres llamadas síncronas entre servicios.

### Qué se encontró y se corrigió al medir

| Hallazgo | Causa | Corrección |
|---|---|---|
| Consultas de preguntas muy lentas con muchas filas | El filtro opcional con `OR … IS NULL` impedía usar índices | Búsqueda dinámica con consulta construida según los filtros + índices `(autor, fecha, id)`, `(fecha, id)` y `(estado, fecha, id)` (migración `question V3`) |
| El gateway rechazaba llamadas sanas con carga | El *bulkhead* por defecto admite 25 llamadas simultáneas | Tope de 1000 con espera máxima de 200 ms |
| Errores por agotamiento de puertos en `edge` | Una conexión nueva por petición hacia el gateway | Upstream con `keepalive` |
| `edge` descartaba réplicas sanas | Tomaba los `503` del circuit breaker como réplica caída | Solo `error` y `timeout` activan el reintento; `max_fails=3 fail_timeout=3s` |

### Réplicas: ¿más réplicas rinden más?

Se comprobó que añadir réplicas **funciona** (Eureka las descubre, el gateway las reparte, y al detener una el
sistema sigue respondiendo sin errores), pero en esta máquina **no mejora** el rendimiento: los contenedores
comparten los mismos 16 CPU, así que más réplicas solo reparten el mismo cómputo y añaden cambios de contexto.
La escalabilidad horizontal tiene sentido cuando cada réplica dispone de recursos propios (varios nodos). Es un
resultado honesto de la medición y se explica en la sustentación; el diseño (servicios sin estado, base por
servicio, balanceo y descubrimiento) es el que permite ese escalado en un entorno con más hardware.

## Tolerancia a fallos (extra)

Verificado a mano: detener una réplica del gateway o de un servicio no interrumpe el servicio; detener RabbitMQ no
impide guardar ni enviar a revisión (Outbox, ver [08](08-implementacion-seguridad-y-resiliencia.md)); un servicio
caído devuelve `503` inmediato por el circuit breaker en lugar de dejar al usuario esperando.
