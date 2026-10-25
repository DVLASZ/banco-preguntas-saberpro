# discovery-server · Registro de servicios (Eureka)

Servidor Eureka donde cada servicio se registra al arrancar y donde el gateway averigua dónde está cada uno. Gracias a él se agregan
réplicas de un servicio sin cambiar ninguna configuración: la nueva réplica se registra y entra en el reparto.

| | |
|---|---|
| **Puerto** | 8761 (`DISCOVERY_SERVER_PORT`) |
| **Consola** | `http://localhost:8761` |
| **Pruebas** | 3 |

## Configuración pensada para desarrollo

| Ajuste | Valor | Por qué |
|---|---|---|
| `register-with-eureka` y `fetch-registry` | `false` | Este servidor es el registro: no se registra en sí mismo ni consulta a otros |
| `enable-self-preservation` | `false` | Una réplica caída debe salir del registro en segundos, no en minutos |
| `eviction-interval-timer-in-ms` | `5000` | Se revisan las réplicas caídas cada 5 s |
| `use-read-only-response-cache` | `false` | Una réplica nueva o caída se ve de inmediato |

| Variable | Valor por defecto | Para qué sirve |
|---|---|---|
| `DISCOVERY_SERVER_PORT` | `8761` | Puerto HTTP |
| `EUREKA_HOSTNAME` | `localhost` | Nombre con el que se anuncia el servidor |

## Ejecutar

```bash
# desde microservicios/
./mvnw -pl infraestructura/discovery-server test
docker compose up -d --build discovery-server
```

Los servicios lo encuentran en `EUREKA_URL` (`http://localhost:8761/eureka` por defecto) y pueden funcionar sin él con
`EUREKA_ENABLED=false`, como hacen las pruebas.
