import { TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { of, throwError } from 'rxjs';
import { ReportsService } from '../../core/api/reports.service';
import { ReporteGrupo as DatosReporte } from '../../core/models';
import { ReporteGrupo } from './reporte-grupo';

const reporte: DatosReporte = {
  generadoEn: '2026-10-19T12:00:00Z', estudiantes: 2, simulacros: 5, promedioGeneral: 68.5,
  porCompetencia: [{ competencia: 'INGLES', correctas: 30, total: 40, porcentaje: 75 }],
  estudiantesDetalle: [
    { estudianteId: 'est1', simulacros: 3, promedio: 80, mejorPuntaje: 90, ultimoPuntaje: 85, tendencia: 'MEJORANDO', debilidades: ['LECTURA_CRITICA'] },
    { estudianteId: 'est2', simulacros: 2, promedio: 50, mejorPuntaje: 60, ultimoPuntaje: 40, tendencia: 'BAJANDO', debilidades: [] },
  ],
};

describe('ReporteGrupo', () => {
  const servicio = { grupo: vi.fn(), grupoCsv: vi.fn() };

  function crear() {
    TestBed.configureTestingModule({ providers: [provideRouter([]), { provide: ReportsService, useValue: servicio }] });
    const fixture = TestBed.createComponent(ReporteGrupo);
    fixture.detectChanges();
    return fixture;
  }

  const texto = (f: ReturnType<typeof crear>) => (f.nativeElement as HTMLElement).textContent ?? '';

  beforeEach(() => {
    servicio.grupo.mockReset();
    servicio.grupoCsv.mockReset();
  });

  it('muestra los totales, las competencias del grupo y una fila por estudiante', () => {
    servicio.grupo.mockReturnValue(of(reporte));

    const f = crear();

    expect(texto(f)).toContain('Promedio general');
    expect(texto(f)).toContain('68.5');
    expect(texto(f)).toContain('Inglés');
    expect(f.nativeElement.querySelectorAll('tbody tr').length).toBe(2);
    expect(texto(f)).toContain('Mejorando');
    expect(texto(f)).toContain('Bajando');
    expect(texto(f)).toContain('Lectura crítica');
  });

  it('al abrir un estudiante o buscarlo por su usuario va a su seguimiento', () => {
    servicio.grupo.mockReturnValue(of(reporte));
    const f = crear();
    const navegar = vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);

    (f.nativeElement.querySelector('tbody tr') as HTMLElement).click();
    expect(navegar).toHaveBeenCalledWith(['/estudiantes', 'est1']);

    f.componentInstance['buscado'] = ' est9 ';
    f.componentInstance['buscar']();
    expect(navegar).toHaveBeenLastCalledWith(['/estudiantes', 'est9']);
  });

  it('descarga el CSV como archivo', () => {
    servicio.grupo.mockReturnValue(of(reporte));
    servicio.grupoCsv.mockReturnValue(of(new Blob(['estudiante,promedio'])));
    const crearUrl = vi.fn().mockReturnValue('blob:x');
    Object.assign(URL, { createObjectURL: crearUrl, revokeObjectURL: vi.fn() });
    const f = crear();

    f.componentInstance['descargarCsv']();

    expect(servicio.grupoCsv).toHaveBeenCalled();
    expect(crearUrl).toHaveBeenCalled();
  });

  it('sin estudiantes lo dice, y si falla ofrece reintentar', () => {
    servicio.grupo.mockReturnValue(of({ ...reporte, estudiantes: 0, simulacros: 0, estudiantesDetalle: [], porCompetencia: [] }));
    expect(texto(crear())).toContain('ningún estudiante ha terminado');

    TestBed.resetTestingModule();
    servicio.grupo.mockReturnValue(throwError(() => new Error('x')));
    expect(texto(crear())).toContain('No se pudo cargar el reporte');
  });
});
