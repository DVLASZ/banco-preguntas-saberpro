package co.unicauca.bancopreguntas.lifecycle.domain;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Ayuda de las pruebas de cada fase: todas las operaciones de la fase, por nombre. */
final class OperacionesDeFase {

    private OperacionesDeFase() {
    }

    static Map<String, Supplier<EstadoPregunta>> de(FaseDePregunta fase) {
        Map<String, Supplier<EstadoPregunta>> m = new LinkedHashMap<>();
        m.put("enviarARevision", fase::enviarARevision);
        m.put("iniciarRevision", fase::iniciarRevision);
        m.put("aprobar", fase::aprobar);
        m.put("rechazar", fase::rechazar);
        m.put("reabrir", fase::reabrir);
        m.put("publicar", fase::publicar);
        return m;
    }

    /** La fase acepta la operación indicada y rechaza todas las demás. */
    static void soloAdmite(FaseDePregunta fase, String permitida, EstadoPregunta destino) {
        de(fase).forEach((nombre, operacion) -> {
            if (nombre.equals(permitida)) {
                assertThat(operacion.get()).as(nombre).isEqualTo(destino);
            } else {
                assertThatThrownBy(operacion::get).as(nombre).isInstanceOf(OperacionNoPermitidaException.class);
            }
        });
    }

    /** La fase rechaza todas las operaciones. */
    static void noAdmiteNinguna(FaseDePregunta fase) {
        de(fase).forEach((nombre, operacion) ->
                assertThatThrownBy(operacion::get).as(nombre).isInstanceOf(OperacionNoPermitidaException.class));
    }
}
