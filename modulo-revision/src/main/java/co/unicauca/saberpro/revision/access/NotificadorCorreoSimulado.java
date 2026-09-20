package co.unicauca.saberpro.revision.access;

import co.unicauca.saberpro.preguntas.domain.Question;
import co.unicauca.saberpro.revision.domain.NotificadorAsignacion;
import co.unicauca.saberpro.revision.domain.Revisor;

/**
 * Correo simulado: el proyecto permite simular las integraciones externas
 * (envío de emails), siempre que se justifique el diseño.
 *
 * <p>TODO(HU-04): armar el correo (destinatario {@code revisor.correo()}, asunto y
 * cuerpo con el id, el nombre y la competencia de la pregunta) y "enviarlo":
 * imprimirlo por consola / registro y guardarlo en una lista para poder
 * verificarlo en las pruebas (por ejemplo un método {@code enviados()}).
 */
public class NotificadorCorreoSimulado implements NotificadorAsignacion {

    @Override
    public void notificar(Revisor revisor, Question pregunta) {
        throw new UnsupportedOperationException("HU-04: implementar NotificadorCorreoSimulado");
    }
}
