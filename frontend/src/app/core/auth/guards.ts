import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { Rol } from '../models';
import { AuthService } from './auth.service';

/** La página principal de cada rol: lo primero que hace con el sistema. */
export function paginaDeInicio(roles: readonly string[]): string {
  if (roles.includes('AUTHOR')) {
    return '/mis-preguntas';
  }
  if (roles.includes('REVIEWER')) {
    return '/bandeja';
  }
  if (roles.includes('ADMIN')) {
    return '/asignar';
  }
  return '/sin-acceso';
}

/** Exige haber iniciado sesión; si no, lleva a la pantalla de acceso recordando a dónde se iba. */
export const authGuard: CanActivateFn = (_ruta, estado) => {
  const auth = inject(AuthService);
  const router = inject(Router);
  return auth.autenticado() ? true : router.createUrlTree(['/login'], { queryParams: { volverA: estado.url } });
};

/** Exige alguno de los roles; quien no lo tiene vuelve a su página de inicio. */
export function rolGuard(...roles: Rol[]): CanActivateFn {
  return () => {
    const auth = inject(AuthService);
    const router = inject(Router);
    if (!auth.autenticado()) {
      return router.createUrlTree(['/login']);
    }
    return auth.tieneAlgunRol(...roles) ? true : router.createUrlTree([paginaDeInicio(auth.usuario()?.roles ?? [])]);
  };
}

/** La raíz lleva al inicio del rol, o al acceso si no hay sesión. */
export const inicioGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  const router = inject(Router);
  return router.createUrlTree([auth.autenticado() ? paginaDeInicio(auth.usuario()?.roles ?? []) : '/login']);
};

/** El acceso solo se muestra a quien no tiene sesión. */
export const invitadoGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  const router = inject(Router);
  return auth.autenticado() ? router.createUrlTree([paginaDeInicio(auth.usuario()?.roles ?? [])]) : true;
};
