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

La corrida completa tarda unos 25 minutos porque las pruebas de persistencia levantan un PostgreSQL embebido por servicio.

## Resultado

Se mide el paquete `domain` de cada servicio: entidades, objetos de valor, enumeraciones, políticas y servicios de dominio. Las clases de excepción no
se cuentan, porque son declaraciones sin lógica.

| Servicio | Clases del dominio | Pruebas del servicio | Cobertura del dominio |
|---|---|---|---|
| `user-service` | 7 | 62 | 98,9 % |
| `question-service` | 11 | 193 | 98,0 % |
| `validation-service` | 14 | 28 | 100,0 % |
| `lifecycle-service` | 16 | 82 | 98,2 % |
| `assignment-service` | 11 | 83 | 98,6 % |
| `evaluation-service` | 18 | 121 | 99,1 % |
| `notification-service` | 18 | 91 | 99,1 % |
| `email-service` | 4 | 38 | 97,2 % |
| `history-service` | 7 | 54 | 100,0 % |
| `audit-service` | 6 | 38 | 99,4 % |
| `simulation-service` | 13 | 88 | 99,0 % |
| `tracking-service` | 11 | 61 | 99,8 % |

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

Las siete clases `Fase*` de `lifecycle-service` (patrón State) se prueban a través de `FaseDePregunta`, que las crea una por estado
(`FaseDePreguntaTest`).

## Otras pruebas que protegen el dominio

- ArchUnit en cada servicio: el dominio no depende de Spring, de JPA ni de la mensajería. En `evaluation-service` también se comprueba el hexágono completo
  (ver [hexagonal y DDD](hexagonal-y-ddd.md)).
- `recorrido_funcional.py` recorre el sistema con tokens reales de Keycloak.
- Las pruebas de API usan MockMvc, y las de persistencia usan PostgreSQL embebido, para que no dependan de un Docker encendido.
