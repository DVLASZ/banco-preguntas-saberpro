import { TestBed } from '@angular/core/testing';
import { APP_CONFIG } from '../config';
import { AuthService, ErrorDeAutenticacion } from './auth.service';
import { tokenDePrueba } from '../../testing/tokens';
import { Redirector } from './redirector';

const CONFIG = { apiUrl: 'http://api.test', keycloakUrl: 'http://kc.test', realm: 'banco', clientId: 'web' };

function respuesta(cuerpo: unknown, estado = 200): Response {
  return new Response(JSON.stringify(cuerpo), { status: estado, headers: { 'Content-Type': 'application/json' } });
}

function tokens(usuario = 'autor1', roles = ['AUTHOR'], expiraEn = 300) {
  return {
    access_token: tokenDePrueba({ preferred_username: usuario, name: `Nombre ${usuario}`, realm_access: { roles } }),
    refresh_token: `renovacion-${usuario}`,
    expires_in: expiraEn,
  };
}

describe('AuthService', () => {
  const redirector = { ir: vi.fn(), origen: 'http://localhost:4200' };
  let fetchMock: ReturnType<typeof vi.fn>;

  function crear(): AuthService {
    TestBed.configureTestingModule({
      providers: [
        { provide: APP_CONFIG, useValue: CONFIG },
        { provide: Redirector, useValue: redirector },
      ],
    });
    return TestBed.inject(AuthService);
  }

  beforeEach(() => {
    sessionStorage.clear();
    redirector.ir.mockReset();
    fetchMock = vi.fn();
    vi.stubGlobal('fetch', fetchMock);
  });

  afterEach(() => {
    vi.unstubAllGlobals();
    vi.useRealTimers();
  });

  it('empieza sin sesión', () => {
    const auth = crear();

    expect(auth.autenticado()).toBe(false);
    expect(auth.usuario()).toBeNull();
  });

  describe('formulario', () => {
    it('inicia sesión con las credenciales y deja el usuario y sus roles', async () => {
      fetchMock.mockResolvedValue(respuesta(tokens('revisor1', ['REVIEWER'])));
      const auth = crear();

      await auth.iniciarSesion(' revisor1 ', 'clave');

      expect(auth.autenticado()).toBe(true);
      expect(auth.usuario()?.id).toBe('revisor1');
      expect(auth.tieneAlgunRol('REVIEWER')).toBe(true);
      expect(auth.tieneAlgunRol('ADMIN', 'AUTHOR')).toBe(false);
      const [url, opciones] = fetchMock.mock.calls[0];
      expect(url).toBe('http://kc.test/realms/banco/protocol/openid-connect/token');
      const cuerpo = new URLSearchParams(opciones.body as URLSearchParams);
      expect(cuerpo.get('grant_type')).toBe('password');
      expect(cuerpo.get('client_id')).toBe('web');
      expect(cuerpo.get('username')).toBe('revisor1');
    });

    it('guarda la sesión en la pestaña y la recupera al recargar', async () => {
      fetchMock.mockResolvedValue(respuesta(tokens()));
      await crear().iniciarSesion('autor1', 'clave');
      TestBed.resetTestingModule();

      const recargado = crear();

      expect(recargado.autenticado()).toBe(true);
      expect(recargado.usuario()?.id).toBe('autor1');
    });

    it('credenciales incorrectas: error claro y sin sesión', async () => {
      fetchMock.mockResolvedValue(respuesta({ error: 'invalid_grant', error_description: 'Invalid user credentials' }, 401));
      const auth = crear();

      await expect(auth.iniciarSesion('autor1', 'mala')).rejects.toMatchObject({ motivo: 'credenciales', message: 'Usuario o contraseña incorrectos.' });
      expect(auth.autenticado()).toBe(false);
    });

    it('cuenta deshabilitada: lo dice distinto', async () => {
      fetchMock.mockResolvedValue(respuesta({ error: 'invalid_grant', error_description: 'Account disabled' }, 400));

      await expect(crear().iniciarSesion('u', 'c')).rejects.toMatchObject({ motivo: 'cuenta' });
    });

    it('Keycloak caído: error de servidor', async () => {
      fetchMock.mockResolvedValue(respuesta({}, 503));

      await expect(crear().iniciarSesion('u', 'c')).rejects.toMatchObject({ motivo: 'servidor' });
    });

    it('sin red: error de servidor', async () => {
      fetchMock.mockRejectedValue(new TypeError('Failed to fetch'));

      const error = await crear().iniciarSesion('u', 'c').catch((e) => e);

      expect(error).toBeInstanceOf(ErrorDeAutenticacion);
      expect(error.motivo).toBe('servidor');
    });
  });

  describe('tokens', () => {
    it('entrega el token mientras es vigente, sin pedir otro', async () => {
      const datos = tokens();
      fetchMock.mockResolvedValue(respuesta(datos));
      const auth = crear();
      await auth.iniciarSesion('autor1', 'clave');

      expect(await auth.tokenVigente()).toBe(datos.access_token);
      expect(fetchMock).toHaveBeenCalledTimes(1);
    });

    it('sin sesión no hay token', async () => {
      expect(await crear().tokenVigente()).toBeNull();
    });

    it('renueva el token cuando está por vencer, y varias peticiones comparten una sola renovación', async () => {
      vi.useFakeTimers();
      const primero = tokens('autor1', ['AUTHOR'], 30);
      const segundo = { ...tokens('autor1', ['AUTHOR'], 300), access_token: tokenDePrueba({ preferred_username: 'autor1', n: 2 }) };
      fetchMock.mockResolvedValueOnce(respuesta(primero)).mockResolvedValueOnce(respuesta(segundo));
      const auth = crear();
      await auth.iniciarSesion('autor1', 'clave');
      vi.advanceTimersByTime(15_000); // quedan 15 s: dentro del margen de renovación

      const [a, b] = await Promise.all([auth.tokenVigente(), auth.tokenVigente()]);

      expect(a).toBe(segundo.access_token);
      expect(b).toBe(segundo.access_token);
      expect(fetchMock).toHaveBeenCalledTimes(2);
      const cuerpo = new URLSearchParams(fetchMock.mock.calls[1][1].body as URLSearchParams);
      expect(cuerpo.get('grant_type')).toBe('refresh_token');
      expect(cuerpo.get('refresh_token')).toBe('renovacion-autor1');
    });

    it('si la renovación falla, la sesión se cierra', async () => {
      vi.useFakeTimers();
      fetchMock.mockResolvedValueOnce(respuesta(tokens('autor1', ['AUTHOR'], 10))).mockResolvedValueOnce(respuesta({ error: 'invalid_grant' }, 400));
      const auth = crear();
      await auth.iniciarSesion('autor1', 'clave');
      vi.advanceTimersByTime(5_000);

      expect(await auth.tokenVigente()).toBeNull();
      expect(auth.autenticado()).toBe(false);
      expect(sessionStorage.getItem('bp.sesion')).toBeNull();
    });
  });

  describe('cerrar sesión', () => {
    it('borra la sesión y avisa a Keycloak con el token de renovación', async () => {
      fetchMock.mockResolvedValueOnce(respuesta(tokens())).mockResolvedValueOnce(new Response(null, { status: 204 }));
      const auth = crear();
      await auth.iniciarSesion('autor1', 'clave');

      await auth.cerrarSesion();

      expect(auth.autenticado()).toBe(false);
      expect(sessionStorage.getItem('bp.sesion')).toBeNull();
      const [url, opciones] = fetchMock.mock.calls[1];
      expect(url).toBe('http://kc.test/realms/banco/protocol/openid-connect/logout');
      expect(new URLSearchParams(opciones.body as URLSearchParams).get('refresh_token')).toBe('renovacion-autor1');
    });

    it('cierra la sesión local aunque Keycloak no responda', async () => {
      fetchMock.mockResolvedValueOnce(respuesta(tokens())).mockRejectedValueOnce(new TypeError('sin red'));
      const auth = crear();
      await auth.iniciarSesion('autor1', 'clave');

      await expect(auth.cerrarSesion()).resolves.toBeUndefined();

      expect(auth.autenticado()).toBe(false);
    });

    it('sin sesión no hace nada', async () => {
      await crear().cerrarSesion();

      expect(fetchMock).not.toHaveBeenCalled();
    });
  });

  describe('acceso institucional', () => {
    it('redirige a Keycloak con PKCE (S256) y un estado propio', async () => {
      const auth = crear();

      await auth.iniciarAccesoInstitucional();

      const destino = new URL(redirector.ir.mock.calls[0][0] as string);
      expect(destino.origin + destino.pathname).toBe('http://kc.test/realms/banco/protocol/openid-connect/auth');
      expect(destino.searchParams.get('response_type')).toBe('code');
      expect(destino.searchParams.get('client_id')).toBe('web');
      expect(destino.searchParams.get('code_challenge_method')).toBe('S256');
      expect(destino.searchParams.get('code_challenge')).toBeTruthy();
      expect(destino.searchParams.get('redirect_uri')).toBe('http://localhost:4200/auth/callback');
      expect(destino.searchParams.get('state')).toBeTruthy();
    });

    it('al volver, cambia el código por tokens usando el verificador guardado', async () => {
      const auth = crear();
      await auth.iniciarAccesoInstitucional();
      const estado = new URL(redirector.ir.mock.calls[0][0] as string).searchParams.get('state')!;
      fetchMock.mockResolvedValue(respuesta(tokens('admin', ['ADMIN'])));

      await auth.completarAccesoInstitucional('codigo-123', estado);

      expect(auth.usuario()?.id).toBe('admin');
      const cuerpo = new URLSearchParams(fetchMock.mock.calls[0][1].body as URLSearchParams);
      expect(cuerpo.get('grant_type')).toBe('authorization_code');
      expect(cuerpo.get('code')).toBe('codigo-123');
      expect(cuerpo.get('code_verifier')).toBeTruthy();
    });

    it('rechaza un estado que no corresponde a la solicitud', async () => {
      const auth = crear();
      await auth.iniciarAccesoInstitucional();

      await expect(auth.completarAccesoInstitucional('codigo', 'estado-ajeno')).rejects.toMatchObject({ motivo: 'estado' });
      expect(fetchMock).not.toHaveBeenCalled();
    });

    it('sin solicitud previa tampoco se acepta', async () => {
      await expect(crear().completarAccesoInstitucional('codigo', 'x')).rejects.toBeInstanceOf(ErrorDeAutenticacion);
    });

    it('la solicitud solo sirve una vez', async () => {
      const auth = crear();
      await auth.iniciarAccesoInstitucional();
      const estado = new URL(redirector.ir.mock.calls[0][0] as string).searchParams.get('state')!;
      fetchMock.mockResolvedValue(respuesta(tokens()));
      await auth.completarAccesoInstitucional('codigo', estado);

      await expect(auth.completarAccesoInstitucional('codigo', estado)).rejects.toMatchObject({ motivo: 'estado' });
    });
  });
});
