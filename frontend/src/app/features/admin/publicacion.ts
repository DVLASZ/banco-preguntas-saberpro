import { ChangeDetectionStrategy, Component, DestroyRef, OnInit, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { RouterLink } from '@angular/router';
import { Subject, debounceTime } from 'rxjs';
import { mensajeDeError } from '../../core/api/http-util';
import { QuestionsService } from '../../core/api/questions.service';
import { OPCIONES_COMPETENCIA, codigoCorto, fecha, textoCompetencia } from '../../core/etiquetas';
import { Competencia, EstadoPregunta, Pagina, Pregunta } from '../../core/models';
import { AvisosService } from '../../ui/avisos';
import { Icono } from '../../ui/icono';
import { EstadoPreguntaInsignia } from '../../ui/insignia';
import { Paginacion } from '../../ui/paginacion';

type Vista = 'APROBADA' | 'PUBLICADA' | 'TODAS';

/** Publicar preguntas: el administrador publica manualmente las que los revisores aprobaron. */
@Component({
  selector: 'bp-publicacion',
  imports: [RouterLink, Icono, EstadoPreguntaInsignia, Paginacion],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './publicacion.html',
  styleUrl: './publicacion.scss',
})
export class Publicacion implements OnInit {
  private readonly preguntas = inject(QuestionsService);
  private readonly avisos = inject(AvisosService);
  private readonly destroyRef = inject(DestroyRef);

  protected readonly tamano = 10;
  protected readonly competencias = OPCIONES_COMPETENCIA;
  protected readonly vista = signal<Vista>('APROBADA');
  protected readonly texto = signal('');
  protected readonly competencia = signal<Competencia | ''>('');
  protected readonly pagina = signal(0);
  protected readonly resultado = signal<Pagina<Pregunta> | null>(null);
  protected readonly cargando = signal(true);
  protected readonly error = signal<string | null>(null);
  protected readonly publicando = signal<string | null>(null);
  protected readonly porPublicar = signal<number | null>(null);

  protected readonly codigoCorto = codigoCorto;
  protected readonly fecha = fecha;
  protected readonly textoCompetencia = textoCompetencia;

  private readonly busqueda = new Subject<void>();

  ngOnInit(): void {
    this.busqueda.pipe(debounceTime(300), takeUntilDestroyed(this.destroyRef)).subscribe(() => this.cargar());
    this.cargar();
    this.contarPorPublicar();
  }

  protected cambiarVista(vista: Vista): void {
    if (vista !== this.vista()) {
      this.vista.set(vista);
      this.pagina.set(0);
      this.cargar();
    }
  }

  protected escribir(texto: string): void {
    this.texto.set(texto);
    this.pagina.set(0);
    this.busqueda.next();
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

  protected cargar(): void {
    this.cargando.set(true);
    this.error.set(null);
    const vista = this.vista();
    const estado: EstadoPregunta | '' = vista === 'TODAS' ? '' : vista;
    this.preguntas
      .listarTodas({ estado, competencia: this.competencia(), texto: this.texto().trim(), page: this.pagina(), size: this.tamano })
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

  protected publicar(p: Pregunta): void {
    this.publicando.set(p.id);
    this.preguntas.publicar(p.id).subscribe({
      next: () => {
        this.publicando.set(null);
        this.avisos.exito(`«${p.nombre}» fue publicada.`);
        this.cargar();
        this.contarPorPublicar();
      },
      error: (e) => {
        this.publicando.set(null);
        this.avisos.error(mensajeDeError(e));
      },
    });
  }

  private contarPorPublicar(): void {
    this.preguntas.listarTodas({ estado: 'APROBADA', size: 1 }).subscribe({
      next: (r) => this.porPublicar.set(r.totalElementos),
      error: () => undefined,
    });
  }
}
