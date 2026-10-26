package co.unicauca.bancopreguntas.history.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Entradas, filtros y resúmenes del historial")
class EntradaDeHistorialTest {

    private static final Instant T = Instant.parse("2026-10-08T10:00:00Z");

    @Test
    @DisplayName("una entrada del historial exige la pregunta")
    void preguntaObligatoria() {
        assertThatThrownBy(() -> new EntradaDeHistorial("id", "ev", null, "a", TipoEntrada.PREGUNTA_ENVIADA, "r", null, T, T))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new EntradaDeHistorial("id", "ev", " ", "a", TipoEntrada.PREGUNTA_ENVIADA, "r", null, T, T))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("exige identificadores, tipo, resumen y fechas")
    void obligatorios() {
        assertThatThrownBy(() -> new EntradaDeHistorial(" ", "ev", "q", "a", TipoEntrada.PREGUNTA_ENVIADA, "r", null, T, T))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new EntradaDeHistorial("id", "", "q", "a", TipoEntrada.PREGUNTA_ENVIADA, "r", null, T, T))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new EntradaDeHistorial("id", "ev", "q", "a", null, "r", null, T, T))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new EntradaDeHistorial("id", "ev", "q", "a", TipoEntrada.PREGUNTA_ENVIADA, " ", null, T, T))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new EntradaDeHistorial("id", "ev", "q", "a", TipoEntrada.PREGUNTA_ENVIADA, "r", null, null, T))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("el observador exige usuario")
    void observador() {
        assertThatThrownBy(() -> new Observador(" ", true, false, false)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("los resúmenes nombran a quien actuó, o al sistema si no hay actor")
    void resumenes() {
        assertThat(Resumenes.preguntaPublicada(null)).isEqualTo("el sistema publicó la pregunta");
        assertThat(Resumenes.preguntaEnviada("a", null, 1)).contains("la pregunta").doesNotContain("«");
        assertThat(Resumenes.cambioDeEstado(null, "PENDIENTE_REVISION", null)).contains("(nuevo)").contains("el sistema");
        assertThat(Resumenes.revisoresAsignados("admin", 1, List.of("r1", "r2"), false)).contains("admin").contains("r1, r2");
        assertThat(Resumenes.revisoresAsignados("system", 2, List.of("r1"), true)).contains("automáticamente");
        assertThat(Resumenes.revisionCompletada(1, true)).contains("aprobada");
        assertThat(Resumenes.revisionCompletada(2, false)).contains("rechazada");
        assertThat(Resumenes.evaluacionTardia("r1", 1, "APROBAR")).contains("tardía");
    }

    @Test
    @DisplayName("la página nunca tiene menos de una página")
    void pagina() {
        assertThat(new Pagina<String>(List.of(), 1, 10, 0).totalPaginas()).isEqualTo(1);
        assertThat(new Pagina<>(List.of("a"), 1, 10, 25).totalPaginas()).isEqualTo(3);
    }
}
