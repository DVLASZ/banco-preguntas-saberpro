import { ChangeDetectionStrategy, Component, DestroyRef, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { NavigationEnd, Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { catchError, filter, interval, of, startWith } from 'rxjs';
import { NotificationsService } from '../core/api/notifications.service';
import { ReviewsService } from '../core/api/reviews.service';
import { AuthService } from '../core/auth/auth.service';
import { RefrescoService } from '../core/refresco';
import { fecha } from '../core/etiquetas';
import { Rol } from '../core/models';
import { Avatar } from '../ui/avatar';
import { Avisos } from '../ui/avisos';
import { Icono } from '../ui/icono';

interface Opcion {
  ruta: string;
  icono: string;
  texto: string;
  roles: readonly Rol[];
  contador?: 'pendientes' | 'bandeja' | 'notificaciones';
}

/** Las opciones del menú; cada una solo se ve con alguno de sus roles. */
export const OPCIONES: readonly Opcion[] = [
  { ruta: '/mis-preguntas', icono: 'preguntas', texto: 'Mis preguntas', roles: ['AUTHOR'] },
  { ruta: '/crear', icono: 'mas-circulo', texto: 'Crear pregunta', roles: ['AUTHOR'] },
  { ruta: '/asignar', icono: 'usuarios', texto: 'Asignar revisores', roles: ['ADMIN'], contador: 'pendientes' },
  { ruta: '/publicacion', icono: 'publicar', texto: 'Publicar preguntas', roles: ['ADMIN'] },
  { ruta: '/bandeja', icono: 'bandeja', texto: 'Bandeja del revisor', roles: ['REVIEWER'], contador: 'bandeja' },
  { ruta: '/notificaciones', icono: 'campana', texto: 'Notificaciones', roles: ['ADMIN', 'AUTHOR', 'REVIEWER'], contador: 'notificaciones' },
  { ruta: '/historial', icono: 'flujo', texto: 'Historial', roles: ['ADMIN', 'AUTHOR', 'REVIEWER'] },
  { ruta: '/auditoria', icono: 'escudo', texto: 'Auditoría', roles: ['ADMIN'] },
];

const ETIQUETA_ROL: Record<string, string> = { ADMIN: 'Administrador', AUTHOR: 'Autor', REVIEWER: 'Revisor' };

/** El marco de las pantallas con sesión: barra lateral, barra superior y avisos. */
@Component({
  selector: 'bp-marco',
  imports: [RouterOutlet, RouterLink, RouterLinkActive, Icono, Avatar, Avisos],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './marco.html',
  styleUrl: './marco.scss',
})
export class Marco {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  private readonly reviews = inject(ReviewsService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly refresco = inject(RefrescoService);
  protected readonly notificaciones = inject(NotificationsService);

  protected readonly usuario = this.auth.usuario;
  protected readonly hoy = fecha(new Date().toISOString());
  protected readonly titulo = signal('');
  protected readonly pendientes = signal(0);
  protected readonly bandeja = signal(0);

  protected readonly opciones = computed(() => {
    const roles = this.usuario()?.roles ?? [];
    return OPCIONES.filter((o) => o.roles.some((r) => roles.includes(r)));
  });

  protected readonly etiquetaDeRol = computed(() => {
    const roles = this.usuario()?.roles ?? [];
    const conocidas = ['ADMIN', 'AUTHOR', 'REVIEWER'].filter((r) => roles.includes(r)).map((r) => ETIQUETA_ROL[r]);
    return conocidas.length ? conocidas.join(' · ') : 'Sin rol';
  });

  protected readonly perfil = computed(() => `Perfil de ${this.etiquetaDeRol().toLowerCase()}`);

  constructor() {
    this.router.events
      .pipe(
        filter((e) => e instanceof NavigationEnd),
        startWith(null),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe(() => {
        this.titulo.set(this.tituloDeLaRuta());
        this.refrescarContadores();
      });
    this.refresco.contadores$
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(() => this.refrescarContadores());
    interval(30_000)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(() => this.refrescarContadores());
  }

  protected contador(opcion: Opcion): number {
    switch (opcion.contador) {
      case 'pendientes':
        return this.pendientes();
      case 'bandeja':
        return this.bandeja();
      case 'notificaciones':
        return this.notificaciones.noLeidas();
      default:
        return 0;
    }
  }

  protected async salir(): Promise<void> {
    await this.auth.cerrarSesion();
    await this.router.navigate(['/login']);
  }

  private tituloDeLaRuta(): string {
    let ruta = this.router.routerState.snapshot.root;
    let titulo = '';
    while (ruta) {
      titulo = (ruta.data['titulo'] as string | undefined) ?? titulo;
      ruta = ruta.firstChild as typeof ruta;
    }
    return titulo;
  }

  private refrescarContadores(): void {
    if (!this.auth.autenticado()) {
      return;
    }
    this.notificaciones.refrescarNoLeidas().pipe(catchError(() => of(null))).subscribe();
    if (this.auth.tieneAlgunRol('ADMIN')) {
      this.reviews
        .pendientesDeAsignacion(0, 1)
        .pipe(catchError(() => of(null)))
        .subscribe((r) => r && this.pendientes.set(r.totalElementos));
    }
    if (this.auth.tieneAlgunRol('REVIEWER')) {
      this.reviews
        .bandeja('PENDIENTES', 0, 1)
        .pipe(catchError(() => of(null)))
        .subscribe((r) => r && this.bandeja.set(r.totalElementos));
    }
  }
}
