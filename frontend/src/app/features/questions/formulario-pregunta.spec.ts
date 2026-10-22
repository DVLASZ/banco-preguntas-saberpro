import { TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { of } from 'rxjs';
import { QuestionsService } from '../../core/api/questions.service';
import { ContenidoPregunta, Pregunta } from '../../core/models';
import { FormularioPregunta } from './formulario-pregunta';

const valido: ContenidoPregunta = {
  nombre: 'Idea principal de un texto',
  contexto: 'La energía solar se ha vuelto una alternativa limpia y cada vez más barata frente a los combustibles fósiles.',
  enunciado: '¿Cuál es la idea principal del texto?',
  opcionA: 'La energía solar es costosa',
  opcionB: 'La energía solar es una alternativa limpia y económica',
  opcionC: 'Los combustibles fósiles son renovables',
  opcionD: 'La energía solar solo sirve de día',
  respuestaCorrecta: 'B',
  justificacion: 'El texto presenta la energía solar como alternativa limpia y barata.',
  bibliografia: 'Texto de ejemplo, 2026',
  competencia: 'LECTURA_CRITICA',
  tema: 'Idea principal',
  subtema: 'Textos argumentativos',
  dificultad: 'BASICO',
};

describe('FormularioPregunta · envío a revisión', () => {
  const guardada = { id: 'q-1', ...valido, estado: 'BORRADOR' } as unknown as Pregunta;
  const servicio = { crear: vi.fn(), actualizar: vi.fn(), obtener: vi.fn(), enviarARevision: vi.fn() };

  function crear() {
    vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);
    const fixture = TestBed.createComponent(FormularioPregunta);
    fixture.detectChanges();
    (fixture.componentInstance as unknown as { contenido: { set(c: ContenidoPregunta): void } }).contenido.set(valido);
    fixture.detectChanges();
    return fixture;
  }

  const boton = (f: ReturnType<typeof crear>, texto: string) =>
    [...f.nativeElement.querySelectorAll('footer button')].find((b) => (b as HTMLElement).textContent!.includes(texto)) as HTMLButtonElement;

  beforeEach(() => {
    servicio.crear.mockReset().mockReturnValue(of(guardada));
    servicio.enviarARevision.mockReset().mockReturnValue(of(guardada));
    TestBed.configureTestingModule({ providers: [provideRouter([]), { provide: QuestionsService, useValue: servicio }] });
  });

  it('enviar a revisión pide confirmación y todavía no guarda ni envía', () => {
    const f = crear();

    boton(f, 'Enviar a revisión').click();
    f.detectChanges();

    expect(f.nativeElement.querySelector('[role="alertdialog"]')).not.toBeNull();
    expect(f.nativeElement.textContent).toContain('no podrás editarla');
    expect(servicio.crear).not.toHaveBeenCalled();
    expect(servicio.enviarARevision).not.toHaveBeenCalled();
  });

  it('«Seguir editando» cancela el envío', () => {
    const f = crear();
    boton(f, 'Enviar a revisión').click();
    f.detectChanges();

    boton(f, 'Seguir editando').click();
    f.detectChanges();

    expect(f.nativeElement.querySelector('[role="alertdialog"]')).toBeNull();
    expect(servicio.enviarARevision).not.toHaveBeenCalled();
  });

  it('«Confirmar envío» guarda el borrador y lo envía a revisión', () => {
    const f = crear();
    boton(f, 'Enviar a revisión').click();
    f.detectChanges();

    boton(f, 'Confirmar envío').click();

    expect(servicio.crear).toHaveBeenCalledTimes(1);
    expect(servicio.enviarARevision).toHaveBeenCalledWith('q-1');
  });

  it('con campos inválidos no pide confirmación', () => {
    const f = TestBed.createComponent(FormularioPregunta);
    f.detectChanges();

    boton(f as unknown as ReturnType<typeof crear>, 'Enviar a revisión').click();
    f.detectChanges();

    expect(f.nativeElement.querySelector('[role="alertdialog"]')).toBeNull();
    expect(servicio.crear).not.toHaveBeenCalled();
  });
});
