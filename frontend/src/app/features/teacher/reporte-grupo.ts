import { ChangeDetectionStrategy, Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { mensajeDeError } from '../../core/api/http-util';
import { ReportsService } from '../../core/api/reports.service';
import { TENDENCIAS, fechaHora, porcentaje, textoCompetencia } from '../../core/etiquetas';
import { ReporteGrupo as DatosReporte } from '../../core/models';
import { Icono } from '../../ui/icono';
import { Insignia } from '../../ui/insignia';

/** Reporte del grupo para docentes (RF-27): promedios, competencias del grupo y cada estudiante, con descarga en CSV. */
@Component({
  selector: 'bp-reporte-grupo',
  imports: [FormsModule, RouterLink, Icono, Insignia],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './reporte-grupo.html',
  styleUrl: './reporte-grupo.scss',
})
export class ReporteGrupo implements OnInit {
  private readonly reportes = inject(ReportsService);
  private readonly router = inject(Router);

  protected readonly tendencias = TENDENCIAS;
  protected readonly fechaHora = fechaHora;
  protected readonly porcentaje = porcentaje;
  protected readonly textoCompetencia = textoCompetencia;

  protected readonly reporte = signal<DatosReporte | null>(null);
  protected readonly cargando = signal(true);
  protected readonly descargando = signal(false);
  protected readonly error = signal<string | null>(null);
  protected buscado = '';

  ngOnInit(): void {
    this.cargar();
  }

  protected cargar(): void {
    this.cargando.set(true);
    this.error.set(null);
    this.reportes.grupo().subscribe({
      next: (r) => {
        this.reporte.set(r);
        this.cargando.set(false);
      },
      error: (e) => {
        this.error.set(mensajeDeError(e));
        this.cargando.set(false);
      },
    });
  }

  protected abrir(estudianteId: string): void {
    void this.router.navigate(['/estudiantes', estudianteId]);
  }

  protected buscar(): void {
    const id = this.buscado.trim();
    if (id) {
      this.abrir(id);
    }
  }

  protected descargarCsv(): void {
    this.descargando.set(true);
    this.reportes.grupoCsv().subscribe({
      next: (archivo) => {
        const enlace = document.createElement('a');
        const url = URL.createObjectURL(archivo);
        enlace.href = url;
        enlace.download = 'reporte-del-grupo.csv';
        enlace.click();
        URL.revokeObjectURL(url);
        this.descargando.set(false);
      },
      error: (e) => {
        this.error.set(mensajeDeError(e));
        this.descargando.set(false);
      },
    });
  }
}
