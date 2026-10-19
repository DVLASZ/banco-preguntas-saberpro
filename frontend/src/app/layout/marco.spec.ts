import { signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { of } from 'rxjs';
import { NotificationsService } from '../core/api/notifications.service';
import { ReviewsService } from '../core/api/reviews.service';
import { AuthService } from '../core/auth/auth.service';
import { RefrescoService } from '../core/refresco';
import { Marco } from './marco';

describe('Marco', () => {
  let roles: string[];
  const auth = {
    autenticado: () => true,
    usuario: () => ({ id: 'u1', nombre: 'Ana Rodríguez', correo: null, roles }),
    tieneAlgunRol: (...pedidos: string[]) => pedidos.some((r) => roles.includes(r)),
    cerrarSesion: vi.fn(),
  };
  const noLeidas = signal(3);
  const notificaciones = { noLeidas, refrescarNoLeidas: vi.fn() };
  const reviews = { pendientesDeAsignacion: vi.fn(), bandeja: vi.fn() };

  function crear() {
    TestBed.configureTestingModule({
      providers: [
        provideRouter([]),
        { provide: AuthService, useValue: auth },
        { provide: NotificationsService, useValue: notificaciones },
        { provide: ReviewsService, useValue: reviews },
      ],
    });
    const fixture = TestBed.createComponent(Marco);
    fixture.detectChanges();
    return fixture;
  }

  const textos = (f: ReturnType<typeof crear>) => [...f.nativeElement.querySelectorAll('nav .opcion span')].map((e) => (e as HTMLElement).textContent!.trim());

  beforeEach(() => {
    roles = ['AUTHOR'];
    auth.cerrarSesion.mockReset().mockResolvedValue(undefined);
    notificaciones.refrescarNoLeidas.mockReset().mockReturnValue(of({ noLeidas: 3 }));
    reviews.pendientesDeAsignacion.mockReset().mockReturnValue(of({ contenido: [], pagina: 0, tamano: 1, totalElementos: 8, totalPaginas: 8 }));
    reviews.bandeja.mockReset().mockReturnValue(of({ contenido: [], pagina: 0, tamano: 1, totalElementos: 6, totalPaginas: 6 }));
  });

  it('el autor ve sus opciones y no las de administrador ni revisor', () => {
    expect(textos(crear())).toEqual(['Mis preguntas', 'Crear pregunta', 'Notificaciones', 'Historial']);
  });

  it('el revisor ve su bandeja con el número de pendientes', () => {
    roles = ['REVIEWER'];
    const f = crear();

    expect(textos(f)).toEqual(['Bandeja del revisor', 'Notificaciones', 'Historial']);
    expect(f.nativeElement.textContent).toContain('6');
    expect(reviews.pendientesDeAsignacion).not.toHaveBeenCalled();
  });

  it('el administrador ve asignar, publicar, auditoría y el número por asignar', () => {
    roles = ['ADMIN'];
    const f = crear();

    expect(textos(f)).toEqual(['Asignar revisores', 'Publicar preguntas', 'Notificaciones', 'Historial', 'Auditoría']);
    expect(f.nativeElement.textContent).toContain('8');
    expect(reviews.bandeja).not.toHaveBeenCalled();
  });

  it('quien tiene dos roles ve las opciones de ambos', () => {
    roles = ['AUTHOR', 'REVIEWER'];

    expect(textos(crear())).toEqual(['Mis preguntas', 'Crear pregunta', 'Bandeja del revisor', 'Notificaciones', 'Historial']);
  });

  it('muestra el nombre y el rol de la persona y la campana con aviso', () => {
    const f = crear();

    expect(f.nativeElement.querySelector('.persona').textContent).toContain('Ana Rodríguez');
    expect(f.nativeElement.querySelector('.persona').textContent).toContain('Autor');
    expect(f.nativeElement.querySelector('.campana i')).not.toBeNull();
  });

  it('sin notificaciones sin leer la campana no tiene aviso', () => {
    noLeidas.set(0);

    expect(crear().nativeElement.querySelector('.campana i')).toBeNull();
    noLeidas.set(3);
  });

  it('cuando algo cambia, refresca los contadores', () => {
    roles = ['ADMIN'];
    crear();
    reviews.pendientesDeAsignacion.mockClear();

    TestBed.inject(RefrescoService).avisarCambioDeContadores();

    expect(reviews.pendientesDeAsignacion).toHaveBeenCalled();
  });

  it('cerrar sesión cierra y lleva al acceso', async () => {
    const f = crear();
    const navegar = vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);

    (f.nativeElement.querySelector('.salir') as HTMLButtonElement).click();
    await f.whenStable();

    expect(auth.cerrarSesion).toHaveBeenCalled();
    expect(navegar).toHaveBeenCalledWith(['/login']);
  });
});
