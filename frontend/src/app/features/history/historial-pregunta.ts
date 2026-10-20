import { ChangeDetectionStrategy, Component, OnInit, inject, input, signal } from '@angular/core';
import { Location } from '@angular/common';
import { HistoryService } from '../../core/api/history.service';
import { mensajeDeError } from '../../core/api/http-util';
import { ENTRADAS, codigoCorto, fechaHora } from '../../core/etiquetas';
import { EntradaHistorial } from '../../core/models';
import { Icono } from '../../ui/icono';

/** El recorrido completo de una pregunta, en orden cronológico, tal como lo puede ver el usuario. */
@Component({
  selector: 'bp-historial-pregunta',
  imports: [Icono],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './historial-pregunta.html',
  styleUrl: './historial-pregunta.scss',
})
export class HistorialPregunta implements OnInit {
  private readonly servicio = inject(HistoryService);
  private readonly ubicacion = inject(Location);

  readonly id = input.required<string>();

  protected readonly entradas = ENTRADAS;
  protected readonly lista = signal<EntradaHistorial[]>([]);
  protected readonly cargando = signal(true);
  protected readonly error = signal<string | null>(null);
  protected readonly codigo = (id: string) => codigoCorto(id);
  protected readonly fechaHora = fechaHora;

  ngOnInit(): void {
    this.servicio.deUnaPregunta(this.id()).subscribe({
      next: (l) => {
        this.lista.set(l);
        this.cargando.set(false);
      },
      error: (e) => {
        this.error.set(mensajeDeError(e));
        this.cargando.set(false);
      },
    });
  }

  protected volver(): void {
    this.ubicacion.back();
  }
}
