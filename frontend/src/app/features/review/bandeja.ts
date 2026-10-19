import { ChangeDetectionStrategy, Component, OnInit, inject, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { catchError, forkJoin, map, of } from 'rxjs';
import { mensajeDeError } from '../../core/api/http-util';
import { ReviewsService } from '../../core/api/reviews.service';
import { codigoCorto, fechaCorta, textoCompetencia } from '../../core/etiquetas';
import { AsignacionRevisor, Pagina } from '../../core/models';
import { Icono } from '../../ui/icono';
import { Insignia } from '../../ui/insignia';
import { Paginacion } from '../../ui/paginacion';

type Pestana = 'PENDIENTES' | 'EVALUADAS';

/** Bandeja del revisor (V2-05, HU-05): las preguntas asignadas por evaluar y las que ya evaluó. */
@Component({
  selector: 'bp-bandeja',
  imports: [RouterLink, Icono, Insignia, Paginacion],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './bandeja.html',
  styleUrl: './bandeja.scss',
})
export class Bandeja implements OnInit {
  private readonly reviews = inject(ReviewsService);
  private readonly router = inject(Router);

  protected readonly tamano = 10;
  protected readonly pestana = signal<Pestana>('PENDIENTES');
  protected readonly pagina = signal(0);
  protected readonly resultado = signal<Pagina<AsignacionRevisor> | null>(null);
  protected readonly cargando = signal(true);
  protected readonly error = signal<string | null>(null);
  protected readonly resumen = signal<{ pendientes: number; evaluadas: number; conRondaRechazada: number } | null>(null);

  protected readonly codigoCorto = codigoCorto;
  protected readonly fechaCorta = fechaCorta;
  protected readonly textoCompetencia = textoCompetencia;

  ngOnInit(): void {
    this.cargar();
    this.cargarResumen();
  }

  protected cambiarPestana(pestana: Pestana): void {
    if (pestana !== this.pestana()) {
      this.pestana.set(pestana);
      this.pagina.set(0);
      this.cargar();
    }
  }

  protected irA(pagina: number): void {
    this.pagina.set(pagina);
    this.cargar();
  }

  protected abrir(a: AsignacionRevisor): void {
    void this.router.navigate(['/evaluar', a.preguntaId]);
  }

  protected cargar(): void {
    this.cargando.set(true);
    this.error.set(null);
    this.reviews.bandeja(this.pestana(), this.pagina(), this.tamano).subscribe({
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

  private cargarResumen(): void {
    forkJoin({
      pendientes: this.reviews.bandeja('PENDIENTES', 0, 100).pipe(catchError(() => of(null))),
      evaluadas: this.reviews.bandeja('EVALUADAS', 0, 1).pipe(
        map((p) => p.totalElementos),
        catchError(() => of(0)),
      ),
    }).subscribe(({ pendientes, evaluadas }) =>
      this.resumen.set({
        pendientes: pendientes?.totalElementos ?? 0,
        evaluadas,
        conRondaRechazada: pendientes?.contenido.filter((a) => a.estadoRonda === 'RECHAZADA').length ?? 0,
      }),
    );
  }
}
