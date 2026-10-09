# API: contratos y colecciones

| Carpeta | Contenido |
|---|---|
| [`openapi/`](openapi/) | Contratos OpenAPI de los servicios: preguntas, revisión y usuarios con notificaciones |
| [`eventos/`](eventos/) | Esquema JSON de los eventos que se publican en RabbitMQ |
| [`postman/`](postman/) | Colecciones de Postman para probar cada servicio a mano |

Cada servicio publica además su propio Swagger en `/swagger-ui.html` cuando está en ejecución. Los contratos de esta
carpeta son el diseño acordado; las colecciones de Postman se agregan a medida que se publica cada servicio.

## Colecciones disponibles

| Colección | Servicio | Cómo usarla |
|---|---|---|
| [`question-service.postman_collection.json`](postman/question-service.postman_collection.json) | `question-service` | Importarla en Postman; las variables `baseUrl`, `userId` y `roles` cambian el usuario con el que se prueba |
