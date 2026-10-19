package co.unicauca.bancopreguntas.tracking.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static co.unicauca.bancopreguntas.tracking.domain.Resultados.comp;
import static co.unicauca.bancopreguntas.tracking.domain.Resultados.resultado;
import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ReporteDeGrupo (RF-28)")
class ReporteDeGrupoTest {

    private static final Umbrales UMBRALES = new Umbrales(70, 50, 5);

    private static List<ResultadoRegistrado> datos() {
        return List.of(
                resultado("s-1", "ana", 1, 9, 10, comp("LECTURA_CRITICA", 5, 5), comp("INGLES", 4, 5)),
                resultado("s-2", "ana", 2, 10, 10, comp("LECTURA_CRITICA", 5, 5), comp("INGLES", 5, 5)),
                resultado("s-3", "luis", 1, 3, 10, comp("LECTURA_CRITICA", 2, 5), comp("INGLES", 1, 5)),
                resultado("s-4", "carlos", 1, 6, 10, comp("LECTURA_CRITICA", 3, 5), comp("INGLES", 3, 5)));
    }

    @Test
    @DisplayName("resume el grupo: estudiantes, simulacros, promedio general y desempeño por competencia")
    void resumen() {
        ReporteDeGrupo r = ReporteDeGrupo.de(datos(), UMBRALES, Resultados.T0);

        assertThat(r.estudiantes()).isEqualTo(3);
        assertThat(r.simulacros()).isEqualTo(4);
        assertThat(r.promedioGeneral()).isEqualTo(70.0);
        assertThat(r.generadoEn()).isEqualTo(Resultados.T0);
        assertThat(r.porCompetencia()).containsExactly(
                new DesempenoPorCompetencia("INGLES", 13, 20, 65.0),
                new DesempenoPorCompetencia("LECTURA_CRITICA", 15, 20, 75.0));
    }

    @Test
    @DisplayName("una fila por estudiante, del mejor promedio al peor, con sus debilidades")
    void detalle() {
        ReporteDeGrupo r = ReporteDeGrupo.de(datos(), UMBRALES, Resultados.T0);

        assertThat(r.detalle()).extracting(ResumenDeEstudiante::estudianteId).containsExactly("ana", "carlos", "luis");
        ResumenDeEstudiante ana = r.detalle().get(0);
        assertThat(ana.simulacros()).isEqualTo(2);
        assertThat(ana.promedio()).isEqualTo(95.0);
        assertThat(ana.mejorPuntaje()).isEqualTo(100.0);
        assertThat(ana.ultimoPuntaje()).isEqualTo(100.0);
        assertThat(ana.debilidades()).isEmpty();
        assertThat(r.detalle().get(2).debilidades()).containsExactly("INGLES", "LECTURA_CRITICA");
    }

    @Test
    @DisplayName("a igual promedio ordena por identificador")
    void empate() {
        ReporteDeGrupo r = ReporteDeGrupo.de(List.of(
                resultado("s-1", "zoe", 1, 5, 10), resultado("s-2", "ana", 1, 5, 10)), UMBRALES, Resultados.T0);

        assertThat(r.detalle()).extracting(ResumenDeEstudiante::estudianteId).containsExactly("ana", "zoe");
    }

    @Test
    @DisplayName("sin resultados el reporte está vacío")
    void vacio() {
        ReporteDeGrupo r = ReporteDeGrupo.de(List.of(), UMBRALES, Resultados.T0);

        assertThat(r.estudiantes()).isZero();
        assertThat(r.promedioGeneral()).isZero();
        assertThat(r.detalle()).isEmpty();
        assertThat(r.aCsv()).isEqualTo("estudiante,simulacros,promedio,mejor,ultimo,tendencia,debilidades\n");
    }

    @Test
    @DisplayName("el CSV trae un encabezado y una línea por estudiante con punto decimal")
    void csv() {
        String csv = ReporteDeGrupo.de(datos(), UMBRALES, Resultados.T0).aCsv();

        assertThat(csv.lines().toList()).containsExactly(
                "estudiante,simulacros,promedio,mejor,ultimo,tendencia,debilidades",
                "ana,2,95.00,100.00,100.00,MEJORANDO,",
                "carlos,1,60.00,60.00,60.00,SIN_DATOS,",
                "luis,1,30.00,30.00,30.00,SIN_DATOS,INGLES | LECTURA_CRITICA");
    }

    @Test
    @DisplayName("el CSV escapa las comas y las comillas")
    void csvEscapado() {
        ReporteDeGrupo r = new ReporteDeGrupo(Resultados.T0, 1, 1, 50, List.of(), List.of(
                new ResumenDeEstudiante("a,\"b\"", 1, 50, 50, 50, Tendencia.SIN_DATOS, List.of("X, Y"))));

        assertThat(r.aCsv().lines().toList().get(1)).isEqualTo("\"a,\"\"b\"\"\",1,50.00,50.00,50.00,SIN_DATOS,\"X, Y\"");
    }
}
