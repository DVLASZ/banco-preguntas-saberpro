package co.unicauca.bancopreguntas.audit.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Entradas, filtros y resúmenes de la auditoría")
class EntradaDeAuditoriaTest {

    private static final Instant T = Instant.parse("2026-10-08T10:00:00Z");

    @Test
    @DisplayName("una entrada de pregunta exige la pregunta; una de seguridad no")
    void preguntaObligatoria() {
        assertThatThrownBy(() -> new EntradaDeAuditoria("id", "ev", null, "a", TipoEntrada.PREGUNTA_ENVIADA, "r", null, T, T))
                .isInstanceOf(IllegalArgumentException.class);
        EntradaDeAuditoria login = new EntradaDeAuditoria("id", "ev", null, "a", TipoEntrada.INICIO_DE_SESION, "r", null, T, T);

        assertThat(login.preguntaId()).isNull();
    }

    @Test
    @DisplayName("exige identificadores, tipo, resumen y fechas")
    void obligatorios() {
        assertThatThrownBy(() -> new EntradaDeAuditoria(" ", "ev", "q", "a", TipoEntrada.CAMBIO_DE_ESTADO, "r", null, T, T))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new EntradaDeAuditoria("id", null, "q", "a", TipoEntrada.CAMBIO_DE_ESTADO, "r", null, T, T))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new EntradaDeAuditoria("id", "ev", "q", "a", null, "r", null, T, T))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new EntradaDeAuditoria("id", "ev", "q", "a", TipoEntrada.CAMBIO_DE_ESTADO, " ", null, T, T))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new EntradaDeAuditoria("id", "ev", "q", "a", TipoEntrada.CAMBIO_DE_ESTADO, "r", null, null, T))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new EntradaDeAuditoria("id", "ev", "q", "a", TipoEntrada.CAMBIO_DE_ESTADO, "r", null, T, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("solo los inicios de sesión son de seguridad")
    void seguridad() {
        for (TipoEntrada tipo : TipoEntrada.values()) {
            boolean esperado = tipo == TipoEntrada.INICIO_DE_SESION || tipo == TipoEntrada.INICIO_DE_SESION_FALLIDO;
            assertThat(tipo.esDeSeguridad()).as(tipo.name()).isEqualTo(esperado);
        }
    }

    @Test
    @DisplayName("el filtro rechaza un rango invertido y normaliza los textos vacíos")
    void filtro() {
        assertThatThrownBy(() -> new FiltroAuditoria(null, null, null, T, T.minusSeconds(1)))
                .isInstanceOf(IllegalArgumentException.class);
        FiltroAuditoria f = new FiltroAuditoria(null, "  ", " q-1 ", null, null);

        assertThat(f.actorId()).isNull();
        assertThat(f.preguntaId()).isEqualTo("q-1");
        assertThat(FiltroAuditoria.sinFiltros()).isEqualTo(new FiltroAuditoria(null, null, null, null, null));
    }

    @Test
    @DisplayName("los resúmenes nombran a quien actuó, o al sistema si no hay actor")
    void resumenes() {
        assertThat(Resumenes.preguntaPublicada(null)).startsWith("el sistema");
        assertThat(Resumenes.cambioDeEstado(null, "BORRADOR", "autor-1")).contains("(nuevo)").contains("BORRADOR");
        assertThat(Resumenes.revisoresAsignados("admin-1", 1, List.of("r1", "r2"), false)).contains("admin-1").contains("r1, r2");
        assertThat(Resumenes.revisoresAsignados(null, 2, List.of("r1"), true)).contains("automáticamente").contains("ronda 2");
        assertThat(Resumenes.revisionCompletada(1, false)).contains("rechazada");
        assertThat(Resumenes.inicioDeSesion("u")).isEqualTo("u inició sesión");
        assertThat(Resumenes.inicioDeSesionFallido(null)).contains("el sistema");
    }

    @Test
    @DisplayName("la página nunca tiene menos de una página")
    void pagina() {
        assertThat(new Pagina<String>(List.of(), 1, 10, 0).totalPaginas()).isEqualTo(1);
        assertThat(new Pagina<>(List.of("a"), 2, 10, 13).totalPaginas()).isEqualTo(2);
    }
}
