# validation-service · Validación estructural

Revisa el contenido de una pregunta contra las reglas de calidad y reporta todos los campos que incumplen. No guarda datos ni usa mensajería.

| | |
|---|---|
| **Puerto** | 8095 (`VALIDATION_SERVICE_PORT`) |
| **Base de datos** | No tiene (servicio sin estado) |
| **Swagger** | `http://localhost:8095/swagger-ui.html` (directo; el servicio es interno) |
| **Pruebas** | 53 pruebas (unitarias, de API, de arquitectura con ArchUnit) |

## Responsabilidad

Es el contexto de **Validación estructural** (HU-03, RF-08 a RF-13). Lo llama `question-service` al crear, modificar y enviar a revisión; el gateway no tiene rutas hacia él. Es un servicio **sin estado**: no tiene base de datos ni se conecta al broker.

## Reglas de negocio

- Siete reglas independientes: campos obligatorios, contexto obligatorio, una única pregunta directa, exactamente cuatro opciones, una única respuesta correcta, sin expresiones prohibidas («todas las anteriores») y longitud y estructura de las opciones.
- Agregar o quitar un criterio es registrar otra regla en `QuestionValidator`, sin tocar las existentes (RNF-13).
- Siempre responde 200: el cuerpo dice si el contenido es válido y, si no, lista cada campo con su mensaje. Quien lo llama decide qué hacer.
- Como cualquier servicio, comprueba el rol (AUTHOR o ADMIN) de quien pidió la operación, que le llega con la identidad propagada.

## API REST

Las rutas **no pasan por el gateway**: las llaman otros servicios con la identidad de quien hizo la petición, y el servicio vuelve a comprobar el token y el rol.

| Método | Ruta | Qué hace |
|---|---|---|
| `POST` | `/api/validation/questions` | Revisar el contenido de una pregunta contra las reglas de calidad. Rol AUTHOR o ADMIN. |

## Mensajería (RabbitMQ)

No usa mensajería: no publica ni escucha eventos.

## Estructura

```
src/main/java/co/unicauca/bancopreguntas/validation/
├── api/              Controlador REST, DTO y manejo de errores
├── application/      Casos de uso y puertos (interfaces que el dominio necesita)
├── domain/           Reglas de negocio; no depende de Spring ni de la base de datos
└── infrastructure/   Adaptadores: configuración
```

**Patrones de diseño que usa:** Strategy (cada `ValidationRule` es una estrategia), Template Method (`ReglaPorOpcion`) y Adapter. Detalle en [patrones](../../../docs/corte-2/05-patrones.md).

## Configuración

| Variable | Valor por defecto | Para qué sirve |
|---|---|---|
| `VALIDATION_SERVICE_PORT` | `8095` | Puerto HTTP |
| `EUREKA_ENABLED` | `true` | Registrarse en Eureka (se apaga en las pruebas) |
| `EUREKA_URL` | `http://localhost:8761/eureka` | Dirección de Eureka |

## Ejecutar y probar

```bash
# desde microservicios/
./mvnw -pl servicios/validation-service -am test          # pruebas del servicio
docker compose up -d --build validation-service    # el servicio con sus dependencias
```

La pila completa se levanta como se explica en el [README de microservicios](../../README.md).
