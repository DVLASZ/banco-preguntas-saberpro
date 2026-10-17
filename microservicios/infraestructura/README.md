# Infraestructura

Piezas que sostienen a los servicios de negocio.

| Carpeta | Contenido |
|---|---|
| [`api-gateway`](api-gateway/) | Entrada única: valida el token, autoriza por rol y aplica circuit breaker, reintentos y límite de llamadas |
| [`discovery-server`](discovery-server/) | Eureka: registro y descubrimiento de servicios, para añadir réplicas sin cambiar configuración |
| [`edge`](edge/) | Configuración de nginx que reparte las peticiones entre las réplicas del gateway |
| [`keycloak`](keycloak/) | Realm de Keycloak con los usuarios y roles de prueba |

Los contenedores se levantan con el `docker-compose.yml` de la carpeta superior.
