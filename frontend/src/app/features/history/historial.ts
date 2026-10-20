import { ChangeDetectionStrategy, Component, OnInit, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { HistoryService } from '../../core/api/history.service';
import { mensajeDeError } from '../../core/api/http-util';
import { ENTRADAS, codigoCorto, diaRelativo, hora } from '../../core/etiquetas';
import { EntradaHistorial, Pagina } from '../../core/models';
import { Icono } from '../../ui/icono';
import { Paginacion } from '../../ui/paginacion';

/** Historial: todo lo que el usuario puede ver de sus preguntas (cada rol ve la parte que le corresponde). */
@Component({
  selector: 'bp-historial',
  imports: [RouterLink, Icono, Paginacion],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './historial.html',
  styleUrl: './historial.scss',
})
export class Historial implements OnInit {
  private readonly servicio = inject(HistoryService);

  protected readonly tamano = 20;
  protected readonly pagina = signal(0);
  protected readonly resultado = signal<Pagina<EntradaHistorial> | null>(null);
  protected readonly cargando = signal(true);
  protected readonly error = signal<string | null>(null);

  protected readonly entradas = ENTRADAS;
  protected readonly hora = hora;
  protected readonly codigoCorto = codigoCorto;

  protected readonly grupos = computed(() => {
    const grupos: { dia: string; items: EntradaHistorial[] }[] = [];
    for (const e of this.resultado()?.contenido ?? []) {
      const dia = diaRelativo(e.ocurridaEn);
      const ultimo = grupos.at(-1);
      if (ultimo?.dia === dia) {
        ultimo.items.push(e);
      } else {
        grupos.push({ dia, items: [e] });
      }
    }
    return grupos;
  });

  ngOnInit(): void {
    this.cargar();
  }

  protected irA(pagina: number): void {
    this.pagina.set(pagina);
    this.cargar();
  }

  protected cargar(): void {
    this.cargando.set(true);
    this.error.set(null);
    this.servicio.mio(this.pagina(), this.tamano).subscribe({
      next: (r) => {
        this.resultado.set(r);
        this.cargando.set(false);
      },
      error: (e) => {
        this.error.set(mensajeDeError(e));
        this.cargando.set(false);
      },
    });
  }
}
