import { ChangeDetectionStrategy, Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { catchError, of } from 'rxjs';
import { mensajeDeError } from '../../core/api/http-util';
import { SimulationsService } from '../../core/api/simulations.service';
import { ESTADOS_SIMULACRO, OPCIONES_COMPETENCIA, OPCIONES_DIFICULTAD, fechaHora, textoCompetencia } from '../../core/etiquetas';
import { Competencia, Dificultad, Pagina, ResumenCatalogo, ResumenSimulacro } from '../../core/models';
import { Icono } from '../../ui/icono';
import { Insignia } from '../../ui/insignia';
import { Paginacion } from '../../ui/paginacion';

/** Simulacros del estudiante (RF-21 a RF-23): arma uno nuevo con filtros, lo continúa o revisa los anteriores. */
@Component({
  selector: 'bp-simulacros',
  imports: [FormsModule, RouterLink, Icono, Insignia, Paginacion],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './simulacros.html',
  styleUrl: './simulacros.scss',
})
export class Simulacros implements OnInit {
  private readonly simulaciones = inject(SimulationsService);
  private readonly router = inject(Router);

  protected readonly competencias = OPCIONES_COMPETENCIA;
  protected readonly dificultades = OPCIONES_DIFICULTAD;
  protected readonly estados = ESTADOS_SIMULACRO;
  protected readonly fechaHora = fechaHora;
  protected readonly textoCompetencia = textoCompetencia;

  protected readonly catalogo = signal<ResumenCatalogo | null>(null);
  protected readonly resultado = signal<Pagina<ResumenSimulacro> | null>(null);
  protected readonly pagina = signal(0);
  protected readonly cargando = signal(true);
  protected readonly error = signal<string | null>(null);

  protected readonly seleccionadas = signal<Competencia[]>([]);
  protected readonly niveles = signal<Dificultad[]>([]);
  protected cantidad = 10;
  protected minutos = 30;
  protected readonly generando = signal(false);
  protected readonly errorAlGenerar = signal<string | null>(null);

  /** Cuántas preguntas hay con los filtros elegidos (según el resumen del catálogo). */
  protected readonly disponibles = computed(() => {
    const c = this.catalogo();
    if (!c) {
      return null;
    }
    const porCompetencia = this.seleccionadas().length
      ? this.seleccionadas().reduce((suma, k) => suma + (c.porCompetencia[k] ?? 0), 0)
      : c.total;
    if (!this.niveles().length) {
      return porCompetencia;
    }
    // el resumen no cruza competencia y dificultad: con ambos filtros es una cota superior
    const porNivel = this.niveles().reduce((suma, k) => suma + (c.porDificultad[k] ?? 0), 0);
    return Math.min(porCompetencia, porNivel);
  });

  ngOnInit(): void {
    this.simulaciones.catalogo().pipe(catchError(() => of(null))).subscribe((c) => this.catalogo.set(c));
    this.cargar();
  }

  protected alternarCompetencia(valor: Competencia): void {
    this.seleccionadas.update((l) => (l.includes(valor) ? l.filter((x) => x !== valor) : [...l, valor]));
  }

  protected alternarNivel(valor: Dificultad): void {
    this.niveles.update((l) => (l.includes(valor) ? l.filter((x) => x !== valor) : [...l, valor]));
  }

  protected irA(pagina: number): void {
    this.pagina.set(pagina);
    this.cargar();
  }

  protected generar(): void {
    this.generando.set(true);
    this.errorAlGenerar.set(null);
    this.simulaciones
      .crear({
        competencias: this.seleccionadas(),
        dificultades: this.niveles(),
        cantidad: this.cantidad,
        duracionMinutos: this.minutos,
      })
      .subscribe({
        next: (s) => void this.router.navigate(['/simulacros', s.id]),
        error: (e) => {
          this.errorAlGenerar.set(mensajeDeError(e));
          this.generando.set(false);
        },
      });
  }

  protected cargar(): void {
    this.cargando.set(true);
    this.error.set(null);
    this.simulaciones.mios(this.pagina(), 8).subscribe({
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
