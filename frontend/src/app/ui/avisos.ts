import { ChangeDetectionStrategy, Component, Injectable, inject, signal } from '@angular/core';
import { Icono } from './icono';

export interface Aviso {
  id: number;
  tipo: 'exito' | 'error' | 'info';
  texto: string;
}

/** Mensajes breves que aparecen en una esquina («Pregunta enviada a revisión»). */
@Injectable({ providedIn: 'root' })
export class AvisosService {
  private readonly _avisos = signal<Aviso[]>([]);
  readonly avisos = this._avisos.asReadonly();
  private siguiente = 1;

  exito(texto: string): void {
    this.mostrar('exito', texto);
  }

  error(texto: string): void {
    this.mostrar('error', texto, 8000);
  }

  info(texto: string): void {
    this.mostrar('info', texto);
  }

  cerrar(id: number): void {
    this._avisos.update((lista) => lista.filter((a) => a.id !== id));
  }

  private mostrar(tipo: Aviso['tipo'], texto: string, duracion = 4500): void {
    const id = this.siguiente++;
    this._avisos.update((lista) => [...lista, { id, tipo, texto }]);
    setTimeout(() => this.cerrar(id), duracion);
  }
}

@Component({
  selector: 'bp-avisos',
  imports: [Icono],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="avisos" role="status" aria-live="polite">
      @for (a of servicio.avisos(); track a.id) {
        <div class="aviso-flotante" [class]="a.tipo">
          <bp-icono [nombre]="a.tipo === 'exito' ? 'check' : a.tipo === 'error' ? 'alerta' : 'info'" />
          <span>{{ a.texto }}</span>
          <button type="button" (click)="servicio.cerrar(a.id)" aria-label="Cerrar aviso"><bp-icono nombre="cerrar" [tamano]="14" /></button>
        </div>
      }
    </div>
  `,
  styles: `
    .avisos {
      position: fixed;
      right: 24px;
      bottom: 24px;
      display: flex;
      flex-direction: column;
      gap: 10px;
      z-index: 50;
      max-width: 420px;
    }
    .aviso-flotante {
      display: flex;
      align-items: center;
      gap: 10px;
      padding: 12px 14px;
      border-radius: 12px;
      background: var(--superficie-2);
      border: 1px solid var(--borde-fuerte);
      box-shadow: var(--sombra);
      &.exito { border-color: rgba(70, 217, 140, 0.5); bp-icono { color: var(--verde); } }
      &.error { border-color: var(--rojo-borde); bp-icono { color: var(--rojo); } }
      &.info bp-icono { color: var(--azul); }
      span { flex: 1; }
      button {
        background: none;
        border: 0;
        color: var(--texto-suave);
        cursor: pointer;
        padding: 2px;
      }
    }
  `,
})
export class Avisos {
  protected readonly servicio = inject(AvisosService);
}
