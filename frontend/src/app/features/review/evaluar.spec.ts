import { HttpErrorResponse } from '@angular/common/http';
import { TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { of, throwError } from 'rxjs';
import { QuestionsService } from '../../core/api/questions.service';
import { ReviewsService } from '../../core/api/reviews.service';
import { AuthService } from '../../core/auth/auth.service';
import { Pregunta, Revision, RevisorDeRonda, RondaRevision } from '../../core/models';
import { AvisosService } from '../../ui/avisos';
import { Evaluar, situacionDelRevisor } from './evaluar';

const pregunta: Pregunta = {
  id: 'dcde4437-7f0c-45ba-9b41-e89718501908', autorId: 'autor1', nombre: 'Patrón Observer', contexto: 'Contexto', enunciado: '¿Qué patrón?',
  opcionA: 'Factory', opcionB: 'Observer', opcionC: 'Singleton', opcionD: 'Adapter', respuestaCorrecta: 'B', justificacion: 'Porque sí',
  bibliografia: 'Gamma', competencia: 'LECTURA_CRITICA', tema: 'Patrones', subtema: 'Observer', dificultad: 'INTERMEDIO', estado: 'EN_REVISION',
  version: 1, creadaEn: '2026-10-08T10:00:00Z', actualizadaEn: '2026-10-08T10:00:00Z',
};

function revisor(id: string, evaluacion: RevisorDeRonda['evaluacion'] = null): RevisorDeRonda {
  return { revisorId: id, asignadoPor: 'admin', asignadoEn: '2026-10-08T11:00:00Z', automatica: false, evaluacion };
}

function ronda(estado: RondaRevision['estado'], revisores: RevisorDeRonda[]): RondaRevision {
  return {
    numero: 1, versionDePregunta: 1, estado, iniciadaEn: '2026-10-08T10:00:00Z', decididaEn: null, cerradaEn: null,
    totalDeRevisores: revisores.length, totalDeEvaluadas: revisores.filter((r) => r.evaluacion).length, detalleVisible: true, revisores,
  };
}

const revision = (r: RondaRevision): Revision => ({ preguntaId: pregunta.id, autorId: 'autor1', titulo: pregunta.nombre, competencia: 'LECTURA_CRITICA', rondas: [r] });

describe('situacionDelRevisor', () => {
  const yo = revisor('revisor1');

  it('puede evaluar mientras la ronda está en curso', () => {
    expect(situacionDelRevisor(ronda('EN_CURSO', [yo]), yo)).toBe('evaluar');
  });

  it('en una ronda rechazada puede dejar una observación tardía', () => {
    expect(situacionDelRevisor(ronda('RECHAZADA', [yo]), yo)).toBe('tardia');
  });

  it('si ya evaluó, no puede volver a hacerlo', () => {
    const evaluado = revisor('revisor1', { decision: 'APROBAR', observacion: null, enviadaEn: '2026-10-08T12:00:00Z', tardia: false });

    expect(situacionDelRevisor(ronda('EN_CURSO', [evaluado]), evaluado)).toBe('evaluada');
  });

  it('en una ronda terminada ya no se evalúa', () => {
    expect(situacionDelRevisor(ronda('APROBADA', [yo]), yo)).toBe('cerrada');
    expect(situacionDelRevisor(ronda('CERRADA', [yo]), yo)).toBe('cerrada');
  });

  it('quien no está en la ronda no puede evaluar', () => {
    expect(situacionDelRevisor(ronda('EN_CURSO', [yo]), undefined)).toBe('ajena');
    expect(situacionDelRevisor(undefined, yo)).toBe('ajena');
  });
});

describe('Evaluar', () => {
  const preguntas = { obtener: vi.fn() };
  const reviews = { revision: vi.fn(), evaluar: vi.fn() };
  const avisos = { exito: vi.fn(), error: vi.fn() };
  const auth = { usuario: () => ({ id: 'revisor1', nombre: 'Rafael', correo: null, roles: ['REVIEWER'] }) };

  function crear(r: Revision) {
    preguntas.obtener.mockReturnValue(of(pregunta));
    reviews.revision.mockReturnValue(of(r));
    const fixture = TestBed.createComponent(Evaluar);
    fixture.componentRef.setInput('id', pregunta.id);
    fixture.detectChanges();
    return fixture;
  }

  const texto = (f: ReturnType<typeof crear>) => (f.nativeElement as HTMLElement).textContent ?? '';
  const boton = (f: ReturnType<typeof crear>, contiene: string) =>
    [...f.nativeElement.querySelectorAll('button')].find((b) => (b as HTMLElement).textContent?.includes(contiene)) as HTMLButtonElement;
  const escribir = (f: ReturnType<typeof crear>, valor: string) => {
    const area: HTMLTextAreaElement = f.nativeElement.querySelector('textarea');
    area.value = valor;
    area.dispatchEvent(new Event('input'));
    f.detectChanges();
  };

  beforeEach(() => {
    preguntas.obtener.mockReset();
    reviews.revision.mockReset();
    reviews.evaluar.mockReset();
    avisos.exito.mockReset();
    avisos.error.mockReset();
    TestBed.configureTestingModule({
      providers: [
        provideRouter([]),
        { provide: QuestionsService, useValue: preguntas },
        { provide: ReviewsService, useValue: reviews },
        { provide: AuthService, useValue: auth },
        { provide: AvisosService, useValue: avisos },
      ],
    });
  });

  it('muestra el contenido de la pregunta con la respuesta correcta marcada', () => {
    const f = crear(revision(ronda('EN_CURSO', [revisor('revisor1'), revisor('revisor2')])));

    expect(texto(f)).toContain('Patrón Observer');
    expect(texto(f)).toContain('¿Qué patrón?');
    expect(f.nativeElement.querySelector('.opciones li.correcta').textContent).toContain('Observer');
  });

  it('sin decisión el botón de enviar está deshabilitado', () => {
    const f = crear(revision(ronda('EN_CURSO', [revisor('revisor1')])));

    expect(boton(f, 'Enviar evaluación').disabled).toBe(true);
    expect(texto(f)).toContain('Elige una decisión.');
  });

  it('rechazar exige una observación', () => {
    const f = crear(revision(ronda('EN_CURSO', [revisor('revisor1')])));

    boton(f, 'Rechazar').click();
    f.detectChanges();
    expect(boton(f, 'Enviar evaluación').disabled).toBe(true);
    expect(texto(f)).toContain('Observaciones obligatorias');

    escribir(f, 'Hay dos respuestas posibles');
    expect(boton(f, 'Enviar evaluación').disabled).toBe(false);
  });

  it('aprobar no exige observación', () => {
    const f = crear(revision(ronda('EN_CURSO', [revisor('revisor1')])));

    boton(f, 'Aprobar').click();
    f.detectChanges();

    expect(boton(f, 'Enviar evaluación').disabled).toBe(false);
    expect(texto(f)).toContain('Observaciones opcionales');
  });

  it('la observación no puede superar el máximo', () => {
    const f = crear(revision(ronda('EN_CURSO', [revisor('revisor1')])));
    boton(f, 'Aprobar').click();

    escribir(f, 'x'.repeat(4001));

    expect(boton(f, 'Enviar evaluación').disabled).toBe(true);
    expect(texto(f)).toContain('no puede superar 4000');
  });

  it('antes de enviar pide confirmar, porque la evaluación es definitiva', () => {
    const f = crear(revision(ronda('EN_CURSO', [revisor('revisor1')])));
    boton(f, 'Aprobar').click();
    f.detectChanges();

    boton(f, 'Enviar evaluación').click();
    f.detectChanges();

    expect(texto(f)).toContain('tu evaluación es definitiva');
    expect(reviews.evaluar).not.toHaveBeenCalled();
    boton(f, 'Revisar').click();
    f.detectChanges();
    expect(texto(f)).not.toContain('tu evaluación es definitiva');
  });

  it('al confirmar envía la decisión y la observación y vuelve a la bandeja', () => {
    reviews.evaluar.mockReturnValue(of({ resultadoDeLaRonda: 'PENDIENTE', tardia: false, ronda: 1 }));
    const navegar = vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);
    const f = crear(revision(ronda('EN_CURSO', [revisor('revisor1'), revisor('revisor2')])));
    boton(f, 'Rechazar').click();
    escribir(f, 'Falta contexto');
    boton(f, 'Enviar evaluación').click();
    f.detectChanges();

    boton(f, 'Confirmar envío').click();

    expect(reviews.evaluar).toHaveBeenCalledWith(pregunta.id, 'RECHAZAR', 'Falta contexto');
    expect(avisos.exito).toHaveBeenCalledWith('Evaluación enviada. Esperando a los demás revisores.');
    expect(navegar).toHaveBeenCalledWith(['/bandeja']);
  });

  it('el mensaje de éxito depende de lo que decidió la ronda', () => {
    vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);
    const enviar = (resultado: object) => {
      reviews.evaluar.mockReturnValue(of(resultado));
      const f = crear(revision(ronda('EN_CURSO', [revisor('revisor1')])));
      boton(f, 'Aprobar').click();
      f.detectChanges();
      boton(f, 'Enviar evaluación').click();
      f.detectChanges();
      boton(f, 'Confirmar envío').click();
    };

    enviar({ resultadoDeLaRonda: 'APROBADA', tardia: false, ronda: 1 });
    enviar({ resultadoDeLaRonda: 'RECHAZADA', tardia: false, ronda: 1 });
    enviar({ resultadoDeLaRonda: 'PENDIENTE', tardia: true, ronda: 1 });

    expect(avisos.exito.mock.calls.map((c) => c[0])).toEqual([
      'Evaluación enviada. Con ella la pregunta quedó aprobada.',
      'Evaluación enviada. La pregunta fue rechazada y volvió al autor.',
      'Tu observación quedó registrada; la ronda ya había sido rechazada.',
    ]);
  });

  it('si el servidor rechaza el envío muestra el error y se puede reintentar', () => {
    reviews.evaluar.mockReturnValue(
      throwError(() => new HttpErrorResponse({ status: 409, error: { codigo: 'OPERACION_NO_PERMITIDA', mensaje: 'Ya evaluaste esta pregunta' } })),
    );
    const f = crear(revision(ronda('EN_CURSO', [revisor('revisor1')])));
    boton(f, 'Aprobar').click();
    f.detectChanges();
    boton(f, 'Enviar evaluación').click();
    f.detectChanges();

    boton(f, 'Confirmar envío').click();
    f.detectChanges();

    expect(avisos.error).toHaveBeenCalledWith('Ya evaluaste esta pregunta');
    expect(boton(f, 'Enviar evaluación')).toBeTruthy();
  });

  it('en una ronda rechazada avisa que su evaluación será una observación tardía', () => {
    const f = crear(revision(ronda('RECHAZADA', [revisor('revisor1'), revisor('revisor2', { decision: 'RECHAZAR', observacion: 'Mal', enviadaEn: '2026-10-08T12:00:00Z', tardia: false })])));

    expect(texto(f)).toContain('La ronda ya fue rechazada');
    expect(texto(f)).toContain('observación tardía');
  });

  it('si ya evaluó, muestra su evaluación y no el formulario', () => {
    const mia = revisor('revisor1', { decision: 'APROBAR', observacion: 'Bien', enviadaEn: '2026-10-08T12:00:00Z', tardia: false });
    const f = crear(revision(ronda('EN_CURSO', [mia, revisor('revisor2')])));

    expect(texto(f)).toContain('Ya enviaste tu evaluación: Aprobar');
    expect(f.nativeElement.querySelector('textarea')).toBeNull();
  });

  it('muestra lo que dijeron los otros revisores', () => {
    const colega = revisor('revisor2', { decision: 'RECHAZAR', observacion: 'Distractores débiles', enviadaEn: '2026-10-08T12:00:00Z', tardia: false });
    const f = crear(revision(ronda('EN_CURSO', [revisor('revisor1'), colega])));

    expect(texto(f)).toContain('revisor2');
    expect(texto(f)).toContain('Distractores débiles');
    expect(texto(f)).toContain('1 de 1 evaluaciones');
  });

  it('quien no está asignado lo ve claro y no puede evaluar', () => {
    const f = crear(revision(ronda('EN_CURSO', [revisor('otro-revisor')])));

    expect(texto(f)).toContain('No estás asignado a esta pregunta');
    expect(f.nativeElement.querySelector('textarea')).toBeNull();
  });

  it('si no se puede cargar la pregunta, lo dice', () => {
    preguntas.obtener.mockReturnValue(throwError(() => new HttpErrorResponse({ status: 404 })));
    reviews.revision.mockReturnValue(of(revision(ronda('EN_CURSO', []))));
    const f = TestBed.createComponent(Evaluar);
    f.componentRef.setInput('id', 'x');
    f.detectChanges();

    expect(texto(f as never)).toContain('No se pudo cargar la pregunta');
  });
});
