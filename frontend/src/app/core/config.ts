import { InjectionToken } from '@angular/core';

/** Direcciones del sistema. Se leen de `config.json` al arrancar, así la misma compilación sirve en cualquier entorno. */
export interface AppConfig {
  /** Dirección del API Gateway. */
  apiUrl: string;
  /** Dirección de Keycloak vista desde el navegador. */
  keycloakUrl: string;
  realm: string;
  clientId: string;
}

export const CONFIG_POR_DEFECTO: AppConfig = {
  apiUrl: 'http://localhost:8080',
  keycloakUrl: 'http://localhost:8180',
  realm: 'bancopreguntas',
  clientId: 'bancopreguntas-web',
};

export const APP_CONFIG = new InjectionToken<AppConfig>('APP_CONFIG', {
  providedIn: 'root',
  factory: () => CONFIG_POR_DEFECTO,
});

/** Carga `config.json`; si no existe o es inválido, usa los valores por defecto. */
export async function cargarConfig(url = 'config.json'): Promise<AppConfig> {
  try {
    const respuesta = await fetch(url, { cache: 'no-store' });
    if (respuesta.ok) {
      return { ...CONFIG_POR_DEFECTO, ...(await respuesta.json()) };
    }
  } catch {
    // sin archivo de configuración: se usan los valores por defecto
  }
  return CONFIG_POR_DEFECTO;
}
