# Plataforma

Código común que usan todos los servicios, para no repetirlo.

| Módulo | Contenido |
|---|---|
| [`bp-platform`](bp-platform/) | Sobre común de los eventos, publicación fiable con Outbox, lectura de eventos, manejo de errores, identidad del usuario y configuración de OpenAPI |

`bp-platform` se activa solo (autoconfiguración de Spring Boot) cuando un servicio lo declara como dependencia.
