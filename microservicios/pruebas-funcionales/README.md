# Pruebas funcionales

Recorrido de las historias de usuario contra la pila completa en Docker. Complementa a las pruebas automatizadas de cada
módulo (que corren sin Docker) comprobando que los contenedores, Keycloak, el gateway, RabbitMQ y las bases de datos
funcionan juntos.

## Qué comprueba

| Historia | Comprobaciones |
|---|---|
| Seguridad | Login con Keycloak, sin token 401, rol incorrecto 403 |
| HU-01 | Crear pregunta válida en Borrador; la validación estructural rechaza dos preguntas directas, «todas las anteriores», falta de contexto y opciones repetidas |
| HU-02 | Enviar a revisión; no se puede enviar dos veces |
| HU-03 | Listado propio con filtro y paginación |
| HU-04 | 0 y 4 revisores se rechazan, el autor no puede ser revisor, con 2 la pregunta pasa a En revisión; avisos al revisor y al autor |
| HU-05 | Rechazar exige observación, unanimidad (una aprobación no basta), el autor lee las observaciones, avisos al administrador, a los revisores y al autor, correos simulados |
| HU-06 | Un rechazo cierra la ronda, el autor reabre (versión 2), corrige, reenvía y se abre una ronda nueva |
| RF-21 a RF-24 | La pregunta publicada llega al catálogo de simulacros; el estudiante genera un simulacro aleatorio; no se revela la respuesta correcta mientras está en curso; tiempo restante; contesta, finaliza y se califica automáticamente; finalizar o contestar dos veces se rechaza; el docente no genera simulacros y un estudiante no ve el de otro |
| RF-25 a RF-28 | El resultado llega al historial; estadísticas individuales; fortalezas y debilidades; el docente consulta a un estudiante, el reporte del grupo y su CSV; un estudiante no ve el reporte ni el seguimiento de otro |
| Tiempo agotado | Solo con `PRUEBA_TIEMPO=1`: un simulacro de un minuto se cierra solo, llega al seguimiento como expirado y ya no acepta respuestas |
| Transversal | Publicación, historial, auditoría de accesos y Swagger |

## Cómo ejecutarlo

```bash
cd microservicios
docker compose up -d --build          # espera a que todos los contenedores estén healthy
python pruebas-funcionales/recorrido_funcional.py
PRUEBA_TIEMPO=1 python pruebas-funcionales/recorrido_funcional.py   # además, el cierre por tiempo agotado (~100 s)
```

Solo usa la biblioteca estándar de Python 3. Lee la clave de desarrollo del realm
(`infraestructura/keycloak/bancopreguntas-realm.json`); se puede cambiar con la variable `PW`. `BASE` y `KC` cambian las
direcciones de `edge` y de Keycloak. Devuelve código de salida 1 si alguna comprobación falla.

Las preguntas que crea quedan en la base local; para empezar de cero: `docker compose down -v`.
