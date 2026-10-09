# Visión general

## Qué es

**Sistema para la Gestión, Validación y Administración de un Banco de Preguntas para la Preparación de las Pruebas
Saber Pro.** Proyecto de curso de Ingeniería de Software II, Universidad del Cauca, periodo 2026.2. Permite que los
autores redacten preguntas de selección múltiple con validación estructural, que un administrador asigne de 1 a 3
revisores, que los revisores evalúen por unanimidad y que el administrador publique las preguntas aprobadas.

## Roles

| Rol | Qué hace |
|---|---|
| Autor | Crea, edita en borrador, envía a revisión y corrige las preguntas rechazadas; ve el historial de revisiones |
| Revisor | Ve las preguntas que se le asignaron, deja observaciones y aprueba o rechaza |
| Administrador | Asigna revisores, publica las preguntas aprobadas y consulta la información del sistema |

## Cómo evoluciona por cortes

| Corte | Qué se construye | Dónde está el código | Dónde está la documentación |
|---|---|---|---|
| 1 | Monolito modular de escritorio (Java Swing) con las historias HU-01 a HU-04 | [`monolito/`](../../monolito/README.md) y rama `corte-1` | [`docs/corte-1`](../corte-1/README.md) |
| 2 | Microservicios orientados a eventos (Spring Boot, RabbitMQ, PostgreSQL) con frontend web en Angular | [`microservicios/`](../../microservicios/README.md) y `frontend/` (se publica por partes) | [`docs/corte-2`](../corte-2/README.md) |
| 3 | Seguridad con Keycloak, hexagonal con DDD en un servicio, Docker Compose y documento final | Los mismos de `microservicios/` y `frontend/` | [`docs/corte-3`](../corte-3/README.md) |

Cada corte reutiliza al anterior: el dominio del monolito pasó a los microservicios (ver
[reutilización del monolito](reutilizacion-del-monolito.md)) y el monolito se conserva como referencia.

## Organización del repositorio

```
banco-preguntas-saberpro/
├── README.md             Presentación del proyecto y cómo ejecutarlo
├── docs/                 Toda la documentación
│   ├── general/          Visión, flujo de trabajo, consultas a los docentes, enunciados
│   ├── corte-1/          Documentación del monolito
│   ├── corte-2/          Documentación de los microservicios
│   └── corte-3/          Alcance y documentación del tercer corte
├── monolito/             Corte 1: aplicación de escritorio (Java Swing)
│   ├── app/              Arranque de la aplicación
│   └── modulos/          Módulos Maven: usuarios, preguntas, simulacros, microkernel, revisión y api-rest
├── microservicios/       Cortes 2 y 3
│   ├── plataforma/       Biblioteca común (bp-platform)
│   └── servicios/        question-service (los demás servicios, el gateway, Keycloak y el frontend se publican por partes)
```

## Equipo

- Edward Esteban Dávila Salazar
- Laura Isabel Sánchez Fernández
- Kevin Yesid Castaño Herrera
