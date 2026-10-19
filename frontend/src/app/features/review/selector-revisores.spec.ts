import { TestBed } from '@angular/core/testing';
import { of, throwError } from 'rxjs';
import { HttpErrorResponse } from '@angular/common/http';
import { ReviewsService } from '../../core/api/reviews.service';
import { UsersService } from '../../core/api/users.service';
import { Usuario } from '../../core/models';
import { AvisosService } from '../../ui/avisos';
import { evaluarSeleccion, SelectorRevisores } from './selector-revisores';

describe('evaluarSeleccion', () => {
  it('sin nadie marcado no es válida', () => {
    const e = evaluarSeleccion(0, 0, 5);

    expect(e.valida).toBe(false);
    expect(e.motivo).toBe('Selecciona al menos un revisor.');
  });

  it('entre 1 y el máximo es válida', () => {
    expect(evaluarSeleccion(1, 0, 3).valida).toBe(true);
    expect(evaluarSeleccion(3, 0, 3).valida).toBe(true);
  });

  it('pasarse del máximo no es válido y dice cuántos sobran', () => {
    const e = evaluarSeleccion(4, 0, 3);

    expect(e.valida).toBe(false);
    expect(e.exceso).toBe(1);
    expect(e.motivo).toContain('límite de 3 revisores');
  });

  it('cuenta los revisores que la ronda ya tiene', () => {
    expect(evaluarSeleccion(1, 2, 3).valida).toBe(true);
    expect(evaluarSeleccion(2, 2, 3)).toMatchObject({ valida: false, exceso: 1, total: 4 });
  });
});

describe('SelectorRevisores', () => {
  const usuario = (id: string, nombre: string): Usuario => ({ id, nombre, correo: null, roles: ['REVIEWER'], activo: true, ultimoInicioDeSesion: null });
  const candidatos = [usuario('revisor1', 'Rafael Uno'), usuario('revisor2', 'Rocío Dos'), usuario('autor1', 'Andrés Autor'), usuario('revisor3', 'Ricardo Tres')];
  const usuarios = { listar: vi.fn() };
  const reviews = { asignar: vi.fn() };
  const avisos = { error: vi.fn(), exito: vi.fn() };

  function crear(extra: { yaAsignados?: string[]; maximo?: number } = {}) {
    const fixture = TestBed.createComponent(SelectorRevisores);
    fixture.componentRef.setInput('preguntaId', 'q-1');
    fixture.componentRef.setInput('autorId', 'autor1');
    fixture.componentRef.setInput('maximo', extra.maximo ?? 3);
    fixture.componentRef.setInput('yaAsignados', extra.yaAsignados ?? []);
    fixture.detectChanges();
    return fixture;
  }

  const casillas = (f: ReturnType<typeof crear>): HTMLInputElement[] => [...f.nativeElement.querySelectorAll('input[type=checkbox]')];
  const boton = (f: ReturnType<typeof crear>, texto: string): HTMLButtonElement =>
    [...f.nativeElement.querySelectorAll('button')].find((b) => (b as HTMLElement).textContent?.includes(texto)) as HTMLButtonElement;

  beforeEach(() => {
    usuarios.listar.mockReset().mockReturnValue(of({ contenido: candidatos, pagina: 0, tamano: 100, totalElementos: 4, totalPaginas: 1 }));
    reviews.asignar.mockReset();
    avisos.error.mockReset();
    TestBed.configureTestingModule({
      providers: [
        { provide: UsersService, useValue: usuarios },
        { provide: ReviewsService, useValue: reviews },
        { provide: AvisosService, useValue: avisos },
      ],
    });
  });

  it('pide solo los revisores activos y no ofrece al autor de la pregunta', () => {
    const f = crear();

    expect(usuarios.listar).toHaveBeenCalledWith({ rol: 'REVIEWER', soloActivos: true, size: 100 });
    expect(casillas(f)).toHaveLength(3);
    expect(f.nativeElement.textContent).not.toContain('Andrés Autor');
  });

  it('no ofrece a los revisores que la ronda ya tiene', () => {
    const f = crear({ yaAsignados: ['revisor1'] });

    expect(casillas(f)).toHaveLength(2);
    expect(f.nativeElement.textContent).toContain('Ya asignados: revisor1');
  });

  it('el contador y el botón siguen la selección', () => {
    const f = crear({ maximo: 3 });
    expect(f.nativeElement.querySelector('.contador').textContent.trim()).toBe('0 / 3');
    expect(boton(f, 'Asignar revisores').disabled).toBe(true);

    casillas(f)[0].click();
    f.detectChanges();

    expect(f.nativeElement.querySelector('.contador').textContent.trim()).toBe('1 / 3');
    expect(boton(f, 'Asignar revisores').disabled).toBe(false);
    expect(f.nativeElement.textContent).toContain('Selección válida');
  });

  it('al pasarse del máximo muestra el aviso y bloquea el botón', () => {
    const f = crear({ maximo: 2 });

    casillas(f).forEach((c) => c.click());
    f.detectChanges();

    expect(f.nativeElement.querySelector('.contador').classList.contains('excedido')).toBe(true);
    expect(f.nativeElement.textContent).toContain('Has superado el límite de 2 revisores');
    expect(f.nativeElement.textContent).toContain('Selección no válida');
    expect(boton(f, 'Asignar revisores').disabled).toBe(true);
  });

  it('desmarcar y limpiar vuelven a la selección vacía', () => {
    const f = crear();
    casillas(f).slice(0, 2).forEach((c) => c.click());
    f.detectChanges();
    casillas(f)[0].click();
    f.detectChanges();
    expect(f.nativeElement.querySelector('.contador').textContent.trim()).toBe('1 / 3');

    boton(f, 'Limpiar').click();
    f.detectChanges();

    expect(f.nativeElement.querySelector('.contador').textContent.trim()).toBe('0 / 3');
  });

  it('la búsqueda filtra por nombre o usuario', () => {
    const f = crear();
    const campo: HTMLInputElement = f.nativeElement.querySelector('input[type=search]');

    campo.value = 'rocío';
    campo.dispatchEvent(new Event('input'));
    f.detectChanges();

    expect(casillas(f)).toHaveLength(1);
    campo.value = 'nadie';
    campo.dispatchEvent(new Event('input'));
    f.detectChanges();
    expect(f.nativeElement.textContent).toContain('Ningún revisor coincide');
  });

  it('asigna a los marcados y avisa con la revisión actualizada', () => {
    const revision = { preguntaId: 'q-1', rondas: [] };
    reviews.asignar.mockReturnValue(of(revision));
    const f = crear();
    const recibidas: unknown[] = [];
    f.componentInstance.asignado.subscribe((r: unknown) => recibidas.push(r));
    casillas(f)[0].click();
    casillas(f)[2].click();
    f.detectChanges();

    boton(f, 'Asignar revisores').click();

    expect(reviews.asignar).toHaveBeenCalledWith('q-1', ['revisor1', 'revisor3']);
    expect(recibidas).toEqual([revision]);
  });

  it('si el servidor rechaza la asignación, muestra el error y conserva la selección', () => {
    reviews.asignar.mockReturnValue(
      throwError(() => new HttpErrorResponse({ status: 400, error: { codigo: 'REVISION_INVALIDA', mensaje: 'El autor no puede ser su revisor' } })),
    );
    const f = crear();
    casillas(f)[0].click();
    f.detectChanges();

    boton(f, 'Asignar revisores').click();
    f.detectChanges();

    expect(avisos.error).toHaveBeenCalledWith('El autor no puede ser su revisor');
    expect(f.nativeElement.querySelector('.contador').textContent.trim()).toBe('1 / 3');
  });

  it('si no se puede cargar la lista de revisores lo dice', () => {
    usuarios.listar.mockReturnValue(throwError(() => new HttpErrorResponse({ status: 503 })));

    const f = crear();

    expect(f.nativeElement.textContent).toContain('No se pudo cargar la lista de revisores');
  });
});
