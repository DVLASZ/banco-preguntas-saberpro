import { ContenidoPregunta } from '../../core/models';
import { CONTENIDO_VACIO, LIMITES, validarContenido } from './validacion';

const valido: ContenidoPregunta = {
  nombre: 'Patrón Observer',
  contexto: 'Una aplicación debe actualizar varias ventanas.',
  enunciado: '¿Qué patrón notifica a varios objetos?',
  opcionA: 'Factory',
  opcionB: 'Observer',
  opcionC: 'Singleton',
  opcionD: 'Adapter',
  respuestaCorrecta: 'B',
  justificacion: 'Observer define una dependencia uno a muchos.',
  bibliografia: 'Gamma et al. (1994)',
  competencia: 'LECTURA_CRITICA',
  tema: 'Patrones',
  subtema: 'Observer',
  dificultad: 'INTERMEDIO',
};

describe('validarContenido', () => {
  it('un contenido completo no tiene errores', () => {
    expect(validarContenido(valido)).toEqual({});
  });

  it('un contenido vacío marca todos los campos como obligatorios', () => {
    const errores = validarContenido(CONTENIDO_VACIO);

    expect(Object.keys(errores)).toHaveLength(14);
    expect(errores.contexto).toBe('Este campo es obligatorio.');
  });

  it('los espacios solos no cuentan como contenido', () => {
    expect(validarContenido({ ...valido, nombre: '   ' }).nombre).toBe('Este campo es obligatorio.');
  });

  it('respeta los largos máximos de cada campo', () => {
    const errores = validarContenido({
      ...valido,
      nombre: 'x'.repeat(LIMITES.nombre + 1),
      opcionA: 'x'.repeat(501),
      contexto: 'x'.repeat(LIMITES.contexto),
    });

    expect(errores.nombre).toBe('No puede superar 200 caracteres.');
    expect(errores.opcionA).toBe('No puede superar 500 caracteres.');
    expect(errores.contexto).toBeUndefined();
  });

  it('la respuesta correcta debe ser A, B, C o D (sin importar mayúsculas)', () => {
    expect(validarContenido({ ...valido, respuestaCorrecta: 'b' }).respuestaCorrecta).toBeUndefined();
    expect(validarContenido({ ...valido, respuestaCorrecta: 'E' }).respuestaCorrecta).toBe('Debe ser A, B, C o D.');
    expect(validarContenido({ ...valido, respuestaCorrecta: 'AB' }).respuestaCorrecta).toBeTruthy();
  });
});
