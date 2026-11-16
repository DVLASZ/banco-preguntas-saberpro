import { ChangeDetectionStrategy, Component, DestroyRef, OnInit, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { interval } from 'rxjs';
import { mensajeDeError } from '../../core/api/http-util';
import { SimulationsService } from '../../core/api/simulations.service';
import { ESTADOS_SIMULACRO, porcentaje, reloj, textoCompetencia, textoDificultad } from '../../core/etiquetas';
import { ItemSimulacro, Simulacro as DatosSimulacro } from '../../core/models';
import { Icono } from '../../ui/icono';
import { Insignia } from '../../ui/insignia';

const LETRAS = ['A', 'B', 'C', 'D'] as const;

/**
 * Presentar un simulacro (RF-23 y RF-24): una pregunta a la vez, con el tiempo que queda a la vista. Cuando termina
 * (porque el estudiante lo finaliza o porque el tiempo se agota) se muestra el resultado con las respuestas correctas.
 */
@Component({
  selector: 'bp-simulacro',
  imports: [RouterLink, Icono, Insignia],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './simulacro.html',
  styleUrl: './simulacro.scss',
})
export class Simulacro implements OnInit {
  private readonly ruta = inject(ActivatedRoute);
  private readonly simulaciones = inject(SimulationsService);
  private readonly destroyRef = inject(DestroyRef);

  protected readonly letras = LETRAS;
  protected readonly estados = ESTADOS_SIMULACRO;
  protected readonly reloj = reloj;
  protected readonly porcentaje = porcentaje;
  protected readonly textoCompetencia = textoCompetencia;
  protected readonly textoDificultad = textoDificultad;

  protected readonly simulacro = signal<DatosSimulacro | null>(null);
  protected readonly actual = signal(0);
  protected readonly restantes = signal(0);
  protected readonly cargando = signal(true);
  protected readonly guardando = signal(false);
  protected readonly confirmando = signal(false);
  protected readonly error = signal<string | null>(null);

  protected readonly enCurso = computed(() => this.simulacro()?.estado === 'EN_CURSO');
  protected readonly pregunta = computed<ItemSimulacro | null>(() => this.simulacro()?.preguntas[this.actual()] ?? null);
  protected readonly respondidas = computed(() => this.simulacro()?.preguntas.filter((p) => p.respuesta).length ?? 0);
  protected readonly sinResponder = computed(() => (this.simulacro()?.totalPreguntas ?? 0) - this.respondidas());
  protected readonly urgente = computed(() => this.enCurso() && this.restantes() <= 60);

  private id = '';

  ngOnInit(): void {
    this.id = this.ruta.snapshot.paramMap.get('id') ?? '';
    this.cargar();
    interval(1000)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(() => this.avanzarElTiempo());
  }

  protected cargar(): void {
    this.cargando.set(true);
    this.error.set(null);
    this.simulaciones.obtener(this.id).subscribe({
      next: (s) => {
        this.recibir(s);
        this.cargando.set(false);
      },
      error: (e) => {
        this.error.set(mensajeDeError(e));
        this.cargando.set(false);
      },
    });
  }

  protected ir(indice: number): void {
    this.actual.set(Math.max(0, Math.min(indice, (this.simulacro()?.preguntas.length ?? 1) - 1)));
  }

  protected textoDe(item: ItemSimulacro, letra: string): string {
    return { A: item.opcionA, B: item.opcionB, C: item.opcionC, D: item.opcionD }[letra] ?? '';
  }

  protected elegir(letra: string): void {
    const item = this.pregunta();
    if (!item || !this.enCurso() || this.guardando()) {
      return;
    }
    this.guardando.set(true);
    this.error.set(null);
    this.simulaciones.responder(this.id, item.preguntaId, letra).subscribe({
      next: (s) => {
        this.recibir(s);
        this.guardando.set(false);
      },
      error: (e) => {
        this.error.set(mensajeDeError(e));
        this.guardando.set(false);
        this.cargar(); // si el tiempo se agotó, el servidor ya lo cerró y calificó
      },
    });
  }

  protected finalizar(): void {
    this.guardando.set(true);
    this.simulaciones.finalizar(this.id).subscribe({
      next: (s) => {
        this.recibir(s);
        this.confirmando.set(false);
        this.guardando.set(false);
      },
      error: (e) => {
        this.error.set(mensajeDeError(e));
        this.confirmando.set(false);
        this.guardando.set(false);
      },
    });
  }

  private recibir(s: DatosSimulacro): void {
    this.simulacro.set(s);
    this.restantes.set(s.segundosRestantes);
  }

  /** Cuenta atrás en pantalla; al llegar a cero se vuelve a consultar, porque el servidor cierra y califica solo. */
  private avanzarElTiempo(): void {
    if (!this.enCurso()) {
      return;
    }
    if (this.restantes() <= 1) {
      this.restantes.set(0);
      this.cargar();
      return;
    }
    this.restantes.update((r) => r - 1);
  }
}
