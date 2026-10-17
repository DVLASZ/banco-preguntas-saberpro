# Servicios de negocio

Cada carpeta es un microservicio independiente con su propia base de datos (PostgreSQL), su propia imagen de
Docker y su propio Swagger. Los servicios no se llaman entre sí: se comunican por eventos en RabbitMQ.

| Servicio | Función | Estado |
|---|---|---|
| [`question-service`](question-service/) | Preguntas, validación estructural y ciclo de vida (HU-01 a HU-03) | Publicado |
| [`review-service`](review-service/) | Asignación de revisores, rondas de evaluación y decisión por unanimidad | Publicado |
| [`notification-service`](notification-service/) | Bandeja de notificaciones y correo simulado | Publicado |
| `history-service` | Historial de revisiones por pregunta | Por publicar |
| [`user-service`](user-service/) | Usuarios y roles | Publicado |

Todos comparten la biblioteca de [`../plataforma/bp-platform`](../plataforma/bp-platform/) y siguen la estructura
descrita en [convenciones](../../docs/general/convenciones.md).
