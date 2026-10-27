# Servicios de negocio

Cada carpeta es un microservicio **pequeño y con una sola responsabilidad**, con su propia base de datos (PostgreSQL, salvo
`validation-service`, que no guarda nada), su propia imagen de Docker y su propio Swagger. Son **diez servicios**.

La mayor parte de la comunicación es por **eventos en RabbitMQ** (cada servicio publica con Outbox y escucha lo que le interesa). Hay solo
dos llamadas REST síncronas entre servicios, todas con la identidad de quien hizo la petición (la propaga `bp-platform`) para que el
servicio llamado vuelva a comprobar el rol:

| Quién llama | A quién | Para qué |
|---|---|---|
| `question-service` | `validation-service` | Revisar la estructura del contenido al crear, modificar y enviar a revisión |
| `question-service` | `lifecycle-service` | Registrar la pregunta y pedir las transiciones (enviar, reabrir, publicar) y el historial de estados |

`validation-service` y `lifecycle-service` son **internos**: el gateway no tiene rutas hacia ellos.

| Servicio | Función | Puerto |
|---|---|---|
| [`user-service`](user-service/) | Usuarios, roles y eventos de acceso de Keycloak | 8081 |
| [`question-service`](question-service/) | Contenido de las preguntas: crear, modificar, consultar y filtrar (HU-01 a HU-03) | 8082 |
| [`validation-service`](validation-service/) | Reglas de calidad del contenido de una pregunta (sin estado) | 8095 |
| [`lifecycle-service`](lifecycle-service/) | Estados de la pregunta, transiciones, historial y sus eventos | 8094 |
| [`assignment-service`](assignment-service/) | Rondas de revisión, asignación de revisores y bandeja del revisor (HU-04) | 8083 |
| [`evaluation-service`](evaluation-service/) | Evaluación de los revisores, decisión por unanimidad y resultado (HU-05) | 8096 |
| [`notification-service`](notification-service/) | Bandeja de avisos de cada usuario | 8084 |
| [`email-service`](email-service/) | Correo simulado por cada aviso, con reintentos y registro | 8090 |
| [`history-service`](history-service/) | Línea de tiempo de cada pregunta (HU-06) | 8085 |
| [`audit-service`](audit-service/) | Auditoría de acciones e inicios de sesión | 8091 |

`simulation-service` y `tracking-service` siguen en esta carpeta desde el primer diseño. Son de simulacros y seguimiento académico, que
corresponden al tercer corte, y no forman parte de esta entrega.

Todos comparten la biblioteca de [`../plataforma/bp-platform`](../plataforma/bp-platform/) y siguen la estructura
descrita en [convenciones](../../docs/general/convenciones.md). Cómo se llegó a esta división y por qué: [decisiones y SOLID](../../docs/corte-2/10-decisiones-y-solid.md).
