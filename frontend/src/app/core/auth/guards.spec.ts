import { signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { ActivatedRouteSnapshot, Router, RouterStateSnapshot, UrlTree, provideRouter } from '@angular/router';
import { AuthService } from './auth.service';
import { authGuard, inicioGuard, invitadoGuard, paginaDeInicio, rolGuard } from './guards';

describe('guards', () => {
  const autenticado = signal(false);
  const roles = signal<string[]>([]);
  const auth = {
    autenticado,
    usuario: () => (autenticado() ? { id: 'u', nombre: 'U', correo: null, roles: roles() } : null),
    tieneAlgunRol: (...pedidos: string[]) => pedidos.some((r) => roles().includes(r)),
  };

  function ejecutar(guardia: ReturnType<typeof rolGuard>, url = '/destino'): boolean | UrlTree {
    return TestBed.runInInjectionContext(() =>
      guardia({} as ActivatedRouteSnapshot, { url } as RouterStateSnapshot),
    ) as boolean | UrlTree;
  }

  function ruta(resultado: boolean | UrlTree): string {
    return typeof resultado === 'boolean' ? String(resultado) : TestBed.inject(Router).serializeUrl(resultado);
  }

  beforeEach(() => {
    autenticado.set(false);
    roles.set([]);
    TestBed.configureTestingModule({ providers: [provideRouter([]), { provide: AuthService, useValue: auth }] });
  });

  it('cada rol tiene su página de inicio, con prioridad al autor', () => {
    expect(paginaDeInicio(['AUTHOR'])).toBe('/mis-preguntas');
    expect(paginaDeInicio(['REVIEWER'])).toBe('/bandeja');
    expect(paginaDeInicio(['ADMIN'])).toBe('/asignar');
    expect(paginaDeInicio(['ADMIN', 'AUTHOR'])).toBe('/mis-preguntas');
    expect(paginaDeInicio([])).toBe('/sin-acceso');
    expect(paginaDeInicio(['OFFLINE_ACCESS'])).toBe('/sin-acceso');
  });

  it('authGuard deja pasar a quien tiene sesión', () => {
    autenticado.set(true);

    expect(ejecutar(authGuard)).toBe(true);
  });

  it('authGuard lleva al acceso recordando a dónde iba', () => {
    expect(ruta(ejecutar(authGuard, '/mis-preguntas?x=1'))).toBe('/login?volverA=%2Fmis-preguntas%3Fx%3D1');
  });

  it('rolGuard deja pasar a quien tiene alguno de los roles', () => {
    autenticado.set(true);
    roles.set(['REVIEWER']);

    expect(ejecutar(rolGuard('ADMIN', 'REVIEWER'))).toBe(true);
  });

  it('rolGuard devuelve a su inicio a quien no tiene el rol', () => {
    autenticado.set(true);
    roles.set(['AUTHOR']);

    expect(ruta(ejecutar(rolGuard('ADMIN')))).toBe('/mis-preguntas');
  });

  it('rolGuard sin sesión lleva al acceso', () => {
    expect(ruta(ejecutar(rolGuard('ADMIN')))).toBe('/login');
  });

  it('la raíz lleva al inicio del rol o al acceso', () => {
    expect(ruta(ejecutar(inicioGuard))).toBe('/login');
    autenticado.set(true);
    roles.set(['ADMIN']);
    expect(ruta(ejecutar(inicioGuard))).toBe('/asignar');
  });

  it('el acceso solo se muestra a quien no tiene sesión', () => {
    expect(ejecutar(invitadoGuard)).toBe(true);
    autenticado.set(true);
    roles.set(['REVIEWER']);
    expect(ruta(ejecutar(invitadoGuard))).toBe('/bandeja');
  });

  it('una cuenta sin roles del banco va a la pantalla de sin acceso', () => {
    autenticado.set(true);
    roles.set([]);

    expect(ruta(ejecutar(rolGuard('AUTHOR')))).toBe('/sin-acceso');
  });
});
