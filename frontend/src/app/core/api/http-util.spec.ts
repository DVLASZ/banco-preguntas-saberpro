import { HttpErrorResponse } from '@angular/common/http';
import { cuerpoDeError, mensajeDeError, parametros } from './http-util';

describe('http-util', () => {
  describe('parametros', () => {
    it('omite los vacíos y convierte lo demás a texto', () => {
      const p = parametros({ texto: 'observer', estado: '', page: 0, size: 10, activo: false, nada: null, falta: undefined });

      expect(p.keys().sort()).toEqual(['activo', 'page', 'size', 'texto']);
      expect(p.get('page')).toBe('0');
      expect(p.get('activo')).toBe('false');
    });
  });

  describe('mensajeDeError', () => {
    const error = (estado: number, cuerpo: unknown = null) => new HttpErrorResponse({ status: estado, error: cuerpo });

    it('sin conexión', () => {
      expect(mensajeDeError(error(0))).toContain('No hay conexión');
    });

    it('usa el mensaje del cuerpo de error común y agrega sus detalles', () => {
      const e = error(400, { codigo: 'VALIDACION_ESTRUCTURAL', mensaje: 'La pregunta no es válida', detalles: ['opcionA: repetida', 'opcionB: vacía'] });

      expect(mensajeDeError(e)).toBe('La pregunta no es válida (opcionA: repetida; opcionB: vacía)');
    });

    it('sin detalles, solo el mensaje', () => {
      expect(mensajeDeError(error(409, { codigo: 'OPERACION_NO_PERMITIDA', mensaje: 'Ya evaluaste esta pregunta' }))).toBe('Ya evaluaste esta pregunta');
    });

    it('mensajes por estado cuando el cuerpo no es el común', () => {
      expect(mensajeDeError(error(401))).toContain('sesión expiró');
      expect(mensajeDeError(error(403))).toContain('No tienes permiso');
      expect(mensajeDeError(error(404))).toContain('No se encontró');
      expect(mensajeDeError(error(500))).toContain('problema');
      expect(mensajeDeError(error(503, '<html>'))).toContain('problema');
    });

    it('un error desconocido tiene mensaje genérico', () => {
      expect(mensajeDeError(new Error('x'))).toBe('Ocurrió un error inesperado.');
      expect(mensajeDeError(error(418))).toBe('Ocurrió un error inesperado.');
    });
  });

  it('cuerpoDeError solo reconoce el formato común', () => {
    expect(cuerpoDeError(new HttpErrorResponse({ status: 400, error: { codigo: 'X', mensaje: 'm' } }))?.codigo).toBe('X');
    expect(cuerpoDeError(new HttpErrorResponse({ status: 400, error: 'texto' }))).toBeNull();
    expect(cuerpoDeError(new Error('x'))).toBeNull();
  });
});
