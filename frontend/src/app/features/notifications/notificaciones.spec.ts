import { signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { of } from 'rxjs';
import { NotificationsService } from '../../core/api/notifications.service';
import { AuthService } from '../../core/auth/auth.service';
import { Notificacion } from '../../core/models';
import { AvisosService } from '../../ui/avisos';
import { Notificaciones, agruparPorDia } from './notificaciones';

function notificacion(id: string, tipo: Notificacion['tipo'], creadaEn: string, leida = false): Notificacion {
  return { id, tipo, preguntaId: 'q-1', asunto: `Asunto ${id}`, mensaje: `Mensaje ${id}`, creadaEn, leida, leidaEn: null, correoEnviadoEn: null };
}

describe('agruparPorDia', () => {
  const ahora = new Date('2026-10-08T21:00:00Z');

  it('agrupa por día respetando el orden recibido', () => {
    const grupos = agruparPorDia(
      [
        notificacion('a', 'REVISION_RECHAZADA', '2026-10-08T18:00:00Z'),
        notificacion('b', 'REVISORES_ASIGNADOS', '2026-10-08T14:00:00Z'),
        notificacion('c', 'REVISION_APROBADA', '2026-10-07T15:00:00Z'),
        notificacion('d', 'PREGUNTA_PUBLICADA', '2026-10-06T15:00:00Z'),
      ],
      ahora,
    );

    expect(grupos.map((g) => g.dia)).toEqual(['Hoy · 8 de octubre', 'Ayer · 7 de octubre', '6 de octubre']);
    expect(grupos[0].items.map((n) => n.id)).toEqual(['a', 'b']);
  });

  it('sin notificaciones no hay grupos', () => {
    expect(agruparPorDia([], ahora)).toEqual([]);
  });
});

describe('Notificaciones', () => {
  const noLeidas = signal(2);
  const servicio = {
    bandeja: vi.fn(),
    refrescarNoLeidas: vi.fn(),
    marcarLeida: vi.fn(),
    marcarTodasLeidas: vi.fn(),
    noLeidas,
  };
  let roles: string[];
  const auth = { usuario: () => ({ id: 'u', nombre: 'U', correo: 'u@correo.test', roles }), tieneAlgunRol: (...r: string[]) => r.some((x) => roles.includes(x)) };

  const lista = [
    notificacion('n1', 'REVISION_RECHAZADA', new Date().toISOString()),
    notificacion('n2', 'REVISORES_ASIGNADOS', new Date().toISOString(), true),
    notificacion('n3', 'PREGUNTA_PUBLICADA', new Date().toISOString()),
  ];

  function crear() {
    servicio.bandeja.mockReturnValue(of({ contenido: lista, pagina: 0, tamano: 20, totalElementos: 3, totalPaginas: 1 }));
    const fixture = TestBed.createComponent(Notificaciones);
    fixture.detectChanges();
    return fixture;
  }

  const items = (f: ReturnType<typeof crear>) => [...f.nativeElement.querySelectorAll('.notificacion')] as HTMLElement[];
  const pestana = (f: ReturnType<typeof crear>, texto: string) =>
    [...f.nativeElement.querySelectorAll('.pestanas button')].find((b) => (b as HTMLElement).textContent?.includes(texto)) as HTMLButtonElement;

  beforeEach(() => {
    roles = ['AUTHOR'];
    [servicio.bandeja, servicio.refrescarNoLeidas, servicio.marcarLeida, servicio.marcarTodasLeidas].forEach((f) => f.mockReset());
    servicio.refrescarNoLeidas.mockReturnValue(of({ noLeidas: 2 }));
    servicio.marcarLeida.mockReturnValue(of({}));
    servicio.marcarTodasLeidas.mockReturnValue(of({ marcadas: 2 }));
    TestBed.configureTestingModule({
      providers: [provideRouter([]), { provide: NotificationsService, useValue: servicio }, { provide: AuthService, useValue: auth }, { provide: AvisosService, useValue: { error: vi.fn() } }],
    });
  });

  it('muestra las notificaciones con las no leídas marcadas', () => {
    const f = crear();

    expect(items(f)).toHaveLength(3);
    expect(items(f)[0].classList.contains('sin-leer')).toBe(true);
    expect(items(f)[1].classList.contains('sin-leer')).toBe(false);
    expect(f.nativeElement.textContent).toContain('Hoy ·');
  });

  it('las pestañas filtran por lectura y por grupo', () => {
    const f = crear();

    pestana(f, 'No leídas').click();
    f.detectChanges();
    expect(items(f)).toHaveLength(2);

    pestana(f, 'Sistema').click();
    f.detectChanges();
    expect(items(f)).toHaveLength(1);
    expect(items(f)[0].textContent).toContain('Asunto n3');

    pestana(f, 'Revisión').click();
    f.detectChanges();
    expect(items(f)).toHaveLength(2);
  });

  it('abrir una no leída la marca como leída y lleva a la pregunta', () => {
    const navegar = vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);
    const f = crear();

    items(f)[0].click();
    f.detectChanges();

    expect(servicio.marcarLeida).toHaveBeenCalledWith('n1');
    expect(navegar).toHaveBeenCalledWith(['/preguntas', 'q-1']);
    expect(items(f)[0].classList.contains('sin-leer')).toBe(false);
  });

  it('abrir una ya leída no vuelve a marcarla', () => {
    vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);
    const f = crear();

    items(f)[1].click();

    expect(servicio.marcarLeida).not.toHaveBeenCalled();
  });

  it('un revisor que abre su asignación va directo a evaluar', () => {
    roles = ['REVIEWER'];
    const navegar = vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);
    const f = crear();

    items(f)[1].click();

    expect(navegar).toHaveBeenCalledWith(['/evaluar', 'q-1']);
  });

  it('marcar todas como leídas actualiza la lista', () => {
    const f = crear();

    (f.nativeElement.querySelector('.acciones button') as HTMLButtonElement).click();
    f.detectChanges();

    expect(servicio.marcarTodasLeidas).toHaveBeenCalled();
    expect(items(f).every((i) => !i.classList.contains('sin-leer'))).toBe(true);
  });

  it('sin notificaciones lo dice', () => {
    servicio.bandeja.mockReturnValue(of({ contenido: [], pagina: 0, tamano: 20, totalElementos: 0, totalPaginas: 1 }));
    const f = TestBed.createComponent(Notificaciones);
    f.detectChanges();

    expect(f.nativeElement.textContent).toContain('Todavía no tienes notificaciones');
  });

  it('ofrece cargar más cuando hay anteriores', () => {
    servicio.bandeja.mockReturnValue(of({ contenido: lista, pagina: 0, tamano: 20, totalElementos: 45, totalPaginas: 3 }));
    const f = TestBed.createComponent(Notificaciones);
    f.detectChanges();

    expect(f.nativeElement.textContent).toContain('Cargar notificaciones anteriores');
  });

  it('muestra el correo al que también se envían los avisos', () => {
    expect(crear().nativeElement.textContent).toContain('u@correo.test');
  });
});
