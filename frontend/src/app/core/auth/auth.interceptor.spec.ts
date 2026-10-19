import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { APP_CONFIG } from '../config';
import { AuthService } from './auth.service';
import { authInterceptor } from './auth.interceptor';

const API = 'http://api.test';

describe('authInterceptor', () => {
  let http: HttpClient;
  let controlador: HttpTestingController;
  const auth = {
    tokenVigente: vi.fn(),
    renovar: vi.fn(),
    cerrarSesion: vi.fn(),
  };
  const router = { navigate: vi.fn() };

  /** Deja que las promesas pendientes (renovación de token) avancen. */
  const esperar = () => new Promise((r) => setTimeout(r, 0));

  beforeEach(() => {
    auth.tokenVigente.mockReset().mockResolvedValue('token-1');
    auth.renovar.mockReset();
    auth.cerrarSesion.mockReset().mockResolvedValue(undefined);
    router.navigate.mockReset().mockResolvedValue(true);
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([authInterceptor])),
        provideHttpClientTesting(),
        { provide: APP_CONFIG, useValue: { apiUrl: API, keycloakUrl: 'http://kc.test', realm: 'r', clientId: 'c' } },
        { provide: AuthService, useValue: auth },
        { provide: Router, useValue: router },
      ],
    });
    http = TestBed.inject(HttpClient);
    controlador = TestBed.inject(HttpTestingController);
  });

  afterEach(() => controlador.verify());

  it('agrega el token a las llamadas al API', async () => {
    http.get(`${API}/api/questions`).subscribe();
    await esperar();

    const peticion = controlador.expectOne(`${API}/api/questions`);
    expect(peticion.request.headers.get('Authorization')).toBe('Bearer token-1');
    peticion.flush({});
  });

  it('no envía el token a otras direcciones', async () => {
    http.get('http://otro-sitio.test/datos').subscribe();
    await esperar();

    const peticion = controlador.expectOne('http://otro-sitio.test/datos');
    expect(peticion.request.headers.has('Authorization')).toBe(false);
    peticion.flush({});
  });

  it('si el servidor responde 401, renueva el token y repite la llamada', async () => {
    auth.renovar.mockResolvedValue(true);
    auth.tokenVigente.mockResolvedValueOnce('token-viejo').mockResolvedValue('token-nuevo');
    let resultado: unknown;
    http.get(`${API}/api/reviews/config`).subscribe((r) => (resultado = r));
    await esperar();

    controlador.expectOne(`${API}/api/reviews/config`).flush({ codigo: 'NO_AUTENTICADO' }, { status: 401, statusText: 'Unauthorized' });
    await esperar();

    const repetida = controlador.expectOne(`${API}/api/reviews/config`);
    expect(repetida.request.headers.get('Authorization')).toBe('Bearer token-nuevo');
    repetida.flush({ ok: true });
    expect(resultado).toEqual({ ok: true });
  });

  it('si no se puede renovar, cierra la sesión y lleva al acceso', async () => {
    auth.renovar.mockResolvedValue(false);
    let error: { status: number } | undefined;
    http.get(`${API}/api/questions`).subscribe({ error: (e) => (error = e) });
    await esperar();

    controlador.expectOne(`${API}/api/questions`).flush({}, { status: 401, statusText: 'Unauthorized' });
    await esperar();
    await esperar();

    expect(error?.status).toBe(401);
    expect(auth.cerrarSesion).toHaveBeenCalled();
    expect(router.navigate).toHaveBeenCalledWith(['/login'], { queryParams: { expirada: 1 } });
  });

  it('los demás errores pasan sin intentar renovar', async () => {
    let error: { status: number } | undefined;
    http.get(`${API}/api/questions`).subscribe({ error: (e) => (error = e) });
    await esperar();

    controlador.expectOne(`${API}/api/questions`).flush({}, { status: 403, statusText: 'Forbidden' });

    expect(error?.status).toBe(403);
    expect(auth.renovar).not.toHaveBeenCalled();
  });

  it('sin sesión la llamada sale sin token y el servidor decide', async () => {
    auth.tokenVigente.mockResolvedValue(null);
    http.get(`${API}/api/questions`).subscribe();
    await esperar();

    const peticion = controlador.expectOne(`${API}/api/questions`);
    expect(peticion.request.headers.has('Authorization')).toBe(false);
    peticion.flush({});
  });
});
