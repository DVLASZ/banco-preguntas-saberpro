# Contratos OpenAPI

Un archivo por servicio, exportado del propio servicio (`/v3/api-docs`), de modo que **describe exactamente lo que el código
expone**. Se vuelven a generar cuando cambia una API.

| Servicio | Contrato |
|---|---|
| user-service | [`openapi-user-service.json`](openapi-user-service.json) |
| question-service | [`openapi-question-service.json`](openapi-question-service.json) |
| validation-service (interno) | [`openapi-validation-service.json`](openapi-validation-service.json) |
| lifecycle-service (interno) | [`openapi-lifecycle-service.json`](openapi-lifecycle-service.json) |
| assignment-service | [`openapi-assignment-service.json`](openapi-assignment-service.json) |
| evaluation-service | [`openapi-evaluation-service.json`](openapi-evaluation-service.json) |
| notification-service | [`openapi-notification-service.json`](openapi-notification-service.json) |
| email-service | [`openapi-email-service.json`](openapi-email-service.json) |
| history-service | [`openapi-history-service.json`](openapi-history-service.json) |
| audit-service | [`openapi-audit-service.json`](openapi-audit-service.json) |

## Regenerarlos

Cada servicio trae una prueba, `ContratoOpenApiTest`, que arranca el servicio con una base de datos embebida y guarda lo que sirve
`/v3/api-docs`. Solo corre cuando se le pide la carpeta de destino (sin la propiedad, las pruebas normales la omiten):

```bash
cd microservicios
./mvnw -q -pl servicios/lifecycle-service -am test -Dtest=ContratoOpenApiTest -Dsurefire.failIfNoSpecifiedTests=false \
  -Dopenapi.export=../docs/corte-2/api/openapi
```

Con la pila levantada también se ven en `http://localhost:8080/swagger-ui.html` (Swagger agregado del gateway). Para abrirlos en
Postman: *Import* y elegir el archivo.
