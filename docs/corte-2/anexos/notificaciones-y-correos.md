# Notificaciones y correos

Cada evento relevante genera una notificación en la bandeja del sistema (`notification-service`) y un correo (`email-service`,
puerto `EmailSender`). Al crear el aviso, `notification-service` publica `notification.created` y `email-service` lo escucha para
enviar el correo. **El correo es simulado**, como indicó el profesor Libardo (consulta 16 de las
[consultas a los docentes](../../general/consultas-a-docentes.md)): el servicio de notificaciones reacciona al evento y deja el aviso en la
bandeja; el de correo registra el «envío» (`SimulatedEmailSender`, con reintentos y registro por decoradores) en lugar de salir a un servidor de correo, para no bloquear
cuentas en las pruebas. Si algún día se pide correo real, solo cambia el adaptador del puerto `EmailSender`. Se notifica
solo a los involucrados: administrador, revisores y autor.

La dirección de cada usuario se deriva de su identificador: `{usuario}@{EMAIL_DOMAIN}` (por defecto
`bancopreguntas.local`). El administrador puede consultar los correos «enviados» en `GET /api/emails` (`email-service`).

## Quién recibe qué

| Evento | Autor | Administrador que asignó | Revisores |
|---|---|---|---|
| `question.submitted` | — (el servicio solo recuerda título y autor) | — | — |
| `review.reviewers-assigned` | `PREGUNTA_EN_REVISION`: su pregunta entró en revisión | — (se recuerda quién asignó) | `REVISORES_ASIGNADOS`: se les asignó la pregunta (manual o automática) |
| `review.evaluation-submitted` | `OBSERVACION_TARDIA`, solo si llegó cuando la ronda ya estaba rechazada | `EVALUACION_RECIBIDA`, si la evaluación llegó a tiempo | — |
| `review.completed` | `REVISION_APROBADA` o `REVISION_RECHAZADA`, con las observaciones de los revisores | `RESULTADO_DE_REVISION` | `RESULTADO_DE_REVISION`, a los que evaluaron |
| `question.published` | `PREGUNTA_PUBLICADA` | — | — |

El administrador a quien se avisa es el que asignó los revisores (`assignedBy` del evento). Si la asignación fue una
reasignación automática (`assignedBy = system`) no se cambia al administrador que ya se conocía. Si todavía no se conoce
ninguno, simplemente no se le avisa.

Cada notificación guarda destinatario, tipo, asunto, mensaje, pregunta asociada, fecha de creación y fecha de lectura; el
correo y su resultado quedan en el registro de `email-service`. Si el envío falla, la notificación se conserva en la bandeja
(el correo es independiente) y el registro del correo muestra el error.

Los consumidores son idempotentes: un evento repetido (mismo `eventId`) no duplica los avisos.

## Cómo se redactan: Factory Method

Cada tipo de aviso tiene su propio **creador** (patrón Factory Method, ver [05 · Patrones](../05-patrones.md)):
`CreadorDeAviso` define cómo se arma la notificación y cada subclase fabrica su mensaje.

| Tipo | Creador | Asunto |
|---|---|---|
| `REVISORES_ASIGNADOS` | `AvisoRevisoresAsignados` | Pregunta por revisar: «título» |
| `PREGUNTA_EN_REVISION` | `AvisoPreguntaEnRevision` | Tu pregunta está en revisión: «título» |
| `EVALUACION_RECIBIDA` | `AvisoEvaluacionRecibida` | Evaluación recibida: «título» |
| `REVISION_APROBADA` | `AvisoRevisionAprobada` | Pregunta aprobada: «título» |
| `REVISION_RECHAZADA` | `AvisoRevisionRechazada` | Pregunta rechazada: «título» |
| `RESULTADO_DE_REVISION` | `AvisoResultadoDeRevision` | Revisión terminada, aprobada (o rechazada): «título» |
| `OBSERVACION_TARDIA` | `AvisoObservacionTardia` | Nueva observación sobre «título» |
| `PREGUNTA_PUBLICADA` | `AvisoPreguntaPublicada` | Pregunta publicada: «título» |

Si no se conoce el título, la pregunta se nombra por su identificador («la pregunta q-9»).

## Textos

Los textos entre llaves se reemplazan con los datos del evento. Las observaciones solo se listan si tienen texto.

**Asignación al revisor.** «Tienes pendiente evaluar {pregunta}. {motivo} Entra a tu bandeja de revisión para aprobarla o
rechazarla.» El motivo es «El administrador te asignó como revisor (ronda {n}).» o, en la reasignación automática, «El
autor corrigió la pregunta y la envió de nuevo; como ya la habías revisado, quedas asignado otra vez (ronda {n}).»

**Pregunta en revisión (autor).** «Se asignaron revisores a {pregunta} (ronda {n}) y su estado ahora es «En revisión». Te
avisaremos cuando termine la revisión.»

**Evaluación recibida (administrador).** «El revisor {revisor} evaluó {pregunta} en la ronda {n}: {decisión}.» y, si la
hay, «Observación: {texto}».

**Aprobada (autor).** «Los revisores aprobaron {pregunta} (ronda {n}). Queda lista para que el administrador la
publique.» seguido de la lista de observaciones.

**Rechazada (autor).** «Un revisor rechazó {pregunta} (ronda {n}). Puedes reabrirla, corregirla y enviarla de nuevo; los
demás revisores aún pueden dejar sus observaciones.» seguido de la lista de observaciones.

**Resultado de la revisión (administrador y revisores).** «La revisión de {pregunta} terminó: los revisores la aprobaron
(ronda {n}). Queda lista para que el administrador la publique.» o «…terminó: fue rechazada (ronda {n}). El autor puede
corregirla y enviarla de nuevo.», con la lista de observaciones.

**Observación tardía (autor).** «El revisor {revisor} dejó su evaluación ({decisión}) sobre {pregunta} en la ronda {n}, que
ya había sido rechazada.» y, si la hay, «Observación: {texto}».

**Publicada (autor).** «El administrador publicó {pregunta}. Ya hace parte del banco de preguntas.»

## Configuración

| Variable | Para qué | Por defecto |
|---|---|---|
| `NOTIFICATION_EMAIL_DOMAIN` | Dominio con el que se arma la dirección de cada usuario | `bancopreguntas.local` |

No hay credenciales de correo ni servidor SMTP: el envío es simulado.
