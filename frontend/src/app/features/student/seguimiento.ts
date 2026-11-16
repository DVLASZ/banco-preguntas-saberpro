import { ChangeDetectionStrategy, Component, OnInit, inject, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { Observable, catchError, forkJoin, of } from 'rxjs';
import { mensajeDeError } from '../../core/api/http-util';
import { StatisticsService } from '../../core/api/statistics.service';
import { TENDENCIAS, fechaHora, porcentaje, reloj, textoCompetencia } from '../../core/etiquetas';
import { AciertosCompetencia, EntradaSeguimiento, Estadisticas, Fortalezas, Pagina } from '../../core/models';
import { Icono } from '../../ui/icono';
import { Insignia } from '../../ui/insignia';
import { Paginacion } from '../../ui/paginacion';

/**
 * Seguimiento académico (RF-25 a RF-27): puntaje promedio y tendencia, fortalezas y debilidades por competencia e
 * historial de simulacros. Con la ruta del estudiante muestra lo propio; con {@code estudiantes/:id}, lo de ese
 * estudiante para docentes y administradores.
 */
@Component({
  selector: 'bp-seguimiento',
  imports: [RouterLink, Icono, Insignia, Paginacion],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './seguimiento.html',
  styleUrl: './seguimiento.scss',
})
export class Seguimiento implements OnInit {
  private readonly ruta = inject(ActivatedRoute);
  private readonly estadisticas = inject(StatisticsService);

  protected readonly tendencias = TENDENCIAS;
  protected readonly fechaHora = fechaHora;
  protected readonly porcentaje = porcentaje;
  protected readonly reloj = reloj;
  protected readonly textoCompetencia = textoCompetencia;

  /** Si se consulta a otro estudiante (vista de docente); {@code null} cuando es lo propio. */
  protected readonly estudianteId = signal<string | null>(null);
  protected readonly resumen = signal<Estadisticas | null>(null);
  protected readonly competencias = signal<Fortalezas | null>(null);
  protected readonly historial = signal<Pagina<EntradaSeguimiento> | null>(null);
  protected readonly pagina = signal(0);
  protected readonly cargando = signal(true);
  protected readonly error = signal<string | null>(null);

  ngOnInit(): void {
    this.estudianteId.set(this.ruta.snapshot.paramMap.get('id'));
    this.cargar();
  }

  protected irA(pagina: number): void {
    this.pagina.set(pagina);
    this.cargarHistorial();
  }

  protected sinDatos(): boolean {
    return (this.resumen()?.simulacros ?? 0) === 0;
  }

  protected cargar(): void {
    const id = this.estudianteId();
    this.cargando.set(true);
    this.error.set(null);
    forkJoin({
      resumen: id ? this.estadisticas.estadisticasDe(id) : this.estadisticas.misEstadisticas(),
      competencias: (id ? this.estadisticas.competenciasDe(id) : this.estadisticas.misCompetencias()).pipe(
        catchError(() => of(null)),
      ),
      historial: this.historialActual(),
    }).subscribe({
      next: (r) => {
        this.resumen.set(r.resumen);
        this.competencias.set(r.competencias);
        this.historial.set(r.historial);
        this.cargando.set(false);
      },
      error: (e) => {
        this.error.set(mensajeDeError(e));
        this.cargando.set(false);
      },
    });
  }

  protected barra(c: AciertosCompetencia): number {
    return Math.max(0, Math.min(100, c.porcentaje));
  }

  private cargarHistorial(): void {
    this.historialActual().subscribe({
      next: (h) => this.historial.set(h),
      error: (e) => this.error.set(mensajeDeError(e)),
    });
  }

  private historialActual(): Observable<Pagina<EntradaSeguimiento>> {
    const id = this.estudianteId();
    return id ? this.estadisticas.historialDe(id, this.pagina(), 8) : this.estadisticas.miHistorial(this.pagina(), 8);
  }
}
