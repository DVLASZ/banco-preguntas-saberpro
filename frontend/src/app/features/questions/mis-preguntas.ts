import { ChangeDetectionStrategy, Component, DestroyRef, OnInit, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { Router, RouterLink } from '@angular/router';
import { Subject, catchError, debounceTime, forkJoin, map, of } from 'rxjs';
import { mensajeDeError } from '../../core/api/http-util';
import { QuestionsService } from '../../core/api/questions.service';
import {
  OPCIONES_COMPETENCIA,
  OPCIONES_ESTADO,
  codigoCorto,
  fecha,
  textoCompetencia,
  textoDificultad,
} from '../../core/etiquetas';
import { Competencia, EstadoPregunta, Pagina, Pregunta } from '../../core/models';
import { EstadoPreguntaInsignia } from '../../ui/insignia';
import { Icono } from '../../ui/icono';
import { Paginacion } from '../../ui/paginacion';

interface Resumen {
  creadas: number;
  borradores: number;
  enRevision: number;
  aprobadas: number;
}

/** Mis preguntas (V2-02, HU-03): resumen, filtros y lista paginada de las preguntas del autor. */
@Component({
  selector: 'bp-mis-preguntas',
  imports: [RouterLink, Icono, EstadoPreguntaInsignia, Paginacion],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './mis-preguntas.html',
  styleUrl: './mis-preguntas.scss',
})
export class MisPreguntas implements OnInit {
  private readonly preguntas = inject(QuestionsService);
  private readonly router = inject(Router);
  private readonly destroyRef = inject(DestroyRef);

  protected readonly tamano = 10;
  protected readonly estados = OPCIONES_ESTADO;
  protected readonly competencias = OPCIONES_COMPETENCIA;

  protected readonly texto = signal('');
  protected readonly estado = signal<EstadoPregunta | ''>('');
  protected readonly competencia = signal<Competencia | ''>('');
  protected readonly pagina = signal(0);

  protected readonly resultado = signal<Pagina<Pregunta> | null>(null);
  protected readonly resumen = signal<Resumen | null>(null);
  protected readonly cargando = signal(true);
  protected readonly error = signal<string | null>(null);

  private readonly busqueda = new Subject<void>();

  protected readonly codigoCorto = codigoCorto;
  protected readonly fecha = fecha;
  protected readonly textoCompetencia = textoCompetencia;
  protected readonly textoDificultad = textoDificultad;

  ngOnInit(): void {
    // El texto se envía cuando la persona deja de escribir; los demás filtros, de inmediato.
    this.busqueda.pipe(debounceTime(300), takeUntilDestroyed(this.destroyRef)).subscribe(() => this.cargar());
    this.cargar();
    this.cargarResumen();
  }

  protected escribir(texto: string): void {
    this.texto.set(texto);
    this.pagina.set(0);
    this.busqueda.next();
  }

  protected filtrarEstado(valor: string): void {
    this.estado.set(valor as EstadoPregunta | '');
    this.pagina.set(0);
    this.cargar();
  }

  protected filtrarCompetencia(valor: string): void {
    this.competencia.set(valor as Competencia | '');
    this.pagina.set(0);
    this.cargar();
  }

  protected irA(pagina: number): void {
    this.pagina.set(pagina);
    this.cargar();
  }

  protected abrir(pregunta: Pregunta): void {
    void this.router.navigate(['/preguntas', pregunta.id]);
  }

  protected cargar(): void {
    this.cargando.set(true);
    this.error.set(null);
    this.preguntas
      .listarMias({
        texto: this.texto().trim(),
        estado: this.estado(),
        competencia: this.competencia(),
        page: this.pagina(),
        size: this.tamano,
      })
      .subscribe({
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
    const total = (estado?: EstadoPregunta) =>
      this.preguntas.listarMias({ estado, size: 1 }).pipe(
        map((p) => p.totalElementos),
        catchError(() => of(0)),
      );
    forkJoin([
      total(),
      total('BORRADOR'),
      total('PENDIENTE_REVISION'),
      total('EN_REVISION'),
      total('APROBADA'),
      total('PUBLICADA'),
    ]).subscribe(([creadas, borradores, pendientes, enRevision, aprobadas, publicadas]) =>
      this.resumen.set({ creadas, borradores, enRevision: pendientes + enRevision, aprobadas: aprobadas + publicadas }),
    );
  }
}
