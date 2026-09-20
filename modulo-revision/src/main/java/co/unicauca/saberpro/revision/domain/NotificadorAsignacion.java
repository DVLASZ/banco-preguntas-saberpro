package co.unicauca.saberpro.revision.domain;

import co.unicauca.saberpro.preguntas.domain.Question;

/**
 * Avisa a un revisor que se le asignó una pregunta (el correo del Entregable 4).
 * Es una abstracción para poder cambiar el correo simulado por uno real (SMTP)
 * sin tocar el servicio.
 */
public interface NotificadorAsignacion {

    void notificar(Revisor revisor, Question pregunta);
}
