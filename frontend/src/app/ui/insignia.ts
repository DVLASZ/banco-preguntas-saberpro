import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';
import { ESTADOS, ESTADOS_RONDA, Tono } from '../core/etiquetas';
import { EstadoPregunta, EstadoRonda } from '../core/models';

/** Píldora con un punto de color: «● Rechazada». */
@Component({
  selector: 'bp-insignia',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `<span class="insignia" [class]="'tono-' + tono()"><i></i><ng-content>{{ texto() }}</ng-content></span>`,
  styleUrl: './insignia.scss',
})
export class Insignia {
  readonly texto = input('');
  readonly tono = input<Tono>('gris');
}

/** La insignia del estado de una pregunta. */
@Component({
  selector: 'bp-estado',
  imports: [Insignia],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `<bp-insignia [texto]="datos().texto" [tono]="datos().tono" />`,
})
export class EstadoPreguntaInsignia {
  readonly estado = input.required<EstadoPregunta>();
  protected readonly datos = computed(() => ESTADOS[this.estado()] ?? { texto: this.estado(), tono: 'gris' as Tono });
}

/** La insignia del estado de una ronda de revisión. */
@Component({
  selector: 'bp-estado-ronda',
  imports: [Insignia],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `<bp-insignia [texto]="datos().texto" [tono]="datos().tono" />`,
})
export class EstadoRondaInsignia {
  readonly estado = input.required<EstadoRonda>();
  protected readonly datos = computed(() => ESTADOS_RONDA[this.estado()] ?? { texto: this.estado(), tono: 'gris' as Tono });
}
