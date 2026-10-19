import { HttpErrorResponse } from '@angular/common/http';
import { TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { of, throwError } from 'rxjs';
import { QuestionsService } from '../../core/api/questions.service';
import { Pagina, Pregunta } from '../../core/models';
import { MisPreguntas } from './mis-preguntas';

function pregunta(id: string, nombre: string, estado: Pregunta['estado'] = 'BORRADOR'): Pregunta {
  return {
    id, autorId: 'autor1', nombre, contexto: 'c', enunciado: 'e', opcionA: 'a', opcionB: 'b', opcionC: 'c', opcionD: 'd', respuestaCorrecta: 'A',
    justificacion: 'j', bibliografia: 'b', competencia: 'LECTURA_CRITICA', tema: 'Patrones', subtema: 'Observer', dificultad: 'INTERMEDIO', estado,
    version: 1, creadaEn: '2026-10-08T10:00:00Z', actualizadaEn: '2026-10-08T10:00:00Z',
  };
}

const pagina = (contenido: Pregunta[], total = contenido.length): Pagina<Pregunta> => ({ contenido, pagina: 0, tamano: 10, totalElementos: total, totalPaginas: Math.max(1, Math.ceil(total / 10)) });

describe('MisPreguntas', () => {
  const servicio = { listarMias: vi.fn() };

  function crear() {
    const fixture = TestBed.createComponent(MisPreguntas);
    fixture.detectChanges();
    return fixture;
  }

  const filas = (f: ReturnType<typeof crear>) => [...f.nativeElement.querySelectorAll('tbody tr.clicable')] as HTMLElement[];

  beforeEach(() => {
    servicio.listarMias.mockReset().mockImplementation((filtro: { estado?: string; size?: number }) => {
      if (filtro.size === 1) {
        const totales: Record<string, number> = { BORRADOR: 2, PENDIENTE_REVISION: 3, EN_REVISION: 4, APROBADA: 1, PUBLICADA: 2 };
        return of(pagina([], filtro.estado ? (totales[filtro.estado] ?? 0) : 12));
      }
      return of(pagina([pregunta('q-1', 'Primera', 'BORRADOR'), pregunta('q-2', 'Segunda', 'RECHAZADA')], 12));
    });
    TestBed.configureTestingModule({ providers: [provideRouter([]), { provide: QuestionsService, useValue: servicio }] });
  });

  it('lista las preguntas con su estado', () => {
    const f = crear();

    expect(filas(f)).toHaveLength(2);
    expect(filas(f)[0].textContent).toContain('Primera');
    expect(filas(f)[1].textContent).toContain('Rechazada');
    expect(f.nativeElement.textContent).toContain('Mostrando 1–10 de 12 preguntas');
  });

  it('el resumen suma los estados: creadas, borradores, en revisión y aprobadas (incluye publicadas)', () => {
    const f = crear();
    const valores = [...f.nativeElement.querySelectorAll('.estadistica .valor')].map((e) => (e as HTMLElement).textContent!.trim());

    expect(valores).toEqual(['12', '2', '7', '3']);
  });

  it('solo los borradores ofrecen editar', () => {
    const f = crear();
    const acciones = filas(f).map((r) => r.querySelector('.accion a')!.textContent!.trim());

    expect(acciones).toEqual(['Editar', 'Ver']);
  });

  it('abrir una fila lleva al detalle', () => {
    const navegar = vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);
    const f = crear();

    filas(f)[1].click();

    expect(navegar).toHaveBeenCalledWith(['/preguntas', 'q-2']);
  });

  it('filtrar por estado vuelve a consultar desde la primera página', () => {
    const f = crear();
    servicio.listarMias.mockClear();
    const select: HTMLSelectElement = f.nativeElement.querySelector('#estado');

    select.value = 'RECHAZADA';
    select.dispatchEvent(new Event('change'));

    expect(servicio.listarMias).toHaveBeenCalledWith(expect.objectContaining({ estado: 'RECHAZADA', page: 0, size: 10 }));
  });

  it('la búsqueda espera a que se deje de escribir', () => {
    vi.useFakeTimers();
    const f = crear();
    servicio.listarMias.mockClear();
    const campo: HTMLInputElement = f.nativeElement.querySelector('#buscar');

    campo.value = 'obs';
    campo.dispatchEvent(new Event('input'));
    campo.value = 'observer';
    campo.dispatchEvent(new Event('input'));
    vi.advanceTimersByTime(100);
    expect(servicio.listarMias).not.toHaveBeenCalled();
    vi.advanceTimersByTime(300);

    expect(servicio.listarMias).toHaveBeenCalledTimes(1);
    expect(servicio.listarMias).toHaveBeenCalledWith(expect.objectContaining({ texto: 'observer' }));
    vi.useRealTimers();
  });

  it('sin preguntas invita a crear la primera', () => {
    servicio.listarMias.mockReturnValue(of(pagina([])));

    const f = crear();

    expect(f.nativeElement.textContent).toContain('Aún no has creado preguntas.');
    expect(f.nativeElement.textContent).toContain('Crear tu primera pregunta');
  });

  it('si los filtros no encuentran nada, lo dice distinto', () => {
    const f = crear();
    servicio.listarMias.mockReturnValue(of(pagina([])));
    const select: HTMLSelectElement = f.nativeElement.querySelector('#estado');

    select.value = 'PUBLICADA';
    select.dispatchEvent(new Event('change'));
    f.detectChanges();

    expect(f.nativeElement.textContent).toContain('Ninguna pregunta coincide con los filtros.');
  });

  it('un error de carga muestra el mensaje y permite reintentar', () => {
    servicio.listarMias.mockReturnValue(throwError(() => new HttpErrorResponse({ status: 503, error: { codigo: 'SERVICIO_NO_DISPONIBLE', mensaje: 'El servicio no está disponible' } })));

    const f = crear();

    expect(f.nativeElement.querySelector('[role=alert]').textContent).toContain('El servicio no está disponible');
    expect(f.nativeElement.textContent).toContain('Reintentar');
  });
});
