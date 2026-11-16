import { TestBed } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap, provideRouter } from '@angular/router';
import { of } from 'rxjs';
import { StatisticsService } from '../../core/api/statistics.service';
import { Estadisticas, Fortalezas } from '../../core/models';
import { Seguimiento } from './seguimiento';

const estadisticas: Estadisticas = {
  estudianteId: 'est1', simulacros: 3, expirados: 1, puntajePromedio: 72.5, mejorPuntaje: 90, peorPuntaje: 50, ultimoPuntaje: 80,
  tiempoPromedioSegundos: 1500, tendencia: 'MEJORANDO',
  porCompetencia: [{ competencia: 'INGLES', correctas: 9, total: 10, porcentaje: 90 }, { competencia: 'LECTURA_CRITICA', correctas: 2, total: 10, porcentaje: 20 }],
};
const fortalezas: Fortalezas = {
  estudianteId: 'est1', umbralFortaleza: 70, umbralDebilidad: 50, minimoDePreguntas: 5,
  fortalezas: [{ competencia: 'INGLES', correctas: 9, total: 10, porcentaje: 90 }],
  debilidades: [{ competencia: 'LECTURA_CRITICA', correctas: 2, total: 10, porcentaje: 20 }],
  intermedias: [], sinDatosSuficientes: [{ competencia: 'COMUNICACION_ESCRITA', correctas: 1, total: 2, porcentaje: 50 }],
};
const historial = {
  contenido: [{ simulacroId: 's-1', iniciadoEn: '2026-10-19T10:00:00Z', finalizadoEn: '2026-10-19T10:25:00Z', duracionSegundos: 1500, expirado: true,
    total: 10, correctas: 8, incorrectas: 1, sinResponder: 1, puntaje: 80, porCompetencia: [] }],
  pagina: 0, tamano: 8, totalElementos: 1, totalPaginas: 1,
};

describe('Seguimiento', () => {
  const servicio = {
    misEstadisticas: vi.fn(), misCompetencias: vi.fn(), miHistorial: vi.fn(),
    estadisticasDe: vi.fn(), competenciasDe: vi.fn(), historialDe: vi.fn(),
  };

  function crear(id: string | null) {
    TestBed.configureTestingModule({
      providers: [
        provideRouter([]),
        { provide: StatisticsService, useValue: servicio },
        { provide: ActivatedRoute, useValue: { snapshot: { paramMap: convertToParamMap(id ? { id } : {}) } } },
      ],
    });
    const fixture = TestBed.createComponent(Seguimiento);
    fixture.detectChanges();
    return fixture;
  }

  const texto = (f: ReturnType<typeof crear>) => (f.nativeElement as HTMLElement).textContent ?? '';

  beforeEach(() => {
    Object.values(servicio).forEach((m) => m.mockReset());
    servicio.misEstadisticas.mockReturnValue(of(estadisticas));
    servicio.misCompetencias.mockReturnValue(of(fortalezas));
    servicio.miHistorial.mockReturnValue(of(historial));
    servicio.estadisticasDe.mockReturnValue(of(estadisticas));
    servicio.competenciasDe.mockReturnValue(of(fortalezas));
    servicio.historialDe.mockReturnValue(of(historial));
  });

  it.each([['SIN_DATOS', 'Sin datos'], ['MEJORANDO', 'Mejorando'], ['ESTABLE', 'Estable'], ['BAJANDO', 'Bajando']] as const)(
    'con la tendencia %s del servicio muestra «%s» y sigue mostrando competencias e historial', (tendencia, etiqueta) => {
      servicio.misEstadisticas.mockReturnValue(of({ ...estadisticas, tendencia }));

      const f = crear(null);

      expect(texto(f)).toContain(etiqueta);
      expect(texto(f)).toContain('Aciertos por competencia');
      expect(texto(f)).toContain('Historial de simulacros');
      expect(texto(f)).toContain('8 de 10');
    });

  it('una tendencia desconocida no deja la pantalla en blanco', () => {
    servicio.misEstadisticas.mockReturnValue(of({ ...estadisticas, tendencia: 'OTRA' as never }));

    const f = crear(null);

    expect(texto(f)).toContain('Sin datos');
    expect(texto(f)).toContain('Historial de simulacros');
  });

  it('para el estudiante muestra su resumen, sus fortalezas y debilidades y su historial', () => {
    const f = crear(null);

    expect(texto(f)).toContain('Mi progreso');
    expect(texto(f)).toContain('72.5');
    expect(texto(f)).toContain('Mejorando');
    expect(texto(f)).toContain('Fortalezas');
    expect(texto(f)).toContain('Por reforzar');
    expect(texto(f)).toContain('Inglés');
    expect(texto(f)).toContain('Faltan al menos 5 preguntas');
    expect(texto(f)).toContain('Tiempo agotado');
    expect(servicio.misEstadisticas).toHaveBeenCalled();
    expect(servicio.estadisticasDe).not.toHaveBeenCalled();
  });

  it('para el docente consulta al estudiante de la ruta y ofrece volver al reporte', () => {
    const f = crear('est1');

    expect(texto(f)).toContain('Seguimiento de est1');
    expect(texto(f)).toContain('Reporte del grupo');
    expect(servicio.estadisticasDe).toHaveBeenCalledWith('est1');
    expect(servicio.historialDe).toHaveBeenCalledWith('est1', 0, 8);
    expect(servicio.misEstadisticas).not.toHaveBeenCalled();
  });

  it('sin simulacros terminados lo dice en vez de mostrar ceros', () => {
    servicio.misEstadisticas.mockReturnValue(of({ ...estadisticas, simulacros: 0, tendencia: 'SIN_DATOS', porCompetencia: [] }));

    const f = crear(null);

    expect(texto(f)).toContain('Todavía no hay simulacros terminados');
  });
});
