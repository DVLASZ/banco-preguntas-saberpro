# Colecciones de Postman

Una colección por cada servicio que el gateway expone, con los casos de éxito y de error de cada historia, y un entorno común.
`validation-service` y `lifecycle-service` son internos (solo los llama `question-service`), así que no tienen colección: se ejercitan
al crear, enviar, reabrir y publicar preguntas con la de `question-service`.

| Colección | Peticiones |
|---|---|
| [`user-service`](user-service.postman_collection.json) | 16 |
| [`question-service`](question-service.postman_collection.json) | 23 |
| [`assignment-evaluation-services`](assignment-evaluation-services.postman_collection.json) | 23 |
| [`notification-service`](notification-service.postman_collection.json) | 15 |
| [`email-service`](email-service.postman_collection.json) | 9 |
| [`history-service`](history-service.postman_collection.json) | 12 |
| [`audit-service`](audit-service.postman_collection.json) | 9 |
| [`catalog-service`](catalog-service.postman_collection.json) | 16 |
| [`simulation-service`](simulation-service.postman_collection.json) | 31 |
| [`statistics-service`](statistics-service.postman_collection.json) | 27 |
| [`report-service`](report-service.postman_collection.json) | 19 |
| **Total** | **200** peticiones con 314 verificaciones (las de «Sesión» y «Preparación» cuentan en cada colección) |

El entorno [`SaberPro_Local`](SaberPro_Local.postman_environment.json) define `baseUrl` (`http://localhost:8080`, el gateway),
`keycloakUrl` (`http://localhost:8180`) y `password` (la clave de los usuarios de demostración). Cada colección empieza con una
carpeta **0 · Sesión** que obtiene los tokens de los usuarios que necesita; no hay que copiar ningún token a mano.

## Ejecutar

Con la pila levantada (`docker compose up -d`, desde `microservicios/`):

- **Postman:** *Import* de la colección y del entorno, elegir `SaberPro_Local` y *Run collection*.
- **Por consola:**

```bash
npx newman run docs/corte-2/api/postman/question-service.postman_collection.json \
  -e docs/corte-2/api/postman/SaberPro_Local.postman_environment.json
```

Las colecciones se pueden repetir sin reiniciar nada. Ver la nota de verificación en [`../README.md`](../README.md).
