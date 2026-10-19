import { TestBed } from '@angular/core/testing';
import { Paginacion, paginasVisibles } from './paginacion';

describe('paginasVisibles', () => {
  it('con pocas páginas las muestra todas', () => {
    expect(paginasVisibles(0, 1)).toEqual([0]);
    expect(paginasVisibles(2, 5)).toEqual([0, 1, 2, 3, 4]);
    expect(paginasVisibles(0, 7)).toEqual([0, 1, 2, 3, 4, 5, 6]);
  });

  it('con muchas, deja la primera, la última y las vecinas, con huecos', () => {
    expect(paginasVisibles(0, 20)).toEqual([0, 1, null, 19]);
    expect(paginasVisibles(10, 20)).toEqual([0, null, 9, 10, 11, null, 19]);
    expect(paginasVisibles(19, 20)).toEqual([0, null, 18, 19]);
  });

  it('no deja hueco cuando las páginas son contiguas', () => {
    expect(paginasVisibles(2, 20)).toEqual([0, 1, 2, 3, null, 19]);
  });
});

describe('Paginacion', () => {
  function crear(pagina: number, tamano: number, total: number, elementos = 'preguntas') {
    const fixture = TestBed.createComponent(Paginacion);
    fixture.componentRef.setInput('pagina', pagina);
    fixture.componentRef.setInput('tamano', tamano);
    fixture.componentRef.setInput('total', total);
    fixture.componentRef.setInput('elementos', elementos);
    fixture.detectChanges();
    return fixture;
  }

  it('resume el rango que se está viendo', () => {
    const fixture = crear(1, 5, 24);

    expect(fixture.nativeElement.querySelector('.resumen').textContent).toBe('Mostrando 6–10 de 24 preguntas');
  });

  it('la última página recorta el rango al total', () => {
    expect(crear(4, 5, 24).nativeElement.querySelector('.resumen').textContent).toBe('Mostrando 21–24 de 24 preguntas');
  });

  it('sin elementos lo dice y no muestra la navegación', () => {
    const fixture = crear(0, 10, 0, 'eventos');

    expect(fixture.nativeElement.querySelector('.resumen').textContent).toBe('Sin eventos');
    expect(fixture.nativeElement.querySelector('nav')).toBeNull();
  });

  it('con una sola página no hay navegación', () => {
    expect(crear(0, 10, 7).nativeElement.querySelector('nav')).toBeNull();
  });

  it('emite la página elegida, desde 0, y marca la actual', () => {
    const fixture = crear(0, 5, 24);
    const emitidas: number[] = [];
    fixture.componentInstance.cambia.subscribe((p: number) => emitidas.push(p));

    const numeros: HTMLButtonElement[] = [...fixture.nativeElement.querySelectorAll('.numero')];
    expect(numeros[0].classList.contains('activa')).toBe(true);
    numeros[2].click();

    expect(emitidas).toEqual([2]);
  });

  it('los botones de los extremos se deshabilitan y no emiten', () => {
    const fixture = crear(0, 5, 24);
    const emitidas: number[] = [];
    fixture.componentInstance.cambia.subscribe((p: number) => emitidas.push(p));

    const [anterior, siguiente]: HTMLButtonElement[] = [...fixture.nativeElement.querySelectorAll('.paso')];
    expect(anterior.disabled).toBe(true);
    expect(siguiente.disabled).toBe(false);
    siguiente.click();

    expect(emitidas).toEqual([1]);
  });

  it('pulsar la página actual no emite nada', () => {
    const fixture = crear(2, 5, 24);
    const emitidas: number[] = [];
    fixture.componentInstance.cambia.subscribe((p: number) => emitidas.push(p));

    (fixture.nativeElement.querySelector('.numero.activa') as HTMLButtonElement).click();

    expect(emitidas).toEqual([]);
  });
});
