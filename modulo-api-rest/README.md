# Módulo API REST (Taller 6)

Microservicio REST construido con **Spring Boot 3.5** y **Spring Data JPA**
que expone el CRUD de las preguntas del banco. Reutiliza el dominio real del
proyecto (`Question`, `QuestionService` de `modulo-preguntas`): no es una
entidad de juguete aparte, y por eso una pregunta creada por la API pasa por
las mismas reglas de validación que una creada desde la aplicación de
escritorio.

El monolito de escritorio (`app`) **no depende** de este módulo, así que el
primer corte (monolito en capas) queda intacto. Este módulo es, además, un
primer paso concreto hacia la migración a microservicios del Corte 2: sale de
`modulo-preguntas` solo lo que ya era su contrato (`QuestionRepository`) y se
le conecta una base de datos por el lado de afuera.

## Arquitectura

```
Cliente (Postman, navegador, otro servicio)
   │  HTTP + JSON
   ▼
controller/   QuestionController      @RestController  /api/questions
   │
service/      IQuestionApiService  →  QuestionApiServiceImpl   (traduce JSON ↔ dominio)
   │
   ▼  usa el servicio de dominio, sin conocer JPA
modulo-preguntas: QuestionService  ──depende de──▶  QuestionRepository (puerto)
                                                          ▲ implementa
repository/  QuestionJpaAdapter  ──usa──▶  QuestionJpaRepository (Spring Data)
                                                │
model/       QuestionEntity (@Entity) ◀── QuestionMapper ──▶ Question (dominio)
                                                │
                                          H2 (archivo ./data/)
```

| Paquete (`co.unicauca.saberpro.api`) | Responsabilidad |
|---|---|
| `controller` | Mapea rutas y verbos HTTP; no contiene lógica |
| `service` | Convierte el JSON de la petición al dominio y delega en `QuestionService` |
| `repository` | Adaptador JPA que implementa el puerto `QuestionRepository` del dominio |
| `model` | `QuestionEntity` (JPA) y `QuestionMapper` (entidad ↔ dominio ↔ respuesta) |
| `dto` | `QuestionRequest` (con validaciones) y `QuestionResponse` |
| `exception` | `GlobalExceptionHandler`: errores del dominio y de validación → respuestas HTTP |
| `config` | Registro del servicio de dominio como bean y datos de ejemplo iniciales |

### Decisiones de diseño

- **La entidad JPA está separada del dominio.** `Question` no tiene
  anotaciones de persistencia; `QuestionEntity` (solo en este módulo) las
  lleva y el adaptador las convierte. Así el dominio no arrastra JPA ni
  Spring (RNF-12: separación entre lógica de negocio y persistencia) y la app
  de escritorio no incorpora Hibernate por accidente. El costo es que un campo
  nuevo del dominio se agrega también en la entidad y el mapper.
- **DELETE archiva, no borra.** RNF-16 prohíbe eliminar físicamente una
  pregunta: `DELETE /api/questions/{id}` la pasa al estado `ARCHIVADA` y
  responde `204`. La pregunta sigue existiendo y se puede consultar.
- **El servidor decide el id y el estado.** El cuerpo de POST/PUT no incluye
  `id` ni `estado`: el id (`P-013`, `P-014`, …) lo genera el adaptador
  continuando el mayor consecutivo guardado, y una pregunta nueva nace siempre
  en `BORRADOR`, como en la aplicación de escritorio.
- **PUT solo cambia el contenido**, nunca el estado, y solo mientras la
  pregunta esté en `BORRADOR` y lo pida su autor (igual que
  `QuestionService.actualizarContenido`).
- **La validación estructural es la misma del dominio** (contexto obligatorio,
  una única pregunta directa, cuatro opciones distintas, sin "todas/ninguna de
  las anteriores", longitud y estructura), aplicada al crear y al actualizar.

## Endpoints

Base: `http://localhost:8080/api/questions`

| Verbo | Ruta | Descripción | Respuesta |
|---|---|---|---|
| GET | `/api/questions` | Lista todas las preguntas | `200` + arreglo JSON |
| GET | `/api/questions/{id}` | Consulta una pregunta | `200` / `404` |
| POST | `/api/questions` | Crea una pregunta | `201` + cabecera `Location` / `400` |
| PUT | `/api/questions/{id}` | Actualiza el contenido de un borrador | `200` / `400` / `403` / `404` / `409` |
| DELETE | `/api/questions/{id}` | Archiva la pregunta | `204` / `404` |

Cuerpo de POST y PUT:

```json
{
  "nombre": "Patrón Strategy",
  "contexto": "Un sistema de envíos debe calcular el costo de forma distinta según el país...",
  "enunciado": "¿Qué permite el patrón Strategy?",
  "opcionA": "Intercambiar algoritmos en tiempo de ejecución",
  "opcionB": "Crear objetos sin exponer su clase concreta",
  "opcionC": "Recorrer una colección sin exponer su estructura",
  "opcionD": "Notificar cambios a varios observadores",
  "respuestaCorrecta": "A",
  "justificacion": "Strategy encapsula algoritmos intercambiables y permite elegirlos en tiempo de ejecución.",
  "bibliografia": "Gamma, E., Helm, R., Johnson, R. y Vlissides, J. (1994). Design Patterns. Addison-Wesley.",
  "competencia": "LECTURA_CRITICA",
  "tema": "Patrones de diseño",
  "subtema": "Strategy",
  "dificultad": "INTERMEDIO",
  "autor": "autor1"
}
```

Valores permitidos: `competencia` = `LECTURA_CRITICA`,
`RAZONAMIENTO_CUANTITATIVO`, `COMPETENCIAS_CIUDADANAS`,
`COMUNICACION_ESCRITA`, `INGLES`; `dificultad` = `BASICO`, `INTERMEDIO`,
`AVANZADO`; `respuestaCorrecta` = `A`, `B`, `C` o `D`.

### Errores

Todos los errores tienen el mismo formato:

```json
{
  "timestamp": "2026-09-20T14:10:30.383Z",
  "status": 400,
  "error": "Bad Request",
  "message": "La solicitud tiene datos inválidos",
  "details": ["nombre: El nombre es obligatorio", "respuestaCorrecta: La respuesta correcta debe ser A, B, C o D"]
}
```

| Situación | Código |
|---|---|
| Falta un campo, texto demasiado largo o letra fuera de A–D | `400` con el detalle de cada campo |
| JSON mal formado, o competencia/dificultad desconocida (se listan los valores válidos) | `400` |
| La pregunta no cumple la validación estructural (p. ej. contexto vacío, opciones repetidas, "todas las anteriores") | `400` con el campo y el motivo de cada incumplimiento |
| El dominio rechaza los datos (`IllegalArgumentException`) | `400` |
| Quien pide modificarla no es su autor | `403` |
| No existe la pregunta con ese id | `404` |
| La pregunta ya no está en `BORRADOR` (no se puede modificar) | `409` |
| Error inesperado | `500` con mensaje genérico (el detalle solo queda en el log) |

## Cómo ejecutarlo

Requiere Java 17+ y Maven (desde la raíz del proyecto):

```bash
mvn -pl modulo-api-rest -am package        # compila, prueba y genera el jar ejecutable
cd modulo-api-rest
java -jar target/banco-preguntas-api.jar   # arranca en http://localhost:8080
```

Al arrancar por primera vez se crean las tablas y se cargan las **12
preguntas de ejemplo** del monolito (`SampleDataSeeder`), para que el primer
GET ya devuelva datos. La base de datos es **H2 en archivo**
(`modulo-api-rest/data/`, ignorado por Git), de modo que los datos sobreviven a
reinicios. Consola de la base de datos: <http://localhost:8080/h2-console>
(JDBC URL `jdbc:h2:file:./data/banco-preguntas-api`, usuario `sa`, sin
contraseña). Para empezar de cero basta con borrar la carpeta `data/`.

Para usar otro motor (Postgres, MariaDB) se cambian las propiedades
`spring.datasource.*` de `application.properties` y se agrega el driver; el
código no cambia.

## Probar con Postman

Importar `postman/Taller06-API-REST.postman_collection.json`. La colección
tiene 8 peticiones para ejecutar en orden (listar, consultar, crear, actualizar,
archivar, verificar el estado `ARCHIVADA`, y los casos de error 404 y 400) y
cada una trae pruebas que validan el código HTTP; el POST guarda el id creado
en la variable `questionId` que usan el PUT, el DELETE y el GET final.

## Pruebas

52 pruebas automatizadas (JUnit 5, Mockito, Spring Boot Test):

| Clase | Pruebas | Qué cubre |
|---|---|---|
| `QuestionMapperTest` | 4 | Ida y vuelta dominio ↔ entidad ↔ respuesta y solicitud → contenido |
| `QuestionJpaAdapterTest` | 11 | Adaptador JPA contra H2: crear, leer, actualizar, contenido extendido, orden y ids |
| `QuestionApiServiceImplTest` | 8 | Traducción JSON → dominio, creación de borradores, DELETE = archivar |
| `QuestionControllerTest` | 16 | Códigos HTTP (200, 201, 204, 400, 403, 404, 409, 500), `Location` y formato de errores (MockMvc) |
| `QuestionApiIntegrationTest` | 10 | Flujo completo sin dobles: API → dominio → JPA → H2 |
| `SampleDataSeederTest` | 3 | Carga inicial de 12 preguntas con su contenido completo e idempotencia |

```bash
mvn -pl modulo-api-rest -am test
```

## Límites actuales

- Sin autenticación ni autorización (Corte 3).
- `GET /api/questions` devuelve todo: aún no hay paginación ni filtros
  (RF-07 y el listado del Autor están pendientes en el primer corte).
- La aplicación de escritorio sigue usando su repositorio en memoria; este
  microservicio tiene su propia base de datos.
- Las reglas de estados siguen las del dominio actual (una pregunta nueva nace
  en `BORRADOR`); cuando el dominio cambie, la API las hereda. Todavía no
  expone la acción "enviar a revisión".

## Autores

Taller realizado en pareja:

- Edward Dávila — edwarddavila@unicauca.edu.co
- Laura Isabel Sánchez Fernández
