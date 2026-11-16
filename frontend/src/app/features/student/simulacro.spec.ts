import { TestBed } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap, provideRouter } from '@angular/router';
import { of, throwError } from 'rxjs';
import { SimulationsService } from '../../core/api/simulations.service';
import { Simulacro as DatosSimulacro } from '../../core/models';
import { Simulacro } from './simulacro';

function item(orden: number, respuesta?: string, extra: Partial<DatosSimulacro['preguntas'][number]> = {}): DatosSimulacro['preguntas'][number] {
  return {
    orden, preguntaId: `q-${orden}`, competencia: 'INGLES', tema: 'Verbos', dificultad: 'BASICO', contexto: '',
    enunciado: `¿Pregunta ${orden}?`, opcionA: 'uno', opcionB: 'dos', opcionC: 'tres', opcionD: 'cuatro', respuesta, ...extra,
  };
}

function simulacro(estado: DatosSimulacro['estado'], extra: Partial<DatosSimulacro> = {}): DatosSimulacro {
  return {
    id: 's-1', estudianteId: 'est1', estado, iniciadoEn: '2026-10-19T10:00:00Z', venceEn: '2026-10-19T10:30:00Z',
    segundosRestantes: 1800, duracionMinutos: 30, totalPreguntas: 2, respondidas: 0, preguntas: [item(1), item(2)], ...extra,
  };
}

describe('Simulacro', () => {
  const servicio = { obtener: vi.fn(), responder: vi.fn(), finalizar: vi.fn() };

  function crear(s: DatosSimulacro) {
    servicio.obtener.mockReturnValue(of(s));
    const fixture = TestBed.createComponent(Simulacro);
    fixture.detectChanges();
    return fixture;
  }

  const texto = (f: ReturnType<typeof crear>) => (f.nativeElement as HTMLElement).textContent ?? '';
  const boton = (f: ReturnType<typeof crear>, contiene: string) =>
    [...f.nativeElement.querySelectorAll('button')].find((b) => (b as HTMLElement).textContent?.includes(contiene)) as HTMLButtonElement;

  beforeEach(() => {
    servicio.obtener.mockReset();
    servicio.responder.mockReset();
    servicio.finalizar.mockReset();
    TestBed.configureTestingModule({
      providers: [
        provideRouter([]),
        { provide: SimulationsService, useValue: servicio },
        { provide: ActivatedRoute, useValue: { snapshot: { paramMap: convertToParamMap({ id: 's-1' }) } } },
      ],
    });
  });

  it('en curso muestra el reloj, la primera pregunta y no revela la respuesta correcta', () => {
    const f = crear(simulacro('EN_CURSO'));

    expect(texto(f)).toContain('30:00');
    expect(texto(f)).toContain('¿Pregunta 1?');
    expect(texto(f)).toContain('0 de 2 respondidas');
    expect(texto(f)).not.toContain('respuesta correcta');
  });

  it('al elegir una opción la envía al servicio y marca la pregunta como respondida', () => {
    servicio.responder.mockReturnValue(of(simulacro('EN_CURSO', { respondidas: 1, preguntas: [item(1, 'B'), item(2)] })));
    const f = crear(simulacro('EN_CURSO'));

    (f.nativeElement.querySelectorAll('.opcion-simulacro')[1] as HTMLButtonElement).click();
    f.detectChanges();

    expect(servicio.responder).toHaveBeenCalledWith('s-1', 'q-1', 'B');
    expect(texto(f)).toContain('1 de 2 respondidas');
  });

  it('finalizar pide confirmación, avisa cuántas faltan y luego califica', () => {
    servicio.finalizar.mockReturnValue(of(simulacro('FINALIZADO', {
      resultado: { total: 2, correctas: 1, incorrectas: 1, sinResponder: 0, puntaje: 50, porCompetencia: [{ competencia: 'INGLES', correctas: 1, total: 2, porcentaje: 50 }] },
      preguntas: [item(1, 'A', { respuestaCorrecta: 'A', acertada: true }), item(2, 'C', { respuestaCorrecta: 'D', acertada: false })],
    })));
    const f = crear(simulacro('EN_CURSO'));

    boton(f, 'Finalizar simulacro').click();
    f.detectChanges();
    expect(texto(f)).toContain('Tienes 2 pregunta(s) sin responder');
    boton(f, 'Sí, finalizar').click();
    f.detectChanges();

    expect(servicio.finalizar).toHaveBeenCalledWith('s-1');
    expect(texto(f)).toContain('Puntaje sobre 100');
    expect(texto(f)).toContain('50');
    expect(texto(f)).toContain('Revisión de tus respuestas');
  });

  it('un simulacro terminado muestra el resultado con la respuesta correcta y la del estudiante', () => {
    const f = crear(simulacro('EXPIRADO', {
      resultado: { total: 2, correctas: 0, incorrectas: 1, sinResponder: 1, puntaje: 0, porCompetencia: [] },
      preguntas: [item(1, 'C', { respuestaCorrecta: 'D', acertada: false }), item(2, undefined, { respuestaCorrecta: 'A', acertada: false })],
    }));

    expect(texto(f)).toContain('Tiempo agotado');
    expect(texto(f)).toContain('respuesta correcta');
    expect(texto(f)).toContain('tu respuesta');
    expect(texto(f)).toContain('Sin responder');
    expect(f.nativeElement.querySelector('.reloj')).toBeNull();
  });

  it('si no se puede cargar, muestra el error con la opción de reintentar', () => {
    servicio.obtener.mockReturnValue(throwError(() => new Error('x')));
    const f = TestBed.createComponent(Simulacro);
    f.detectChanges();

    expect(texto(f)).toContain('No se pudo cargar el simulacro');
    expect(boton(f, 'Reintentar')).toBeTruthy();
  });
});
