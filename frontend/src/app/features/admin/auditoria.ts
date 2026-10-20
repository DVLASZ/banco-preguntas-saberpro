import { ChangeDetectionStrategy, Component, DestroyRef, OnInit, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { RouterLink } from '@angular/router';
import { Subject, debounceTime } from 'rxjs';
import { mensajeDeError } from '../../core/api/http-util';
import { HistoryService } from '../../core/api/history.service';
import { NotificationsService } from '../../core/api/notifications.service';
import { ENTRADAS, OPCIONES_TIPO_ENTRADA, codigoCorto, fechaHora } from '../../core/etiquetas';
import { CorreoSimulado, EntradaHistorial, Pagina, TipoEntrada } from '../../core/models';
import { Icono } from '../../ui/icono';
import { Paginacion } from '../../ui/paginacion';

type Vista = 'actividad' | 'correos';

/**
 * Auditoría (solo administrador): toda la actividad del sistema, incluidos los inicios de sesión y los
 * intentos fallidos, y los correos simulados que salieron.
 */
@Component({
  selector: 'bp-auditoria',
  imports: [RouterLink, Icono, Paginacion],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './auditoria.html',
  styleUrl: './auditoria.scss',
})
export class Auditoria implements OnInit {
  private readonly historial = inject(HistoryService);
  private readonly notificaciones = inject(NotificationsService);
  private readonly destroyRef = inject(DestroyRef);

  protected readonly tamano = 15;
  protected readonly tipos = OPCIONES_TIPO_ENTRADA;
  protected readonly entradas = ENTRADAS;
  protected readonly vista = signal<Vista>('actividad');

  protected readonly tipo = signal<TipoEntrada | ''>('');
  protected readonly actor = signal('');
  protected readonly pregunta = signal('');
  protected readonly pagina = signal(0);

  protected readonly actividad = signal<Pagina<EntradaHistorial> | null>(null);
  protected readonly correos = signal<Pagina<CorreoSimulado> | null>(null);
  protected readonly cargando = signal(true);
  protected readonly error = signal<string | null>(null);
  protected readonly abierto = signal<string | null>(null);

  protected readonly fechaHora = fechaHora;
  protected readonly codigoCorto = codigoCorto;

  private readonly busqueda = new Subject<void>();

  ngOnInit(): void {
    this.busqueda.pipe(debounceTime(300), takeUntilDestroyed(this.destroyRef)).subscribe(() => this.cargar());
    this.cargar();
  }

  protected cambiarVista(vista: Vista): void {
    if (vista !== this.vista()) {
      this.vista.set(vista);
      this.pagina.set(0);
      this.abierto.set(null);
      this.cargar();
    }
  }

  protected filtrarTipo(valor: string): void {
    this.tipo.set(valor as TipoEntrada | '');
    this.pagina.set(0);
    this.cargar();
  }

  protected escribirActor(texto: string): void {
    this.actor.set(texto);
    this.pagina.set(0);
    this.busqueda.next();
  }

  protected escribirPregunta(texto: string): void {
    this.pregunta.set(texto);
    this.pagina.set(0);
    this.busqueda.next();
  }

  protected irA(pagina: number): void {
    this.pagina.set(pagina);
    this.cargar();
  }

  protected alternar(id: string): void {
    this.abierto.set(this.abierto() === id ? null : id);
  }

  protected detalles(e: EntradaHistorial): string {
    return e.detalles ? JSON.stringify(e.detalles, null, 2) : '';
  }

  protected cargar(): void {
    this.cargando.set(true);
    this.error.set(null);
    const alFallar = (e: unknown) => {
      this.error.set(mensajeDeError(e));
      this.cargando.set(false);
    };
    if (this.vista() === 'actividad') {
      this.historial
        .auditoria({ tipo: this.tipo(), actorId: this.actor().trim(), preguntaId: this.pregunta().trim(), page: this.pagina(), size: this.tamano })
        .subscribe({
          next: (r) => {
            this.actividad.set(r);
            this.cargando.set(false);
          },
          error: alFallar,
        });
    } else {
      this.notificaciones.correos(this.pagina(), this.tamano).subscribe({
        next: (r) => {
          this.correos.set(r);
          this.cargando.set(false);
        },
        error: alFallar,
      });
    }
  }
}
