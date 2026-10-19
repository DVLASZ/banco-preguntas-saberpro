import { HttpErrorResponse, HttpInterceptorFn, HttpRequest } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, from, switchMap, throwError } from 'rxjs';
import { APP_CONFIG } from '../config';
import { AuthService } from './auth.service';

/**
 * Agrega el token a las llamadas al API. Si el servidor responde 401 (el token venció o se revocó), intenta
 * renovarlo una vez y repite la llamada; si no se puede, cierra la sesión y lleva al inicio de sesión.
 */
export const authInterceptor: HttpInterceptorFn = (peticion, siguiente) => {
  const config = inject(APP_CONFIG);
  const auth = inject(AuthService);
  const router = inject(Router);

  if (!peticion.url.startsWith(config.apiUrl)) {
    return siguiente(peticion);
  }
  const conToken = (req: HttpRequest<unknown>, token: string | null) =>
    token ? req.clone({ setHeaders: { Authorization: `Bearer ${token}` } }) : req;

  return from(auth.tokenVigente()).pipe(
    switchMap((token) => siguiente(conToken(peticion, token))),
    catchError((error: unknown) => {
      if (error instanceof HttpErrorResponse && error.status === 401) {
        return from(auth.renovar()).pipe(
          switchMap(async (renovada) => (renovada ? await auth.tokenVigente() : null)),
          switchMap((token) => {
            if (!token) {
              void auth.cerrarSesion().then(() => router.navigate(['/login'], { queryParams: { expirada: 1 } }));
              return throwError(() => error);
            }
            return siguiente(conToken(peticion, token));
          }),
        );
      }
      return throwError(() => error);
    }),
  );
};
