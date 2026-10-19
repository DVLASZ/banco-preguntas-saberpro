package co.unicauca.bancopreguntas.tracking.application;

import co.unicauca.bancopreguntas.tracking.domain.Resultados;
import co.unicauca.bancopreguntas.tracking.domain.Tendencia;
import co.unicauca.bancopreguntas.tracking.domain.Umbrales;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.ZoneOffset;

import static co.unicauca.bancopreguntas.tracking.domain.Resultados.comp;
import static co.unicauca.bancopreguntas.tracking.domain.Resultados.resultado;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("TrackingApplicationService (casos de uso)")
class TrackingApplicationServiceTest {

    private InMemoryHistorialRepository historial;
    private TrackingApplicationService servicio;

    @BeforeEach
    void preparar() {
        historial = new InMemoryHistorialRepository();
        servicio = new TrackingApplicationService(historial, new InMemoryProcessedEvents(), new Umbrales(70, 50, 5),
                Clock.fixed(Resultados.T0, ZoneOffset.UTC));
    }

    @Test
    @DisplayName("un simulacro terminado entra al historial del estudiante")
    void alCompletarse() {
        servicio.alCompletarseUnSimulacro("e-1", resultado("s-1", "ana", 1, 8, 10, comp("INGLES", 8, 10)));

        assertThat(servicio.historial("ana", 1, 10).elementos()).singleElement()
                .satisfies(r -> assertThat(r.simulacroId()).isEqualTo("s-1"));
        assertThat(servicio.historial("luis", 1, 10).totalElementos()).isZero();
    }

    @Test
    @DisplayName("un evento repetido, o el mismo simulacro con otro evento, no se cuenta dos veces")
    void idempotente() {
        servicio.alCompletarseUnSimulacro("e-1", resultado("s-1", "ana", 1, 8, 10));
        servicio.alCompletarseUnSimulacro("e-1", resultado("s-2", "ana", 2, 5, 10));
        servicio.alCompletarseUnSimulacro("e-3", resultado("s-1", "ana", 1, 8, 10));

        assertThat(servicio.estadisticas("ana").simulacros()).isEqualTo(1);
    }

    @Test
    @DisplayName("el historial sale paginado y el más reciente primero")
    void historialPaginado() {
        servicio.alCompletarseUnSimulacro("e-1", resultado("s-1", "ana", 1, 5, 10));
        servicio.alCompletarseUnSimulacro("e-2", resultado("s-2", "ana", 3, 6, 10));
        servicio.alCompletarseUnSimulacro("e-3", resultado("s-3", "ana", 2, 7, 10));

        var pagina = servicio.historial("ana", 1, 2);

        assertThat(pagina.totalElementos()).isEqualTo(3);
        assertThat(pagina.totalPaginas()).isEqualTo(2);
        assertThat(pagina.elementos()).extracting(r -> r.simulacroId()).containsExactly("s-2", "s-3");
        assertThatThrownBy(() -> servicio.historial("ana", 0, 10)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> servicio.historial("ana", 1, 0)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("las estadísticas se calculan sobre el historial del estudiante")
    void estadisticas() {
        servicio.alCompletarseUnSimulacro("e-1", resultado("s-1", "ana", 1, 4, 10));
        servicio.alCompletarseUnSimulacro("e-2", resultado("s-2", "ana", 2, 5, 10));
        servicio.alCompletarseUnSimulacro("e-3", resultado("s-3", "ana", 3, 8, 10));
        servicio.alCompletarseUnSimulacro("e-4", resultado("s-4", "luis", 1, 1, 10));

        var e = servicio.estadisticas("ana");

        assertThat(e.simulacros()).isEqualTo(3);
        assertThat(e.puntajePromedio()).isEqualTo(56.67);
        assertThat(e.tendencia()).isEqualTo(Tendencia.MEJORANDO);
        assertThat(servicio.estadisticas("nadie").simulacros()).isZero();
    }

    @Test
    @DisplayName("fortalezas y debilidades usan los umbrales configurados")
    void competencias() {
        servicio.alCompletarseUnSimulacro("e-1", resultado("s-1", "ana", 1, 6, 12,
                comp("LECTURA_CRITICA", 5, 6), comp("INGLES", 1, 6)));

        var a = servicio.competencias("ana");

        assertThat(a.fortalezas()).extracting(d -> d.competencia()).containsExactly("LECTURA_CRITICA");
        assertThat(a.debilidades()).extracting(d -> d.competencia()).containsExactly("INGLES");
        assertThat(servicio.umbrales().minimoDePreguntas()).isEqualTo(5);
    }

    @Test
    @DisplayName("el reporte de grupo reúne a todos los estudiantes")
    void reporte() {
        servicio.alCompletarseUnSimulacro("e-1", resultado("s-1", "ana", 1, 9, 10, comp("INGLES", 9, 10)));
        servicio.alCompletarseUnSimulacro("e-2", resultado("s-2", "luis", 1, 3, 10, comp("INGLES", 3, 10)));

        var r = servicio.reporteDeGrupo();

        assertThat(r.estudiantes()).isEqualTo(2);
        assertThat(r.promedioGeneral()).isEqualTo(60.0);
        assertThat(r.generadoEn()).isEqualTo(Resultados.T0);
        assertThat(r.detalle().get(0).estudianteId()).isEqualTo("ana");
    }
}
