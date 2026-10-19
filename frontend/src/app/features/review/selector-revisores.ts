import { ChangeDetectionStrategy, Component, OnInit, computed, inject, input, output, signal } from '@angular/core';
import { mensajeDeError } from '../../core/api/http-util';
import { ReviewsService } from '../../core/api/reviews.service';
import { UsersService } from '../../core/api/users.service';
import { Revision, Usuario } from '../../core/models';
import { Avatar } from '../../ui/avatar';
import { AvisosService } from '../../ui/avisos';
import { Icono } from '../../ui/icono';

export interface EstadoSeleccion {
  /** Revisores que quedarían en la ronda: los ya asignados más los marcados. */
  total: number;
  valida: boolean;
  /** Por qué no es válida (vacío si lo es). */
  motivo: string | null;
  /** Cuántos sobran respecto del máximo (0 si no se pasa). */
  exceso: number;
}

/**
 * Reglas de la selección de revisores: al menos uno nuevo y, entre los ya asignados y los nuevos, no más del
 * máximo configurado. El servidor vuelve a comprobarlas.
 */
export function evaluarSeleccion(marcados: number, yaAsignados: number, maximo: number): EstadoSeleccion {
  const total = marcados + yaAsignados;
  if (marcados === 0) {
    return { total, valida: false, motivo: 'Selecciona al menos un revisor.', exceso: 0 };
  }
  if (total > maximo) {
    return {
      total,
      valida: false,
      motivo: `Has superado el límite de ${maximo} revisores. Hay ${total} seleccionados: desmarca al menos ${total - maximo}; la asignación aún no se ha guardado.`,
      exceso: total - maximo,
    };
  }
  return { total, valida: true, motivo: null, exceso: 0 };
}

/** Lista de revisores disponibles con casillas, contador «n / máximo» y el botón que asigna (HU-04). */
@Component({
  selector: 'bp-selector-revisores',
  imports: [Avatar, Icono],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './selector-revisores.html',
  styleUrl: './selector-revisores.scss',
})
export class SelectorRevisores implements OnInit {
  private readonly usuarios = inject(UsersService);
  private readonly reviews = inject(ReviewsService);
  private readonly avisos = inject(AvisosService);

  readonly preguntaId = input.required<string>();
  readonly autorId = input<string>('');
  /** Revisores que ya tiene la ronda (no se pueden quitar ni repetir). */
  readonly yaAsignados = input<string[]>([]);
  readonly maximo = input.required<number>();
  readonly etiquetaBoton = input('Asignar revisores');

  /** Se emite con la revisión actualizada cuando la asignación se guarda. */
  readonly asignado = output<Revision>();

  protected readonly candidatos = signal<Usuario[]>([]);
  protected readonly cargando = signal(true);
  protected readonly errorDeCarga = signal<string | null>(null);
  protected readonly buscar = signal('');
  protected readonly marcados = signal<ReadonlySet<string>>(new Set());
  protected readonly enviando = signal(false);

  protected readonly visibles = computed(() => {
    const ya = new Set(this.yaAsignados());
    const texto = this.buscar().trim().toLowerCase();
    return this.candidatos()
      .filter((u) => u.id !== this.autorId() && !ya.has(u.id))
      .filter((u) => !texto || `${u.id} ${u.nombre} ${u.correo ?? ''}`.toLowerCase().includes(texto));
  });

  protected readonly seleccion = computed(() => evaluarSeleccion(this.marcados().size, this.yaAsignados().length, this.maximo()));

  ngOnInit(): void {
    this.usuarios.listar({ rol: 'REVIEWER', soloActivos: true, size: 100 }).subscribe({
      next: (p) => {
        this.candidatos.set(p.contenido);
        this.cargando.set(false);
      },
      error: (e) => {
        this.errorDeCarga.set(mensajeDeError(e));
        this.cargando.set(false);
      },
    });
  }

  protected alternar(id: string): void {
    this.marcados.update((m) => {
      const nuevo = new Set(m);
      if (!nuevo.delete(id)) {
        nuevo.add(id);
      }
      return nuevo;
    });
  }

  protected limpiar(): void {
    this.marcados.set(new Set());
  }

  protected asignar(): void {
    if (!this.seleccion().valida || this.enviando()) {
      return;
    }
    this.enviando.set(true);
    this.reviews.asignar(this.preguntaId(), [...this.marcados()]).subscribe({
      next: (revision) => {
        this.enviando.set(false);
        this.marcados.set(new Set());
        this.asignado.emit(revision);
      },
      error: (e) => {
        this.enviando.set(false);
        this.avisos.error(mensajeDeError(e));
      },
    });
  }
}
