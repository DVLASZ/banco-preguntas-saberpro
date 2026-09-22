package co.unicauca.saberpro.revision.access;

import co.unicauca.saberpro.preguntas.domain.Question;
import co.unicauca.saberpro.revision.domain.NotificadorAsignacion;
import co.unicauca.saberpro.revision.domain.Revisor;

import java.util.ArrayList;
import java.util.List;

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

    public record Correo(String destinatario, String asunto, String cuerpo) {
    }

    private final List<Correo> enviados = new ArrayList<>();

    @Override
    public void notificar(Revisor revisor, Question pregunta) {
        String asunto = "Nueva pregunta asignada para revisión: " + pregunta.getId();
        String cuerpo = "Hola " + revisor.nombreCompleto() + ",\n\n"
                + "Se te ha asignado la pregunta " + pregunta.getId() + " (\"" + pregunta.getNombre() + "\") "
                + "de la competencia " + pregunta.getCompetencia() + " para su revisión.\n\n"
                + "Saludos,\nBanco de Preguntas Saber Pro";
        Correo correo = new Correo(revisor.correo(), asunto, cuerpo);
        System.out.println("[CORREO SIMULADO] Para: " + correo.destinatario()
                + " | Asunto: " + correo.asunto() + "\n" + correo.cuerpo());
        enviados.add(correo);
    }

    public List<Correo> enviados() {
        return List.copyOf(enviados);
    }
}
