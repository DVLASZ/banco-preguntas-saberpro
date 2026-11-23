# Cobertura de las pruebas del dominio

La rúbrica del tercer corte pide pruebas unitarias para todas las clases del dominio, entidades y servicios. Para no decirlo "a ojo" medimos la
cobertura con JaCoCo, que cuenta qué instrucciones del código ejecutan las pruebas.

## Cómo se mide

JaCoCo está configurado en el `pom.xml` de `microservicios/`, así que se activa solo al correr las pruebas:

```bash
cd microservicios
./mvnw test
```

Cada módulo deja su informe en `target/site/jacoco/index.html` (para verlo) y `target/site/jacoco/jacoco.csv` (para procesarlo). Para un solo
servicio:

```bash
./mvnw -pl servicios/evaluation-service -am test
```

La corrida completa tarda cerca de 25 minutos porque las pruebas de persistencia levantan un PostgreSQL embebido por servicio.

## Resultado

Última corrida completa: 13 de noviembre de 2026, los 16 módulos en verde, 1102 pruebas en total y cuatro omitidas a propósito (el contrato OpenAPI de question, assignment, evaluation y lifecycle,
que solo corre cuando se le pasa la propiedad `openapi.export` para exportar el contrato).

Se mide el paquete `domain` de cada servicio: entidades, objetos de valor, enumeraciones, políticas y servicios de dominio. Las clases de excepción no
se cuentan, porque son declaraciones sin lógica.

| Servicio | Clases del dominio | Pruebas del servicio | Cobertura del dominio |
|---|---|---|---|
| `user-service` | 7 | 62 | 98,9 % |
| `question-service` | 11 | 148 | 98,0 % |
| `validation-service` | 14 | 53 | 100,0 % |
| `lifecycle-service` | 16 | 143 | 98,4 % |
| `assignment-service` | 11 | 101 | 98,6 % |
| `evaluation-service` | 18 | 122 | 99,1 % |
| `notification-service` | 18 | 85 | 99,1 % |
| `email-service` | 4 | 38 | 97,2 % |
| `history-service` | 7 | 54 | 100,0 % |
| `audit-service` | 6 | 38 | 99,4 % |
| `simulation-service` | 13 | 93 | 99,0 % |
| `tracking-service` | 11 | 67 | 99,8 % |

Ninguna clase del dominio queda por debajo del 80 %. Las instrucciones sin cubrir son pocas y sueltas; el informe de JaCoCo las muestra línea por línea.

## Qué se agregó en este corte

Al medir aparecieron clases con menos del 80 %. Se les escribió prueba propia:

| Clase | Servicio | Prueba nueva |
|---|---|---|
| `Observacion` | `evaluation-service` | `ObservacionTest` |
| `Asignacion` | `evaluation-service` y `assignment-service` | `AsignacionTest` en cada uno |
| `Evaluacion` | `evaluation-service` | `EvaluacionTest` |
| `Observador` | `evaluation-service` | `ObservadorTest` |
| `EstadoRonda` | `assignment-service` | `EstadoRondaTest` |
| `CambioEstado` | `question-service` y `lifecycle-service` | `CambioEstadoTest` en cada uno |

Después de esa primera medición se agregaron pruebas directas para lo que seguía probándose solo de rebote:

| Clase | Servicio | Prueba nueva |
|---|---|---|
| Las siete clases `Fase*` (patrón State) | `lifecycle-service` | un `Fase<Estado>Test` por clase, con el helper `OperacionesDeFase` |
| `SelectorDePreguntas` (contrato de la estrategia) | `simulation-service` | `SelectorDePreguntasTest` |
| `Desempeno` | `tracking-service` | `DesempenoTest` |

`FaseDePreguntaTest` se mantiene, porque comprueba que la fábrica entrega la fase correcta para cada estado.

## Otras pruebas que protegen el dominio

- ArchUnit en cada servicio: el dominio no depende de Spring, de JPA ni de la mensajería. En `evaluation-service` también se comprueba el hexágono completo
  (ver [hexagonal y DDD](hexagonal-y-ddd.md)).
- `recorrido_funcional.py` recorre el sistema con tokens reales de Keycloak.
- Las pruebas de API usan MockMvc, y las de persistencia usan PostgreSQL embebido, para que no dependan de un Docker encendido.
