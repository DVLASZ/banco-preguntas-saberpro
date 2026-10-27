package co.unicauca.bancopreguntas.question.application.port;

import co.unicauca.bancopreguntas.question.domain.EstadoPregunta;

/** El estado que informa lifecycle-service tras una transición: estado, versión del contenido y número de cambio. */
public record EstadoDeCiclo(EstadoPregunta estado, int version, int secuencia) {
}
