# 05 · Patrones de diseño

La rúbrica pide al menos 6 patrones GoF: 2 creacionales, 2 estructurales y 2 de comportamiento. La
selección parte de los patrones ya presentes en el primer corte y agrega uno estructural nuevo.

| Tipo | Patrón | Dónde se aplica | Para qué | Origen |
|---|---|---|---|---|
| Creacional | **Builder** | Construcción de `Question` y de los eventos con su sobre común | Armar objetos con muchos campos opcionales de forma legible | Primer corte |
| Creacional | **Factory Method** | Creación de eventos de dominio según el tipo y de las reglas de validación | Que el cliente no conozca las clases concretas | Primer corte |
| Estructural | **Adapter** | Adaptadores de persistencia (`JpaQuestionRepository`, `JpaReviewRepository`), publicación en RabbitMQ y envío de correo (`SmtpEmailSender` detrás del puerto `EmailSender`) | Aislar el dominio de la tecnología externa | Primer corte |
| Estructural | **Facade** | `api-gateway` como punto único de entrada sobre los servicios | Ocultar la topología interna y centralizar la autorización | **Nuevo** |
| Comportamiento | **Strategy** | `DecisionPolicy` en `review-service` (hoy unanimidad; mañana mayoría) | Cambiar la regla de decisión sin tocar el flujo | Primer corte (política nueva) |
| Comportamiento | **Observer** | Publicación y suscripción de eventos por RabbitMQ entre servicios | Notificar cambios sin acoplar productor y consumidores | Primer corte, ahora distribuido |

## Patrones de respaldo

| Patrón | Dónde |
|---|---|
| State | `EstadoPregunta` y sus transiciones válidas |
| Template Method | Validaciones de la pregunta que comparten esqueleto |
| Chain of Responsibility | Encadenamiento de reglas de validación estructural |

## Patrones de arquitectura de microservicios

| Patrón | Dónde |
|---|---|
| API Gateway | `api-gateway` (Spring Cloud Gateway): entrada única, autorización, CORS |
| Service Registry y Discovery | Eureka; las réplicas se registran solas |
| Base de datos por servicio | Cinco instancias de PostgreSQL |
| Arquitectura orientada a eventos | RabbitMQ, exchange *topic* `bancopreguntas.events` |
| Consumidor idempotente | Tabla `processed_event` en cada consumidor |
| **Transactional Outbox** | question, review y user: el evento se guarda con el cambio y un relevo lo publica |
| Circuit Breaker, Retry y Bulkhead | Gateway (Resilience4J) |
| Dead Letter Queue | `bancopreguntas.dead-letter`, por política de RabbitMQ |
| Seguridad con JWT | Keycloak, validado en gateway y servicios |
| Balanceo de carga | `edge` (nginx) delante de las réplicas del gateway |
