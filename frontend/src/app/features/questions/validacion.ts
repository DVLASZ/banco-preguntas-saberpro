import { ContenidoPregunta } from '../../core/models';

type Campo = keyof ContenidoPregunta;

/** Límites de longitud que también aplica el servidor (0: sin límite de texto, es una lista cerrada). */
export const LIMITES: Record<Campo, number> = {
  nombre: 200,
  contexto: 4000,
  enunciado: 2000,
  opcionA: 500,
  opcionB: 500,
  opcionC: 500,
  opcionD: 500,
  respuestaCorrecta: 1,
  justificacion: 4000,
  bibliografia: 1000,
  competencia: 0,
  tema: 200,
  subtema: 200,
  dificultad: 0,
};

export const CAMPOS: readonly Campo[] = Object.keys(LIMITES) as Campo[];

export const CONTENIDO_VACIO: ContenidoPregunta = {
  nombre: '',
  contexto: '',
  enunciado: '',
  opcionA: '',
  opcionB: '',
  opcionC: '',
  opcionD: '',
  respuestaCorrecta: '',
  justificacion: '',
  bibliografia: '',
  competencia: '' as ContenidoPregunta['competencia'],
  tema: '',
  subtema: '',
  dificultad: '' as ContenidoPregunta['dificultad'],
};

/**
 * Comprobaciones de formato antes de llamar al servidor: obligatorios y largos máximos. La validación
 * estructural del contenido (por ejemplo, que no haya opciones repetidas) la hace el servidor.
 *
 * @returns un mensaje por cada campo con problema
 */
export function validarContenido(c: ContenidoPregunta): Partial<Record<Campo, string>> {
  const errores: Partial<Record<Campo, string>> = {};
  for (const campo of CAMPOS) {
    const valor = String(c[campo] ?? '').trim();
    if (valor === '') {
      errores[campo] = 'Este campo es obligatorio.';
      continue;
    }
    const maximo = LIMITES[campo];
    if (maximo > 0 && valor.length > maximo) {
      errores[campo] = `No puede superar ${maximo} caracteres.`;
    }
  }
  if (!errores.respuestaCorrecta && !/^[A-Da-d]$/.test(c.respuestaCorrecta.trim())) {
    errores.respuestaCorrecta = 'Debe ser A, B, C o D.';
  }
  return errores;
}
