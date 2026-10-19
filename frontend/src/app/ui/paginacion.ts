import { ChangeDetectionStrategy, Component, computed, input, output } from '@angular/core';
import { Icono } from './icono';

/** Páginas que se muestran: la primera, la última y las vecinas de la actual, con huecos marcados con `null`. */
export function paginasVisibles(actual: number, total: number): (number | null)[] {
  if (total <= 7) {
    return Array.from({ length: total }, (_, i) => i);
  }
  const conjunto = new Set<number>([0, total - 1, actual - 1, actual, actual + 1]);
  const orden = [...conjunto].filter((p) => p >= 0 && p < total).sort((a, b) => a - b);
  const resultado: (number | null)[] = [];
  orden.forEach((p, i) => {
    if (i > 0 && p - orden[i - 1] > 1) {
      resultado.push(null);
    }
    resultado.push(p);
  });
  return resultado;
}

/** Paginación numerada («Mostrando 1–5 de 24 preguntas», anterior, números, siguiente). Las páginas cuentan desde 0. */
@Component({
  selector: 'bp-paginacion',
  imports: [Icono],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="paginacion">
      <span class="resumen">{{ resumen() }}</span>
      @if (totalPaginas() > 1) {
        <nav aria-label="Paginación">
          <button type="button" class="paso" [disabled]="pagina() === 0" (click)="ir(pagina() - 1)" aria-label="Página anterior">
            <bp-icono nombre="chevron-izq" [tamano]="16" />
          </button>
          @for (p of paginas(); track $index) {
            @if (p === null) {
              <span class="hueco">…</span>
            } @else {
              <button type="button" class="numero" [class.activa]="p === pagina()" [attr.aria-current]="p === pagina() ? 'page' : null" (click)="ir(p)">
                {{ p + 1 }}
              </button>
            }
          }
          <button type="button" class="paso" [disabled]="pagina() >= totalPaginas() - 1" (click)="ir(pagina() + 1)" aria-label="Página siguiente">
            <bp-icono nombre="chevron-der" [tamano]="16" />
          </button>
        </nav>
      }
    </div>
  `,
  styleUrl: './paginacion.scss',
})
export class Paginacion {
  readonly pagina = input.required<number>();
  readonly tamano = input.required<number>();
  readonly total = input.required<number>();
  /** Cómo se llaman los elementos: «preguntas», «notificaciones»… */
  readonly elementos = input('resultados');
  readonly cambia = output<number>();

  protected readonly totalPaginas = computed(() => Math.max(1, Math.ceil(this.total() / this.tamano())));
  protected readonly paginas = computed(() => paginasVisibles(this.pagina(), this.totalPaginas()));
  protected readonly resumen = computed(() => {
    if (this.total() === 0) {
      return `Sin ${this.elementos()}`;
    }
    const desde = this.pagina() * this.tamano() + 1;
    const hasta = Math.min(this.total(), (this.pagina() + 1) * this.tamano());
    return `Mostrando ${desde}–${hasta} de ${this.total()} ${this.elementos()}`;
  });

  protected ir(pagina: number): void {
    if (pagina >= 0 && pagina < this.totalPaginas() && pagina !== this.pagina()) {
      this.cambia.emit(pagina);
    }
  }
}
