# Convenciones

## Nombres

| Elemento | Convención | Ejemplo |
|---|---|---|
| Carpetas del repositorio | Español, en minúsculas y con guiones | `microservicios`, `pruebas-de-carga` |
| Módulos del monolito | Prefijo `modulo-` | `modulo-preguntas` |
| Servicios | Nombre en inglés con sufijo `-service`, porque son el nombre técnico (imagen de Docker, cola, artefacto Maven) | `question-service` |
| Paquetes Java del monolito | `co.unicauca.saberpro...` | `co.unicauca.saberpro.preguntas` |
| Paquetes Java de los servicios | `co.unicauca.bancopreguntas.<servicio>...` | `co.unicauca.bancopreguntas.question` |
| Eventos | `<contexto>.<hecho>`, en inglés y en minúsculas | `question.submitted`, `review.completed` |
| Colas | `<servicio>.q.<evento>` | `review.q.question-submitted` |
| Documentos | Número de orden, guiones y minúsculas | `05-patrones.md` |

Los nombres técnicos de los servicios no se traducen: cambiarlos obligaría a cambiar las imágenes de Docker, las
colas y los artefactos de Maven.

## Idioma

La documentación, los mensajes de commit y los textos de la interfaz están en español. El código (clases, métodos,
variables) sigue el estilo de cada módulo: el monolito y los servicios mezclan nombres de dominio en español con
términos técnicos en inglés.

## Estructura de un servicio

```
domain/           Entidades, reglas y eventos de dominio, sin Spring
application/      Casos de uso y puertos de salida
infrastructure/   Adaptadores: persistencia, mensajería, configuración
api/              Controlador REST, DTO y manejo de errores
```

Una prueba de arquitectura (ArchUnit) vigila que el dominio no dependa de Spring ni de la infraestructura.

## Documentos

- Cada corte tiene su carpeta en `docs/`; lo que aplica a todo el proyecto va en `docs/general/`.
- Los enlaces entre documentos son relativos, para que funcionen en GitHub y en el editor.
- Los diagramas se escriben en Mermaid y, cuando hace falta, se exportan también como imagen.
