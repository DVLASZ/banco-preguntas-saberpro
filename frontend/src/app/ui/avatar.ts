import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';
import { iniciales } from '../core/auth/jwt';

/** Círculo con las iniciales de una persona. */
@Component({
  selector: 'bp-avatar',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `<span class="avatar" [style.width.px]="tamano()" [style.height.px]="tamano()" [style.font-size.px]="tamano() * 0.38">{{ letras() }}</span>`,
  styles: `
    .avatar {
      display: inline-flex;
      align-items: center;
      justify-content: center;
      border-radius: 50%;
      background: var(--superficie-3);
      border: 1px solid var(--borde);
      color: var(--texto);
      font-weight: 600;
    }
  `,
})
export class Avatar {
  readonly nombre = input.required<string>();
  readonly tamano = input(32);
  protected readonly letras = computed(() => iniciales(this.nombre()));
}
