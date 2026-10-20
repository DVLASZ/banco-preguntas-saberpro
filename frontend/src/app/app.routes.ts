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
      // Autor
      {
        path: 'mis-preguntas',
        canActivate: [rolGuard('AUTHOR')],
        data: { titulo: 'Mis preguntas' },
        loadComponent: () => import('./features/questions/mis-preguntas').then((m) => m.MisPreguntas),
      },
      {
        path: 'crear',
        canActivate: [rolGuard('AUTHOR')],
        data: { titulo: 'Crear pregunta' },
        loadComponent: () => import('./features/questions/formulario-pregunta').then((m) => m.FormularioPregunta),
      },
      {
        path: 'preguntas/:id/editar',
        canActivate: [rolGuard('AUTHOR')],
        data: { titulo: 'Corregir pregunta' },
        loadComponent: () => import('./features/questions/formulario-pregunta').then((m) => m.FormularioPregunta),
      },
      {
        path: 'preguntas/:id',
        canActivate: [rolGuard('AUTHOR', 'ADMIN')],
        data: { titulo: 'Detalle de pregunta' },
        loadComponent: () => import('./features/questions/detalle-pregunta').then((m) => m.DetallePregunta),
      },
      // Administrador
      {
        path: 'asignar',
        canActivate: [rolGuard('ADMIN')],
        data: { titulo: 'Asignar revisores' },
        loadComponent: () => import('./features/review/asignar-revisores').then((m) => m.AsignarRevisores),
      },
      {
        path: 'publicacion',
        canActivate: [rolGuard('ADMIN')],
        data: { titulo: 'Publicar preguntas' },
        loadComponent: () => import('./features/admin/publicacion').then((m) => m.Publicacion),
      },
      {
        path: 'auditoria',
        canActivate: [rolGuard('ADMIN')],
        data: { titulo: 'Auditoría' },
        loadComponent: () => import('./features/admin/auditoria').then((m) => m.Auditoria),
      },
      // Revisor
      {
        path: 'bandeja',
        canActivate: [rolGuard('REVIEWER')],
        data: { titulo: 'Bandeja del revisor' },
        loadComponent: () => import('./features/review/bandeja').then((m) => m.Bandeja),
      },
      {
        path: 'evaluar/:id',
        canActivate: [rolGuard('REVIEWER')],
        data: { titulo: 'Bandeja del revisor' },
        loadComponent: () => import('./features/review/evaluar').then((m) => m.Evaluar),
      },
      // Todos los roles
      {
        path: 'notificaciones',
        canActivate: [rolGuard('ADMIN', 'AUTHOR', 'REVIEWER')],
        data: { titulo: 'Notificaciones' },
        loadComponent: () => import('./features/notifications/notificaciones').then((m) => m.Notificaciones),
      },
      {
        path: 'historial',
        canActivate: [rolGuard('ADMIN', 'AUTHOR', 'REVIEWER')],
        data: { titulo: 'Historial' },
        loadComponent: () => import('./features/history/historial').then((m) => m.Historial),
      },
      {
        path: 'historial/:id',
        canActivate: [rolGuard('ADMIN', 'AUTHOR', 'REVIEWER')],
        data: { titulo: 'Historial' },
        loadComponent: () => import('./features/history/historial-pregunta').then((m) => m.HistorialPregunta),
      },
    ],
  },
  { path: '**', redirectTo: '' },
];
