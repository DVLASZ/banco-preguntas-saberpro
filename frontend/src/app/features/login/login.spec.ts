import { TestBed } from '@angular/core/testing';
import { ActivatedRoute, Router, provideRouter } from '@angular/router';
import { AuthService, ErrorDeAutenticacion } from '../../core/auth/auth.service';
import { Login } from './login';

describe('Login', () => {
  const auth = {
    iniciarSesion: vi.fn(),
    iniciarAccesoInstitucional: vi.fn(),
    usuario: vi.fn(),
  };
  let consulta: Record<string, string>;

  function crear() {
    TestBed.configureTestingModule({
      providers: [
        provideRouter([]),
        { provide: AuthService, useValue: auth },
        { provide: ActivatedRoute, useValue: { snapshot: { queryParamMap: { get: (k: string) => consulta[k] ?? null } } } },
      ],
    });
    const fixture = TestBed.createComponent(Login);
    fixture.detectChanges();
    return fixture;
  }

  const campo = (f: ReturnType<typeof crear>, id: string) => f.nativeElement.querySelector(`#${id}`) as HTMLInputElement;
  const escribir = (f: ReturnType<typeof crear>, id: string, valor: string) => {
    const c = campo(f, id);
    c.value = valor;
    c.dispatchEvent(new Event('input'));
    f.detectChanges();
  };
  const enviar = async (f: ReturnType<typeof crear>) => {
    f.nativeElement.querySelector('form').dispatchEvent(new Event('submit', { cancelable: true }));
    await f.whenStable();
    f.detectChanges();
  };

  beforeEach(() => {
    consulta = {};
    auth.iniciarSesion.mockReset();
    auth.iniciarAccesoInstitucional.mockReset().mockResolvedValue(undefined);
    auth.usuario.mockReset().mockReturnValue({ id: 'autor1', nombre: 'A', correo: null, roles: ['AUTHOR'] });
  });

  it('con los campos vacíos no llama a Keycloak y marca lo que falta', async () => {
    const f = crear();

    await enviar(f);

    expect(auth.iniciarSesion).not.toHaveBeenCalled();
    expect(f.nativeElement.textContent).toContain('Escribe tu usuario o correo.');
    expect(f.nativeElement.textContent).toContain('Escribe tu contraseña.');
  });

  it('inicia sesión y lleva a la página de inicio del rol', async () => {
    auth.iniciarSesion.mockResolvedValue(undefined);
    const f = crear();
    const navegar = vi.spyOn(TestBed.inject(Router), 'navigateByUrl').mockResolvedValue(true);
    escribir(f, 'usuario', 'autor1');
    escribir(f, 'clave', 'secreta');

    await enviar(f);

    expect(auth.iniciarSesion).toHaveBeenCalledWith('autor1', 'secreta');
    expect(navegar).toHaveBeenCalledWith('/mis-preguntas');
  });

  it('vuelve a la página que se pedía si es una ruta de la aplicación', async () => {
    consulta = { volverA: '/preguntas/q-1' };
    auth.iniciarSesion.mockResolvedValue(undefined);
    const f = crear();
    const navegar = vi.spyOn(TestBed.inject(Router), 'navigateByUrl').mockResolvedValue(true);
    escribir(f, 'usuario', 'u');
    escribir(f, 'clave', 'c');

    await enviar(f);

    expect(navegar).toHaveBeenCalledWith('/preguntas/q-1');
  });

  it('nunca redirige a una dirección externa', async () => {
    auth.iniciarSesion.mockResolvedValue(undefined);
    const destinos: unknown[] = [];
    for (const malo of ['//sitio-malicioso.test', 'https://sitio-malicioso.test', 'javascript:alert(1)']) {
      TestBed.resetTestingModule();
      consulta = { volverA: malo };
      const f = crear();
      vi.spyOn(TestBed.inject(Router), 'navigateByUrl').mockImplementation(async (url) => {
        destinos.push(url);
        return true;
      });
      escribir(f, 'usuario', 'u');
      escribir(f, 'clave', 'c');
      await enviar(f);
    }

    expect(destinos).toEqual(['/mis-preguntas', '/mis-preguntas', '/mis-preguntas']);
  });

  it('con credenciales incorrectas muestra el error y vacía la contraseña', async () => {
    auth.iniciarSesion.mockRejectedValue(new ErrorDeAutenticacion('Usuario o contraseña incorrectos.', 'credenciales'));
    const f = crear();
    escribir(f, 'usuario', 'autor1');
    escribir(f, 'clave', 'mala');

    await enviar(f);

    expect(f.nativeElement.querySelector('[role=alert]').textContent).toContain('Usuario o contraseña incorrectos.');
    expect(campo(f, 'clave').value).toBe('');
  });

  it('un error inesperado también se muestra', async () => {
    auth.iniciarSesion.mockRejectedValue(new Error('boom'));
    const f = crear();
    escribir(f, 'usuario', 'u');
    escribir(f, 'clave', 'c');

    await enviar(f);

    expect(f.nativeElement.querySelector('[role=alert]').textContent).toContain('No se pudo iniciar sesión');
  });

  it('avisa cuando la sesión expiró', () => {
    consulta = { expirada: '1' };

    expect(crear().nativeElement.textContent).toContain('Tu sesión expiró');
  });

  it('el acceso institucional y el olvido de contraseña van por Keycloak', () => {
    const f = crear();
    const botones = [...f.nativeElement.querySelectorAll('button')] as HTMLButtonElement[];

    botones.find((b) => b.textContent?.includes('Acceso institucional'))!.click();
    botones.find((b) => b.textContent?.includes('Olvidaste'))!.click();

    expect(auth.iniciarAccesoInstitucional).toHaveBeenCalledTimes(2);
  });
});
