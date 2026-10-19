import { ChangeDetectionStrategy, Component, OnInit, computed, inject, input, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { forkJoin } from 'rxjs';
import { mensajeDeError } from '../../core/api/http-util';
import { QuestionsService } from '../../core/api/questions.service';
import { ReviewsService } from '../../core/api/reviews.service';
import { AuthService } from '../../core/auth/auth.service';
import { codigoCorto, fecha, fechaCorta, fechaHora, textoCompetencia, textoDificultad } from '../../core/etiquetas';
import { Decision, Pregunta, Revision, RevisorDeRonda, RondaRevision } from '../../core/models';
import { RefrescoService } from '../../core/refresco';
import { Avatar } from '../../ui/avatar';
import { AvisosService } from '../../ui/avisos';
import { Icono } from '../../ui/icono';
import { EstadoRondaInsignia } from '../../ui/insignia';

export const MAXIMO_OBSERVACION = 4000;

/**
 * Qué puede hacer el revisor en la ronda vigente: evaluar (la ronda está en curso), dejar una observación tardía
 * (ya fue rechazada pero sigue abierta), o nada (ya evaluó, o la ronda terminó).
 */
export function situacionDelRevisor(
  ronda: RondaRevision | undefined,
  yo: RevisorDeRonda | undefined,
): 'evaluar' | 'tardia' | 'evaluada' | 'cerrada' | 'ajena' {
  if (!ronda || !yo) {
    return 'ajena';
  }
  if (yo.evaluacion) {
    return 'evaluada';
  }
  if (ronda.estado === 'EN_CURSO') {
    return 'evaluar';
  }
  return ronda.estado === 'RECHAZADA' ? 'tardia' : 'cerrada';
}

/** Evaluar pregunta (V2-06, HU-05): el contenido del ítem, la decisión del revisor y lo que dijeron sus colegas. */
@Component({
  selector: 'bp-evaluar',
  imports: [RouterLink, Icono, Avatar, EstadoRondaInsignia],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './evaluar.html',
  styleUrl: './evaluar.scss',
})
export class Evaluar implements OnInit {
  private readonly preguntas = inject(QuestionsService);
  private readonly reviews = inject(ReviewsService);
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  private readonly avisos = inject(AvisosService);
  private readonly refresco = inject(RefrescoService);

  readonly id = input.required<string>();

  protected readonly pregunta = signal<Pregunta | null>(null);
  protected readonly revision = signal<Revision | null>(null);
  protected readonly error = signal<string | null>(null);
  protected readonly cargando = signal(true);

  protected readonly decision = signal<Decision | null>(null);
  protected readonly observacion = signal('');
  protected readonly confirmando = signal(false);
  protected readonly enviando = signal(false);
  protected readonly intento = signal(false);

  protected readonly maximo = MAXIMO_OBSERVACION;
  protected readonly letras = ['A', 'B', 'C', 'D'] as const;
  protected readonly codigoCorto = codigoCorto;
  protected readonly fecha = fecha;
  protected readonly fechaCorta = fechaCorta;
  protected readonly fechaHora = fechaHora;
  protected readonly textoCompetencia = textoCompetencia;
  protected readonly textoDificultad = textoDificultad;

  protected readonly ronda = computed(() => this.revision()?.rondas.at(-1));
  protected readonly yo = computed(() => this.ronda()?.revisores.find((r) => r.revisorId === this.auth.usuario()?.id));
  protected readonly situacion = computed(() => situacionDelRevisor(this.ronda(), this.yo()));
  protected readonly colegas = computed(() => (this.ronda()?.revisores ?? []).filter((r) => r.revisorId !== this.auth.usuario()?.id && r.evaluacion));
  protected readonly colegasTotal = computed(() => Math.max(0, (this.ronda()?.totalDeRevisores ?? 1) - 1));

  protected readonly errorDeObservacion = computed(() => {
    const texto = this.observacion().trim();
    if (this.decision() === 'RECHAZAR' && texto === '') {
      return 'Debes escribir una observación para rechazar la pregunta.';
    }
    if (texto.length > MAXIMO_OBSERVACION) {
      return `La observación no puede superar ${MAXIMO_OBSERVACION} caracteres.`;
    }
    return null;
  });
  protected readonly puedeEnviar = computed(() => this.decision() !== null && this.errorDeObservacion() === null);
  protected readonly pista = computed(() => {
    if (this.decision() === null) {
      return 'Elige una decisión.';
    }
    return this.decision() === 'RECHAZAR' ? 'Observaciones obligatorias' : 'Observaciones opcionales';
  });

  ngOnInit(): void {
    forkJoin({ pregunta: this.preguntas.obtener(this.id()), revision: this.reviews.revision(this.id()) }).subscribe({
      next: ({ pregunta, revision }) => {
        this.pregunta.set(pregunta);
        this.revision.set(revision);
        this.cargando.set(false);
      },
      error: (e) => {
        this.error.set(mensajeDeError(e));
        this.cargando.set(false);
      },
    });
  }

  protected opcion(p: Pregunta, letra: string): string {
    return letra === 'A' ? p.opcionA : letra === 'B' ? p.opcionB : letra === 'C' ? p.opcionC : p.opcionD;
  }

  protected elegir(decision: Decision): void {
    this.decision.set(decision);
    this.confirmando.set(false);
  }

  protected pedirConfirmacion(): void {
    this.intento.set(true);
    if (this.puedeEnviar()) {
      this.confirmando.set(true);
    }
  }

  protected enviar(): void {
    const decision = this.decision();
    if (!decision || !this.puedeEnviar() || this.enviando()) {
      return;
    }
    this.enviando.set(true);
    this.reviews.evaluar(this.id(), decision, this.observacion()).subscribe({
      next: (r) => {
        const texto = r.tardia
          ? 'Tu observación quedó registrada; la ronda ya había sido rechazada.'
          : r.resultadoDeLaRonda === 'APROBADA'
            ? 'Evaluación enviada. Con ella la pregunta quedó aprobada.'
            : r.resultadoDeLaRonda === 'RECHAZADA'
              ? 'Evaluación enviada. La pregunta fue rechazada y volvió al autor.'
              : 'Evaluación enviada. Esperando a los demás revisores.';
        this.avisos.exito(texto);
        this.refresco.avisarCambioDeContadores();
        void this.router.navigate(['/bandeja']);
      },
      error: (e) => {
        this.enviando.set(false);
        this.confirmando.set(false);
        this.avisos.error(mensajeDeError(e));
      },
    });
  }
}
