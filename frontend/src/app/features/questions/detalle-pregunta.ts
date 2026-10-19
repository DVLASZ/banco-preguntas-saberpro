import { ChangeDetectionStrategy, Component, OnInit, computed, inject, input, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { catchError, of } from 'rxjs';
import { mensajeDeError } from '../../core/api/http-util';
import { QuestionsService } from '../../core/api/questions.service';
import { ReviewsService } from '../../core/api/reviews.service';
import { AuthService } from '../../core/auth/auth.service';
import { codigoCorto, fecha, fechaHora, textoCompetencia, textoDificultad, textoEstado, ESTADOS } from '../../core/etiquetas';
import { Pregunta, Revision, RondaRevision } from '../../core/models';
import { Avatar } from '../../ui/avatar';
import { AvisosService } from '../../ui/avisos';
import { Icono } from '../../ui/icono';
import { EstadoPreguntaInsignia, EstadoRondaInsignia } from '../../ui/insignia';

/**
 * Detalle de una pregunta (V2-07, HU-05): su contenido, el historial de estados y las observaciones de los
 * revisores. El autor puede corregirla si fue rechazada; el administrador puede publicarla si fue aprobada.
 */
@Component({
  selector: 'bp-detalle-pregunta',
  imports: [RouterLink, Icono, Avatar, EstadoPreguntaInsignia, EstadoRondaInsignia],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './detalle-pregunta.html',
  styleUrl: './detalle-pregunta.scss',
})
export class DetallePregunta implements OnInit {
  private readonly preguntas = inject(QuestionsService);
  private readonly reviews = inject(ReviewsService);
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  private readonly avisos = inject(AvisosService);

  readonly id = input.required<string>();

  protected readonly pregunta = signal<Pregunta | null>(null);
  protected readonly revision = signal<Revision | null>(null);
  protected readonly error = signal<string | null>(null);
  protected readonly cargando = signal(true);
  protected readonly trabajando = signal(false);
  protected readonly completo = signal(false);

  protected readonly fecha = fecha;
  protected readonly fechaHora = fechaHora;
  protected readonly codigoCorto = codigoCorto;
  protected readonly textoEstado = textoEstado;
  protected readonly textoCompetencia = textoCompetencia;
  protected readonly textoDificultad = textoDificultad;
  protected readonly opciones = ['A', 'B', 'C', 'D'] as const;

  protected readonly esAutor = computed(() => this.auth.tieneAlgunRol('AUTHOR') && this.pregunta()?.autorId === this.auth.usuario()?.id);
  protected readonly esAdmin = computed(() => this.auth.tieneAlgunRol('ADMIN'));
  protected readonly puedeCorregir = computed(() => this.esAutor() && this.pregunta()?.estado === 'RECHAZADA');
  protected readonly puedeEditar = computed(() => this.esAutor() && this.pregunta()?.estado === 'BORRADOR');
  protected readonly puedePublicar = computed(() => this.esAdmin() && this.pregunta()?.estado === 'APROBADA');
  protected readonly volverA = computed(() => (this.esAutor() ? '/mis-preguntas' : '/publicacion'));
  protected readonly volverTexto = computed(() => (this.esAutor() ? 'Volver a mis preguntas' : 'Volver a la publicación'));

  /** Las rondas, de la más reciente a la más antigua. */
  protected readonly rondas = computed<RondaRevision[]>(() => [...(this.revision()?.rondas ?? [])].reverse());

  protected readonly respuesta = computed(() => {
    const p = this.pregunta();
    return p ? this.textoOpcion(p, p.respuestaCorrecta) : '';
  });

  protected readonly historial = computed(() =>
    (this.pregunta()?.historialEstados ?? []).map((c) => ({
      texto: textoEstado(c.hacia),
      tono: ESTADOS[c.hacia]?.tono ?? 'gris',
      cuando: fechaHora(c.fecha),
      quien: c.cambiadoPor ? c.cambiadoPor : 'Sistema',
    })),
  );

  ngOnInit(): void {
    this.preguntas.obtener(this.id()).subscribe({
      next: (p) => {
        this.pregunta.set(p);
        this.cargando.set(false);
        if (p.estado !== 'BORRADOR') {
          this.reviews
            .revision(p.id)
            .pipe(catchError(() => of(null)))
            .subscribe((r) => this.revision.set(r));
        }
      },
      error: (e) => {
        this.error.set(mensajeDeError(e));
        this.cargando.set(false);
      },
    });
  }

  protected textoOpcion(p: Pregunta, letra: string): string {
    const l = letra.toUpperCase();
    const texto = l === 'A' ? p.opcionA : l === 'B' ? p.opcionB : l === 'C' ? p.opcionC : p.opcionD;
    return `${l} · ${texto}`;
  }

  protected opcion(p: Pregunta, letra: string): string {
    return letra === 'A' ? p.opcionA : letra === 'B' ? p.opcionB : letra === 'C' ? p.opcionC : p.opcionD;
  }

  protected corregir(): void {
    const p = this.pregunta();
    if (!p) {
      return;
    }
    this.trabajando.set(true);
    this.preguntas.reabrir(p.id).subscribe({
      next: (r) => {
        this.avisos.exito(`La pregunta volvió a borrador como versión ${r.version}. Corrígela y envíala de nuevo.`);
        void this.router.navigate(['/preguntas', r.id, 'editar']);
      },
      error: (e) => {
        this.trabajando.set(false);
        this.avisos.error(mensajeDeError(e));
      },
    });
  }

  protected publicar(): void {
    const p = this.pregunta();
    if (!p) {
      return;
    }
    this.trabajando.set(true);
    this.preguntas.publicar(p.id).subscribe({
      next: (r) => {
        this.pregunta.set({ ...p, estado: r.estado, historialEstados: p.historialEstados });
        this.trabajando.set(false);
        this.avisos.exito('Pregunta publicada.');
        this.ngOnInit();
      },
      error: (e) => {
        this.trabajando.set(false);
        this.avisos.error(mensajeDeError(e));
      },
    });
  }

  protected decisiones(ronda: RondaRevision): number {
    return ronda.revisores.filter((r) => r.evaluacion).length;
  }
}
