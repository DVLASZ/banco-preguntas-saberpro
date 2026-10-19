import { Injectable, computed, inject, signal } from '@angular/core';
import { APP_CONFIG } from '../config';
import { Rol } from '../models';
import { UsuarioSesion, usuarioDeToken } from './jwt';
import { aleatorio, desafio } from './pkce';
import { Redirector } from './redirector';

/** Fallo de inicio de sesión que se puede mostrar al usuario. */
export class ErrorDeAutenticacion extends Error {
  constructor(
    mensaje: string,
    readonly motivo: 'credenciales' | 'cuenta' | 'servidor' | 'estado',
  ) {
    super(mensaje);
  }
}

interface Sesion {
  accessToken: string;
  refreshToken: string;
  /** Instante (ms) en que vence el token de acceso. */
  venceEn: number;
}

interface RespuestaToken {
  access_token: string;
  refresh_token: string;
  expires_in: number;
}

const CLAVE_SESION = 'bp.sesion';
const CLAVE_PKCE = 'bp.pkce';
/** Se renueva el token un poco antes de que venza. */
const MARGEN_MS = 20_000;

/**
 * Autenticación contra Keycloak. Hay dos caminos: el formulario de la aplicación (credenciales del usuario
 * contra el endpoint de tokens) y el acceso institucional (redirección a Keycloak con código y PKCE). En ambos,
 * los tokens quedan en el almacenamiento de la pestaña y se renuevan solos antes de vencer.
 */
@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly config = inject(APP_CONFIG);
  private readonly redirector = inject(Redirector);

  private readonly _sesion = signal<Sesion | null>(this.restaurar());
  private renovacion: Promise<boolean> | null = null;

  readonly autenticado = computed(() => this._sesion() !== null);
  readonly usuario = computed<UsuarioSesion | null>(() => {
    const sesion = this._sesion();
    if (!sesion) {
      return null;
    }
    try {
      return usuarioDeToken(sesion.accessToken);
    } catch {
      return null;
    }
  });

  tieneAlgunRol(...roles: Rol[]): boolean {
    const mios = this.usuario()?.roles ?? [];
    return roles.some((r) => mios.includes(r));
  }

  // ------------------------------------------------------------------ inicio de sesión

  /** Inicia sesión con el formulario. @throws ErrorDeAutenticacion si las credenciales no sirven */
  async iniciarSesion(usuario: string, clave: string): Promise<void> {
    const cuerpo = new URLSearchParams({
      grant_type: 'password',
      client_id: this.config.clientId,
      username: usuario.trim(),
      password: clave,
    });
    this.guardar(await this.pedirToken(cuerpo));
  }

  /** Redirige a la pantalla de acceso de Keycloak (código de autorización con PKCE). */
  async iniciarAccesoInstitucional(): Promise<void> {
    const verificador = aleatorio();
    const estado = aleatorio(16);
    sessionStorage.setItem(CLAVE_PKCE, JSON.stringify({ verificador, estado }));
    const parametros = new URLSearchParams({
      client_id: this.config.clientId,
      response_type: 'code',
      scope: 'openid',
      redirect_uri: this.urlDeRetorno(),
      state: estado,
      code_challenge: await desafio(verificador),
      code_challenge_method: 'S256',
    });
    this.redirector.ir(`${this.base()}/protocol/openid-connect/auth?${parametros}`);
  }

  /** Completa el acceso institucional con el código que devolvió Keycloak. */
  async completarAccesoInstitucional(codigo: string, estado: string): Promise<void> {
    const guardado = sessionStorage.getItem(CLAVE_PKCE);
    sessionStorage.removeItem(CLAVE_PKCE);
    const pkce = guardado ? (JSON.parse(guardado) as { verificador: string; estado: string }) : null;
    if (!pkce || pkce.estado !== estado) {
      throw new ErrorDeAutenticacion('La respuesta de acceso no corresponde a tu solicitud. Inténtalo de nuevo.', 'estado');
    }
    const cuerpo = new URLSearchParams({
      grant_type: 'authorization_code',
      client_id: this.config.clientId,
      code: codigo,
      redirect_uri: this.urlDeRetorno(),
      code_verifier: pkce.verificador,
    });
    this.guardar(await this.pedirToken(cuerpo));
  }

  // ------------------------------------------------------------------ tokens

  /** Un token de acceso vigente, renovándolo si está por vencer; `null` si ya no hay sesión. */
  async tokenVigente(): Promise<string | null> {
    const sesion = this._sesion();
    if (!sesion) {
      return null;
    }
    if (Date.now() < sesion.venceEn - MARGEN_MS) {
      return sesion.accessToken;
    }
    return (await this.renovar()) ? (this._sesion()?.accessToken ?? null) : null;
  }

  /** Pide un token nuevo con el de renovación. Varias peticiones simultáneas comparten una sola renovación. */
  renovar(): Promise<boolean> {
    if (this.renovacion) {
      return this.renovacion;
    }
    const sesion = this._sesion();
    if (!sesion) {
      return Promise.resolve(false);
    }
    const cuerpo = new URLSearchParams({
      grant_type: 'refresh_token',
      client_id: this.config.clientId,
      refresh_token: sesion.refreshToken,
    });
    this.renovacion = this.pedirToken(cuerpo)
      .then((nueva) => {
        this.guardar(nueva);
        return true;
      })
      .catch(() => {
        this.limpiar();
        return false;
      })
      .finally(() => (this.renovacion = null));
    return this.renovacion;
  }

  /** Cierra la sesión en Keycloak (si responde) y borra los tokens de este navegador. */
  async cerrarSesion(): Promise<void> {
    const sesion = this._sesion();
    this.limpiar();
    if (!sesion) {
      return;
    }
    try {
      await fetch(`${this.base()}/protocol/openid-connect/logout`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
        body: new URLSearchParams({ client_id: this.config.clientId, refresh_token: sesion.refreshToken }),
      });
    } catch {
      // la sesión local ya se cerró; si Keycloak no responde, el token expira solo
    }
  }

  // ------------------------------------------------------------------ internos

  private base(): string {
    return `${this.config.keycloakUrl}/realms/${this.config.realm}`;
  }

  private urlDeRetorno(): string {
    return `${this.redirector.origen}/auth/callback`;
  }

  private async pedirToken(cuerpo: URLSearchParams): Promise<Sesion> {
    let respuesta: Response;
    try {
      respuesta = await fetch(`${this.base()}/protocol/openid-connect/token`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
        body: cuerpo,
      });
    } catch {
      throw new ErrorDeAutenticacion('No se pudo contactar al servicio de acceso. Revisa tu conexión.', 'servidor');
    }
    if (!respuesta.ok) {
      const error = (await respuesta.json().catch(() => ({}))) as { error?: string; error_description?: string };
      if (respuesta.status >= 500) {
        throw new ErrorDeAutenticacion('El servicio de acceso no está disponible. Inténtalo en unos minutos.', 'servidor');
      }
      if (error.error === 'invalid_grant' && /disabled|not fully set up/i.test(error.error_description ?? '')) {
        throw new ErrorDeAutenticacion('Tu cuenta está deshabilitada o incompleta. Contacta a un administrador.', 'cuenta');
      }
      throw new ErrorDeAutenticacion('Usuario o contraseña incorrectos.', 'credenciales');
    }
    const token = (await respuesta.json()) as RespuestaToken;
    return {
      accessToken: token.access_token,
      refreshToken: token.refresh_token,
      venceEn: Date.now() + token.expires_in * 1000,
    };
  }

  private guardar(sesion: Sesion): void {
    this._sesion.set(sesion);
    try {
      sessionStorage.setItem(CLAVE_SESION, JSON.stringify(sesion));
    } catch {
      // sin almacenamiento disponible: la sesión dura lo que dure la pestaña cargada
    }
  }

  private limpiar(): void {
    this._sesion.set(null);
    try {
      sessionStorage.removeItem(CLAVE_SESION);
    } catch {
      // nada que borrar
    }
  }

  private restaurar(): Sesion | null {
    try {
      const guardada = sessionStorage.getItem(CLAVE_SESION);
      return guardada ? (JSON.parse(guardada) as Sesion) : null;
    } catch {
      return null;
    }
  }
}
