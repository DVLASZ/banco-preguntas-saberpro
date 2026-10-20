package co.unicauca.bancopreguntas.notification.domain.aviso;

import co.unicauca.bancopreguntas.notification.domain.TipoNotificacion;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/** Registro de los creadores de aviso: entrega el que corresponde a cada {@link TipoNotificacion}. */
public final class CreadoresDeAviso {

    private static final Map<TipoNotificacion, CreadorDeAviso> POR_TIPO = new EnumMap<>(TipoNotificacion.class);

    static {
        List.<CreadorDeAviso>of(new AvisoRevisoresAsignados(), new AvisoRevisionAprobada(),
                new AvisoRevisionRechazada(), new AvisoObservacionTardia(), new AvisoPreguntaPublicada(),
                new AvisoPreguntaEnRevision(), new AvisoEvaluacionRecibida(), new AvisoResultadoDeRevision())
                .forEach(creador -> POR_TIPO.put(creador.tipo(), creador));
    }

    private CreadoresDeAviso() {
    }

    /** @throws IllegalStateException si el tipo no tiene creador registrado */
    public static CreadorDeAviso para(TipoNotificacion tipo) {
        CreadorDeAviso creador = POR_TIPO.get(tipo);
        if (creador == null) {
            throw new IllegalStateException("No hay creador de aviso para " + tipo);
        }
        return creador;
    }
}
