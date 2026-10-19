import { Rol } from '../models';

/** El usuario que identifica un token de acceso de Keycloak. */
export interface UsuarioSesion {
  /** Nombre de usuario: el mismo identificador que usan los servicios. */
  id: string;
  nombre: string;
  correo: string | null;
  roles: string[];
}

/** Roles que reconoce la aplicación, en el orden en que se muestran. */
export const ROLES: readonly Rol[] = ['ADMIN', 'AUTHOR', 'REVIEWER'];

function decodificarBase64Url(texto: string): string {
  const base64 = texto.replace(/-/g, '+').replace(/_/g, '/');
  const relleno = base64 + '='.repeat((4 - (base64.length % 4)) % 4);
  const binario = atob(relleno);
  const bytes = Uint8Array.from(binario, (c) => c.charCodeAt(0));
  return new TextDecoder().decode(bytes);
}

/** Lee las reclamaciones de un JWT sin validarlo (la validación la hacen el gateway y los servicios). */
export function leerReclamaciones(token: string): Record<string, unknown> {
  const partes = token.split('.');
  if (partes.length < 2) {
    throw new Error('El token no tiene el formato de un JWT');
  }
  return JSON.parse(decodificarBase64Url(partes[1]));
}

export function usuarioDeToken(token: string): UsuarioSesion {
  const r = leerReclamaciones(token);
  const id = String(r['preferred_username'] ?? r['sub'] ?? '');
  const acceso = r['realm_access'] as { roles?: unknown } | undefined;
  const roles = Array.isArray(acceso?.roles)
    ? (acceso.roles as unknown[]).filter((x): x is string => typeof x === 'string').map((x) => x.toUpperCase())
    : [];
  const nombre = String(r['name'] ?? ([r['given_name'], r['family_name']].filter(Boolean).join(' ') || id));
  return { id, nombre, correo: typeof r['email'] === 'string' ? (r['email'] as string) : null, roles };
}

/** Iniciales para el avatar: «Ana Rodríguez» → «AR». */
export function iniciales(nombre: string): string {
  const palabras = nombre.trim().split(/\s+/).filter(Boolean);
  if (palabras.length === 0) {
    return '?';
  }
  const letras = palabras.length === 1 ? palabras[0].slice(0, 2) : palabras[0][0] + palabras[palabras.length - 1][0];
  return letras.toUpperCase();
}
