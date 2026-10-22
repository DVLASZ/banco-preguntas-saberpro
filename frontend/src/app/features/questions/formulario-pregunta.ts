import { ChangeDetectionStrategy, Component, OnInit, computed, inject, input, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { Observable } from 'rxjs';
import { cuerpoDeError, mensajeDeError } from '../../core/api/http-util';
import { QuestionsService } from '../../core/api/questions.service';
import { OPCIONES_COMPETENCIA, OPCIONES_DIFICULTAD, codigoCorto, fechaHora } from '../../core/etiquetas';
import { ContenidoPregunta, Pregunta } from '../../core/models';
import { AvisosService } from '../../ui/avisos';
import { Icono } from '../../ui/icono';
import { EstadoPreguntaInsignia } from '../../ui/insignia';
import { CONTENIDO_VACIO, LIMITES, validarContenido } from './validacion';

type Campo = keyof ContenidoPregunta;

/** Crear pregunta (V2-03, HU-01) y corregir un borrador: contenido del ítem, clasificación y respuesta. */
@Component({
  selector: 'bp-formulario-pregunta',
  imports: [RouterLink, Icono, EstadoPreguntaInsignia],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './formulario-pregunta.html',
  styleUrl: './formulario-pregunta.scss',
})
export class FormularioPregunta implements OnInit {
  private readonly preguntas = inject(QuestionsService);
  private readonly router = inject(Router);
  private readonly avisos = inject(AvisosService);

  /** Se enlaza al parámetro `:id` de la ruta (vacío al crear). */
  readonly id = input<string>();

  protected readonly competencias = OPCIONES_COMPETENCIA;
  protected readonly dificultades = OPCIONES_DIFICULTAD;
  protected readonly letras = ['A', 'B', 'C', 'D'] as const;
  protected readonly limites = LIMITES;

  protected readonly contenido = signal<ContenidoPregunta>({ ...CONTENIDO_VACIO });
  protected readonly guardada = signal<Pregunta | null>(null);
  protected readonly cargando = signal(false);
  protected readonly trabajando = signal<'guardar' | 'enviar' | null>(null);
  /** El envío a revisión pide confirmación (HU-02): una vez enviada, la pregunta ya no se edita. */
  protected readonly confirmandoEnvio = signal(false);
  protected readonly intento = signal(false);
  protected readonly tocados = signal<ReadonlySet<Campo>>(new Set());
  protected readonly erroresDelServidor = signal<string[]>([]);
  protected readonly errorDeCarga = signal<string | null>(null);

  protected readonly errores = computed(() => validarContenido(this.contenido()));
  protected readonly edicion = computed(() => this.id() !== undefined);
  protected readonly codigo = computed(() => (this.guardada() ? codigoCorto(this.guardada()!.id) : null));
  protected readonly fechaHora = fechaHora;

  ngOnInit(): void {
    const id = this.id();
    if (!id) {
      return;
    }
    this.cargando.set(true);
    this.preguntas.obtener(id).subscribe({
      next: (p) => {
        if (p.estado !== 'BORRADOR') {
          this.avisos.info('Solo se pueden editar los borradores. Para corregir una pregunta rechazada, ábrela y elige «Corregir pregunta».');
          void this.router.navigate(['/preguntas', p.id]);
          return;
        }
        this.guardada.set(p);
        this.contenido.set(this.aContenido(p));
        this.cargando.set(false);
      },
      error: (e) => {
        this.errorDeCarga.set(mensajeDeError(e));
        this.cargando.set(false);
      },
    });
  }

  protected valor(campo: Campo): string {
    return String(this.contenido()[campo] ?? '');
  }

  protected cambiar(campo: Campo, valor: string): void {
    this.contenido.update((c) => ({ ...c, [campo]: valor }));
    this.erroresDelServidor.set([]);
  }

  protected tocar(campo: Campo): void {
    this.tocados.update((t) => new Set(t).add(campo));
  }

  /** El mensaje de un campo, visible solo después de tocarlo o de intentar guardar. */
  protected error(campo: Campo): string | null {
    return this.intento() || this.tocados().has(campo) ? (this.errores()[campo] ?? null) : null;
  }

  protected textoOpcion(letra: string): string {
    return this.valor(('opcion' + letra) as Campo);
  }

  protected etiquetaOpcion(letra: string): string {
    return this.valor('respuestaCorrecta').toUpperCase() === letra ? `Opción ${letra} · Correcta` : `Opción ${letra}`;
  }

  protected guardarBorrador(): void {
    this.ejecutar('guardar', false);
  }

  protected enviarARevision(): void {
    this.intento.set(true);
    if (Object.keys(this.errores()).length > 0) {
      this.avisos.error('Revisa los campos marcados antes de continuar.');
      return;
    }
    this.confirmandoEnvio.set(true);
  }

  protected confirmarEnvio(): void {
    this.confirmandoEnvio.set(false);
    this.ejecutar('enviar', true);
  }

  private ejecutar(accion: 'guardar' | 'enviar', enviar: boolean): void {
    this.intento.set(true);
    this.erroresDelServidor.set([]);
    if (Object.keys(this.errores()).length > 0) {
      this.avisos.error('Revisa los campos marcados antes de continuar.');
      return;
    }
    this.trabajando.set(accion);
    const contenido = this.normalizado();
    const existente = this.guardada();
    const guardar$: Observable<Pregunta> = existente
      ? this.preguntas.actualizar(existente.id, contenido)
      : this.preguntas.crear(contenido);
    guardar$.subscribe({
      next: (p) => {
        this.guardada.set(p);
        if (!enviar) {
          this.trabajando.set(null);
          this.avisos.exito('Borrador guardado.');
          if (!existente) {
            void this.router.navigate(['/preguntas', p.id, 'editar'], { replaceUrl: true });
          }
          return;
        }
        this.preguntas.enviarARevision(p.id).subscribe({
          next: () => {
            this.avisos.exito('Pregunta enviada a revisión.');
            void this.router.navigate(['/mis-preguntas']);
          },
          error: (e) => this.fallar(e, 'Se guardó el borrador, pero no se pudo enviar a revisión. '),
        });
      },
      error: (e) => this.fallar(e),
    });
  }

  private fallar(error: unknown, prefijo = ''): void {
    this.trabajando.set(null);
    const cuerpo = cuerpoDeError(error);
    if (cuerpo?.detalles?.length) {
      this.erroresDelServidor.set(cuerpo.detalles);
    }
    this.avisos.error(prefijo + (cuerpo?.detalles?.length ? cuerpo.mensaje : mensajeDeError(error)));
  }

  private normalizado(): ContenidoPregunta {
    const c = this.contenido();
    const limpio = Object.fromEntries(Object.entries(c).map(([k, v]) => [k, typeof v === 'string' ? v.trim() : v]));
    return { ...(limpio as unknown as ContenidoPregunta), respuestaCorrecta: c.respuestaCorrecta.trim().toUpperCase() };
  }

  private aContenido(p: Pregunta): ContenidoPregunta {
    return {
      nombre: p.nombre,
      contexto: p.contexto,
      enunciado: p.enunciado,
      opcionA: p.opcionA,
      opcionB: p.opcionB,
      opcionC: p.opcionC,
      opcionD: p.opcionD,
      respuestaCorrecta: p.respuestaCorrecta,
      justificacion: p.justificacion,
      bibliografia: p.bibliografia,
      competencia: p.competencia,
      tema: p.tema,
      subtema: p.subtema,
      dificultad: p.dificultad,
    };
  }
}
