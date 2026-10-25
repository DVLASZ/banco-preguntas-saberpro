# edge · Balanceador de entrada (nginx)

Único punto de entrada de la pila: recibe las peticiones en el puerto **8080** y las reparte entre las réplicas del
[`api-gateway`](../api-gateway/). Es lo que permite escalar el gateway con `docker compose up -d --scale api-gateway=3`.

Toda su configuración está en [`nginx.conf`](nginx.conf):

| Ajuste | Qué hace |
|---|---|
| `resolver 127.0.0.11 valid=5s` y `server api-gateway:8080 resolve` | Vuelve a resolver el nombre cada pocos segundos: una réplica nueva entra en el reparto y una caída sale sin reiniciar nada |
| `max_fails=3 fail_timeout=3s` | Una réplica se da por caída solo por no poder conectarse y por poco tiempo; un 503 de negocio (por ejemplo, del circuit breaker) no la saca del reparto |
| `keepalive 256` y `keepalive_requests 100000` | Conexiones persistentes hacia las réplicas; sin ellas, con carga se agotan los puertos del equipo |
| `proxy_next_upstream error timeout` (3 intentos) | Si una réplica no responde, se prueba con otra (nginx solo repite lecturas, no escrituras) |
| `proxy_connect_timeout 2s` y `proxy_read_timeout 30s` | Límites de espera |
| `location = /edge-health` | Respuesta `UP` para las comprobaciones de salud |

No contiene lógica de negocio ni de seguridad: el token lo valida el gateway.
