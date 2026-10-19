import { Routes } from '@angular/router';
import { authGuard, inicioGuard, invitadoGuard, rolGuard } from './core/auth/guards';
import { Marco } from './layout/marco';

export const routes: Routes = [
  { path: '', pathMatch: 'full', canActivate: [inicioGuard], children: [] },
  {
    path: 'login',
    canActivate: [invitadoGuard],
    loadComponent: () => import('./features/login/login').then((m) => m.Login),
  },
  { path: 'auth/callback', loadComponent: () => import('./features/login/retorno').then((m) => m.Retorno) },
  {
    path: 'sin-acceso',
    canActivate: [authGuard],
    loadComponent: () => import('./features/login/sin-acceso').then((m) => m.SinAcceso),
  },
  {
    path: '',
    component: Marco,
    canActivate: [authGuard],
    children: [
    ],
  },
  { path: '**', redirectTo: '' },
];
