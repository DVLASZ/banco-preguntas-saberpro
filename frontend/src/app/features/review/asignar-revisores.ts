import { ChangeDetectionStrategy, Component, OnInit, computed, inject, signal } from '@angular/core';
import { mensajeDeError } from '../../core/api/http-util';
import { ReviewsService } from '../../core/api/reviews.service';
import { codigoCorto, fechaCorta, textoCompetencia } from '../../core/etiquetas';
import { CasoPendiente } from '../../core/models';
import { RefrescoService } from '../../core/refresco';
import { AvisosService } from '../../ui/avisos';
import { Icono } from '../../ui/icono';
import { Insignia } from '../../ui/insignia';
import { SelectorRevisores } from './selector-revisores';

/** Asignar revisores (V2-04, HU-04): la cola de preguntas pendientes y la selección de revisores de una. */
@Component({
  selector: 'bp-asignar-revisores',
  imports: [Icono, Insignia, SelectorRevisores],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './asignar-revisores.html',
  styleUrl: './asignar-revisores.scss',
})
export class AsignarRevisores implements OnInit {
  private readonly reviews = inject(ReviewsService);
  private readonly avisos = inject(AvisosService);
  private readonly refresco = inject(RefrescoService);

  protected readonly pendientes = signal<CasoPendiente[]>([]);
  protected readonly total = signal(0);
  protected readonly maximo = signal(5);
  protected readonly seleccionada = signal<CasoPendiente | null>(null);
  protected readonly buscar = signal('');
  protected readonly cargando = signal(true);
  protected readonly error = signal<string | null>(null);

  protected readonly codigoCorto = codigoCorto;
  protected readonly fechaCorta = fechaCorta;
  protected readonly textoCompetencia = textoCompetencia;

  protected readonly visibles = computed(() => {
    const texto = this.buscar().trim().toLowerCase();
    return this.pendientes().filter(
      (p) => !texto || `${p.titulo} ${p.autorId} ${codigoCorto(p.preguntaId)} ${p.preguntaId}`.toLowerCase().includes(texto),
    );
  });

  ngOnInit(): void {
    this.reviews.configuracion().subscribe({ next: (c) => this.maximo.set(c.maximoDeRevisores), error: () => undefined });
    this.cargar();
  }

  protected cargar(): void {
    this.cargando.set(true);
    this.error.set(null);
    this.reviews.pendientesDeAsignacion(0, 50).subscribe({
      next: (r) => {
        this.pendientes.set(r.contenido);
        this.total.set(r.totalElementos);
        const actual = this.seleccionada();
        if (!actual || !r.contenido.some((p) => p.preguntaId === actual.preguntaId)) {
          this.seleccionada.set(r.contenido[0] ?? null);
        }
        this.cargando.set(false);
      },
      error: (e) => {
        this.error.set(mensajeDeError(e));
        this.cargando.set(false);
      },
    });
  }

  protected elegir(caso: CasoPendiente): void {
    this.seleccionada.set(caso);
  }

  protected alAsignar(caso: CasoPendiente): void {
    this.avisos.exito(`Revisores asignados a «${caso.titulo}».`);
    this.seleccionada.set(null);
    this.cargar();
    this.refresco.avisarCambioDeContadores();
  }
}
