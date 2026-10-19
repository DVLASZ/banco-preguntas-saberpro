import { TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { of } from 'rxjs';
import { QuestionsService } from '../../core/api/questions.service';
import { ReviewsService } from '../../core/api/reviews.service';
import { AuthService } from '../../core/auth/auth.service';
import { Pregunta, Revision } from '../../core/models';
import { AvisosService } from '../../ui/avisos';
import { DetallePregunta } from './detalle-pregunta';

function pregunta(estado: Pregunta['estado']): Pregunta {
  return {
    id: 'q-1', autorId: 'autor1', nombre: 'Patrón Observer', contexto: 'Contexto', enunciado: '¿Qué patrón?', opcionA: 'Factory', opcionB: 'Observer',
    opcionC: 'Singleton', opcionD: 'Adapter', respuestaCorrecta: 'B', justificacion: 'Justificación', bibliografia: 'Gamma', competencia: 'LECTURA_CRITICA',
    tema: 'Patrones', subtema: 'Observer', dificultad: 'INTERMEDIO', estado, version: 2, creadaEn: '2026-10-08T10:00:00Z', actualizadaEn: '2026-10-08T11:00:00Z',
    historialEstados: [
      { desde: null, hacia: 'BORRADOR', cambiadoPor: 'autor1', fecha: '2026-10-08T10:00:00Z' },
      { desde: 'BORRADOR', hacia: 'PENDIENTE_REVISION', cambiadoPor: 'autor1', fecha: '2026-10-08T10:30:00Z' },
    ],
  };
}

const revision: Revision = {
  preguntaId: 'q-1', autorId: 'autor1', titulo: 'Patrón Observer', competencia: 'LECTURA_CRITICA',
  rondas: [
    {
      numero: 1, versionDePregunta: 1, estado: 'RECHAZADA', iniciadaEn: '2026-10-08T10:30:00Z', decididaEn: '2026-10-08T12:00:00Z', cerradaEn: null,
      totalDeRevisores: 2, totalDeEvaluadas: 2, detalleVisible: true,
      revisores: [
        { revisorId: 'revisor1', asignadoPor: 'admin', asignadoEn: '2026-10-08T11:00:00Z', automatica: false, evaluacion: { decision: 'RECHAZAR', observacion: 'Dos respuestas posibles', enviadaEn: '2026-10-08T12:00:00Z', tardia: false } },
        { revisorId: 'revisor2', asignadoPor: 'admin', asignadoEn: '2026-10-08T11:00:00Z', automatica: false, evaluacion: { decision: 'APROBAR', observacion: 'Yo la veo bien', enviadaEn: '2026-10-08T13:00:00Z', tardia: true } },
      ],
    },
    {
      numero: 2, versionDePregunta: 2, estado: 'EN_CURSO', iniciadaEn: '2026-10-09T10:00:00Z', decididaEn: null, cerradaEn: null,
      totalDeRevisores: 2, totalDeEvaluadas: 0, detalleVisible: false, revisores: [],
    },
  ],
};

describe('DetallePregunta', () => {
  const preguntas = { obtener: vi.fn(), reabrir: vi.fn(), publicar: vi.fn() };
  const reviews = { revision: vi.fn() };
  const avisos = { exito: vi.fn(), error: vi.fn() };
  let roles: string[];
  let usuarioId: string;
  const auth = { usuario: () => ({ id: usuarioId, nombre: 'N', correo: null, roles }), tieneAlgunRol: (...r: string[]) => r.some((x) => roles.includes(x)) };

  function crear(estado: Pregunta['estado']) {
    preguntas.obtener.mockReturnValue(of(pregunta(estado)));
    reviews.revision.mockReturnValue(of(revision));
    const fixture = TestBed.createComponent(DetallePregunta);
    fixture.componentRef.setInput('id', 'q-1');
    fixture.detectChanges();
    return fixture;
  }

  const texto = (f: ReturnType<typeof crear>) => (f.nativeElement as HTMLElement).textContent ?? '';
  const boton = (f: ReturnType<typeof crear>, contiene: string) =>
    [...f.nativeElement.querySelectorAll('button, a.boton')].find((b) => (b as HTMLElement).textContent?.includes(contiene)) as HTMLElement | undefined;

  beforeEach(() => {
    roles = ['AUTHOR'];
    usuarioId = 'autor1';
    [preguntas.obtener, preguntas.reabrir, preguntas.publicar, reviews.revision, avisos.exito, avisos.error].forEach((m) => m.mockReset());
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

  it('muestra el contenido, el historial de estados y las observaciones de las rondas', () => {
    const f = crear('RECHAZADA');

    expect(texto(f)).toContain('Patrón Observer');
    expect(texto(f)).toContain('Respuesta correcta: B · Observer');
    expect(texto(f)).toContain('Pendiente de revisión');
    expect(texto(f)).toContain('Dos respuestas posibles');
    expect(texto(f)).toContain('Observación tardía');
    expect(texto(f)).toContain('Ronda 2');
  });

  it('las rondas van de la más reciente a la más antigua, y la reciente sin detalle lo explica', () => {
    const f = crear('EN_REVISION');
    const titulos = [...f.nativeElement.querySelectorAll('.ronda h2')].map((e) => (e as HTMLElement).textContent!.trim());

    expect(titulos[0]).toContain('Ronda 2');
    expect(titulos[1]).toContain('Ronda 1');
    expect(texto(f)).toContain('Las observaciones se mostrarán cuando termine la ronda');
  });

  it('el contenido completo se despliega a pedido', () => {
    const f = crear('RECHAZADA');
    expect(f.nativeElement.querySelector('.lista-opciones')).toBeNull();

    boton(f, 'Ver contenido completo')!.click();
    f.detectChanges();

    expect(f.nativeElement.querySelectorAll('.lista-opciones li')).toHaveLength(4);
    expect(f.nativeElement.querySelector('.lista-opciones li.correcta').textContent).toContain('Observer');
  });

  it('el autor de una pregunta rechazada puede corregirla: la reabre y va a editar', () => {
    preguntas.reabrir.mockReturnValue(of({ ...pregunta('BORRADOR'), version: 3 }));
    const navegar = vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);
    const f = crear('RECHAZADA');

    boton(f, 'Corregir pregunta')!.click();

    expect(preguntas.reabrir).toHaveBeenCalledWith('q-1');
    expect(avisos.exito).toHaveBeenCalledWith(expect.stringContaining('versión 3'));
    expect(navegar).toHaveBeenCalledWith(['/preguntas', 'q-1', 'editar']);
  });

  it('solo el autor de una rechazada ve «Corregir»; el de un borrador ve «Editar»', () => {
    expect(boton(crear('EN_REVISION'), 'Corregir pregunta')).toBeUndefined();
    TestBed.resetTestingModule();
    TestBed.configureTestingModule({ providers: [provideRouter([]), { provide: QuestionsService, useValue: preguntas }, { provide: ReviewsService, useValue: reviews }, { provide: AuthService, useValue: auth }, { provide: AvisosService, useValue: avisos }] });

    expect(boton(crear('BORRADOR'), 'Editar borrador')).toBeDefined();
  });

  it('quien no es el autor no puede corregir aunque la pregunta esté rechazada', () => {
    usuarioId = 'otro-autor';

    expect(boton(crear('RECHAZADA'), 'Corregir pregunta')).toBeUndefined();
  });

  it('el administrador ve «Publicar» solo si la pregunta está aprobada', () => {
    roles = ['ADMIN'];
    usuarioId = 'admin';

    expect(boton(crear('APROBADA'), 'Publicar')).toBeDefined();
    TestBed.resetTestingModule();
    TestBed.configureTestingModule({ providers: [provideRouter([]), { provide: QuestionsService, useValue: preguntas }, { provide: ReviewsService, useValue: reviews }, { provide: AuthService, useValue: auth }, { provide: AvisosService, useValue: avisos }] });

    expect(boton(crear('EN_REVISION'), 'Publicar')).toBeUndefined();
  });

  it('un borrador no consulta la revisión', () => {
    crear('BORRADOR');

    expect(reviews.revision).not.toHaveBeenCalled();
  });

  it('el aviso depende del estado: rechazada pide ajustes, aprobada queda lista', () => {
    expect(texto(crear('RECHAZADA'))).toContain('necesita ajustes antes de publicarse');
    TestBed.resetTestingModule();
    TestBed.configureTestingModule({ providers: [provideRouter([]), { provide: QuestionsService, useValue: preguntas }, { provide: ReviewsService, useValue: reviews }, { provide: AuthService, useValue: auth }, { provide: AvisosService, useValue: avisos }] });

    expect(texto(crear('APROBADA'))).toContain('Pregunta aprobada por los revisores');
  });
});
