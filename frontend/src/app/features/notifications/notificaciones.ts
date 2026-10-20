import { ChangeDetectionStrategy, Component, OnInit, computed, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { mensajeDeError } from '../../core/api/http-util';
import { NotificationsService } from '../../core/api/notifications.service';
import { AuthService } from '../../core/auth/auth.service';
import { NOTIFICACIONES, diaRelativo, hora } from '../../core/etiquetas';
import { Notificacion } from '../../core/models';
import { AvisosService } from '../../ui/avisos';
import { Icono } from '../../ui/icono';

type Pestana = 'todas' | 'noLeidas' | 'revision' | 'sistema';

export interface GrupoDeDia {
  dia: string;
  items: Notificacion[];
}

/** Agrupa las notificaciones (ya ordenadas, la más reciente primero) por día: «Hoy», «Ayer», «6 de octubre». */
export function agruparPorDia(lista: Notificacion[], ahora: Date = new Date()): GrupoDeDia[] {
  const grupos: GrupoDeDia[] = [];
  for (const n of lista) {
    const dia = diaRelativo(n.creadaEn, ahora);
    const ultimo = grupos.at(-1);
    if (ultimo && ultimo.dia === dia) {
      ultimo.items.push(n);
    } else {
      grupos.push({ dia, items: [n] });
    }
  }
  return grupos;
}

/** Notificaciones (V2-08): cada avance de las preguntas, agrupado por día, con las no leídas marcadas. */
@Component({
  selector: 'bp-notificaciones',
  imports: [Icono],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './notificaciones.html',
  styleUrl: './notificaciones.scss',
})
export class Notificaciones implements OnInit {
  private readonly servicio = inject(NotificationsService);
  private readonly router = inject(Router);
  private readonly auth = inject(AuthService);
  private readonly avisos = inject(AvisosService);

  protected readonly tamano = 20;
  protected readonly pestana = signal<Pestana>('todas');
  protected readonly lista = signal<Notificacion[]>([]);
  protected readonly total = signal(0);
  protected readonly pagina = signal(0);
  protected readonly cargando = signal(true);
  protected readonly cargandoMas = signal(false);
  protected readonly error = signal<string | null>(null);

  protected readonly tipos = NOTIFICACIONES;
  protected readonly hora = hora;
  protected readonly noLeidas = this.servicio.noLeidas;
  protected readonly correo = computed(() => this.auth.usuario()?.correo ?? null);

  protected readonly visibles = computed(() => {
    const p = this.pestana();
    return this.lista().filter((n) => {
      if (p === 'noLeidas') {
        return !n.leida;
      }
      if (p === 'revision' || p === 'sistema') {
        return NOTIFICACIONES[n.tipo]?.grupo === p;
      }
      return true;
    });
  });
  protected readonly grupos = computed(() => agruparPorDia(this.visibles()));
  protected readonly hayMas = computed(() => this.lista().length < this.total());

  ngOnInit(): void {
    this.cargar();
    this.servicio.refrescarNoLeidas().subscribe({ error: () => undefined });
  }

  protected cambiarPestana(p: Pestana): void {
    this.pestana.set(p);
  }

  protected cargar(): void {
    this.cargando.set(true);
    this.error.set(null);
    this.servicio.bandeja(false, 0, this.tamano).subscribe({
      next: (r) => {
        this.lista.set(r.contenido);
        this.total.set(r.totalElementos);
        this.pagina.set(0);
        this.cargando.set(false);
      },
      error: (e) => {
        this.error.set(mensajeDeError(e));
        this.cargando.set(false);
      },
    });
  }

  protected cargarMas(): void {
    this.cargandoMas.set(true);
    this.servicio.bandeja(false, this.pagina() + 1, this.tamano).subscribe({
      next: (r) => {
        this.lista.update((l) => [...l, ...r.contenido]);
        this.pagina.set(r.pagina);
        this.cargandoMas.set(false);
      },
      error: (e) => {
        this.cargandoMas.set(false);
        this.avisos.error(mensajeDeError(e));
      },
    });
  }

  protected marcarTodas(): void {
    this.servicio.marcarTodasLeidas().subscribe({
      next: () => this.lista.update((l) => l.map((n) => ({ ...n, leida: true }))),
      error: (e) => this.avisos.error(mensajeDeError(e)),
    });
  }

  /** Abre la notificación: la marca como leída y lleva a la pregunta, según el rol de quien la lee. */
  protected abrir(n: Notificacion): void {
    if (!n.leida) {
      this.servicio.marcarLeida(n.id).subscribe({
        next: () => this.lista.update((l) => l.map((x) => (x.id === n.id ? { ...x, leida: true } : x))),
        error: () => undefined,
      });
    }
    const destino = this.destino(n);
    if (destino) {
      void this.router.navigate(destino);
    }
  }

  protected textoEnlace(n: Notificacion): string {
    switch (n.tipo) {
      case 'REVISORES_ASIGNADOS':
        return this.auth.tieneAlgunRol('REVIEWER') ? 'Evaluar pregunta' : 'Ver estado';
      case 'REVISION_RECHAZADA':
      case 'OBSERVACION_TARDIA':
        return 'Ver observaciones';
      default:
        return 'Ver pregunta';
    }
  }

  private destino(n: Notificacion): string[] | null {
    if (!n.preguntaId) {
      return null;
    }
    if (n.tipo === 'REVISORES_ASIGNADOS' && this.auth.tieneAlgunRol('REVIEWER')) {
      return ['/evaluar', n.preguntaId];
    }
    return this.auth.tieneAlgunRol('AUTHOR', 'ADMIN') ? ['/preguntas', n.preguntaId] : ['/historial', n.preguntaId];
  }
}
