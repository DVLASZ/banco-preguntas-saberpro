# Notificaciones y plantillas de correo

Cada evento relevante genera una notificación en la bandeja del sistema y un correo
(`notification-service`, puerto `EmailSender`). **El correo es simulado**, como indicó el profesor Libardo: el
servicio reacciona al evento y registra el envío en lugar de salir a un servidor de correo, para no bloquear
cuentas en las pruebas. Si algún día se pide correo real, solo cambia el adaptador del puerto `EmailSender`. Se notifica solo a los involucrados:
administrador, revisores y autor.

## Quién recibe qué

| Evento | Autor | Administrador | Revisores asignados |
|---|---|---|---|
| `review.reviewers-assigned` | Aviso: tu pregunta pasó a "En revisión" | — | Aviso: se te asignó una pregunta |
| `review.evaluation-submitted` | Aviso: nueva evaluación recibida | Aviso: nueva evaluación recibida | — |
| `review.completed` | Resultado y observaciones | Resultado | Resultado |

Cada notificación guarda destinatario, tipo, asunto, mensaje, pregunta asociada, si se leyó y el estado del
correo (`PENDIENTE`, `ENVIADO`, `FALLIDO`). Si el correo falla, la notificación en bandeja se conserva y el
estado queda en `FALLIDO` para poder reintentar.

## Plantillas

Los textos entre llaves se reemplazan con los datos del evento.

### Revisores asignados (al revisor)

- **Asunto:** Se te asignó una pregunta para revisar
- **Cuerpo:**
  Hola {nombreRevisor},
  El administrador te asignó la pregunta "{nombrePregunta}" ({competencia}) para su revisión.
  Ingresa al sistema, lee la pregunta y registra tu evaluación.
  Banco de Preguntas Saber Pro

### Revisores asignados (al autor)

- **Asunto:** Tu pregunta pasó a revisión
- **Cuerpo:**
  Hola {nombreAutor},
  Tu pregunta "{nombrePregunta}" ya tiene {cantidadRevisores} revisor(es) asignado(s) y está en estado "En revisión".

### Evaluación recibida (al autor y al administrador)

- **Asunto:** Nueva evaluación en "{nombrePregunta}"
- **Cuerpo:**
  Hola {nombreDestinatario},
  Un revisor registró su evaluación de la pregunta "{nombrePregunta}": {decision}.
  Evaluaciones pendientes: {pendientes}.

### Pregunta aprobada (autor, administrador y revisores)

- **Asunto:** La pregunta "{nombrePregunta}" fue aprobada
- **Cuerpo:**
  Hola {nombreDestinatario},
  Todos los revisores aprobaron la pregunta "{nombrePregunta}". Su estado ahora es "Aprobada".

### Pregunta rechazada (autor, administrador y revisores)

- **Asunto:** La pregunta "{nombrePregunta}" fue rechazada
- **Cuerpo:**
  Hola {nombreDestinatario},
  La pregunta "{nombrePregunta}" fue rechazada. Observaciones de los revisores:
  {listaObservaciones}
  El autor puede corregirla desde el estado "Borrador" y volver a enviarla a revisión.

## Configuración

El remitente y el servidor SMTP se configuran por variables de entorno (`SMTP_HOST`, `SMTP_PORT`, `SMTP_USER`,
`SMTP_PASS`, `SMTP_FROM`) en un archivo `.env` que no se versiona. En desarrollo y pruebas apuntan a Mailpit,
que captura los correos sin enviarlos.
