import { HttpErrorResponse, HttpParams } from '@angular/common/http';
import { ApiErrorBody } from '../models';

/** Construye los parámetros de consulta omitiendo los vacíos. */
export function parametros(valores: Record<string, string | number | boolean | null | undefined>): HttpParams {
  let p = new HttpParams();
  for (const [clave, valor] of Object.entries(valores)) {
    if (valor !== null && valor !== undefined && valor !== '') {
      p = p.set(clave, String(valor));
    }
  }
  return p;
}

/** El cuerpo de error común de los servicios, si la respuesta lo trae. */
export function cuerpoDeError(error: unknown): ApiErrorBody | null {
  if (error instanceof HttpErrorResponse && error.error && typeof error.error === 'object' && 'codigo' in error.error) {
    return error.error as ApiErrorBody;
  }
  return null;
}

/** Un mensaje claro para mostrar al usuario a partir de cualquier error de una llamada al API. */
export function mensajeDeError(error: unknown): string {
  if (error instanceof HttpErrorResponse) {
    if (error.status === 0) {
      return 'No hay conexión con el servidor. Revisa tu red e inténtalo de nuevo.';
    }
    const cuerpo = cuerpoDeError(error);
    if (cuerpo) {
      const detalles = cuerpo.detalles?.length ? ` (${cuerpo.detalles.join('; ')})` : '';
      return `${cuerpo.mensaje}${detalles}`;
    }
    if (error.status === 401) {
      return 'Tu sesión expiró. Vuelve a iniciar sesión.';
    }
    if (error.status === 403) {
      return 'No tienes permiso para realizar esta operación.';
    }
    if (error.status === 404) {
      return 'No se encontró lo que buscas.';
    }
    if (error.status >= 500) {
      return 'El servidor tuvo un problema. Inténtalo de nuevo en un momento.';
    }
  }
  return 'Ocurrió un error inesperado.';
}
