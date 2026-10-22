import { codigoCorto, diaRelativo, ENTRADAS, ESTADOS, fecha, fechaCorta, fechaHora, hora, NOTIFICACIONES, textoCompetencia, textoDificultad, textoEstado } from './etiquetas';
import { TipoEntrada, TipoNotificacion } from './models';

describe('etiquetas', () => {
  it('traduce los estados, competencias y dificultades; lo desconocido se deja tal cual', () => {
    expect(textoEstado('PENDIENTE_REVISION')).toBe('Pendiente de revisión');
    expect(textoEstado('XYZ')).toBe('XYZ');
    expect(textoCompetencia('RAZONAMIENTO_CUANTITATIVO')).toBe('Razonamiento cuantitativo');
    expect(textoDificultad('AVANZADO')).toBe('Avanzado');
  });

  it('cada estado de pregunta tiene texto y tono', () => {
    for (const estado of Object.values(ESTADOS)) {
      expect(estado.texto).toBeTruthy();
      expect(estado.tono).toBeTruthy();
    }
    expect(ESTADOS.RECHAZADA.tono).toBe('rojo');
    expect(ESTADOS.APROBADA.tono).toBe('verde');
  });

  it('cada tipo de notificación y de entrada del historial tiene su icono y texto', () => {
    const notificaciones: TipoNotificacion[] = ['REVISORES_ASIGNADOS', 'REVISION_APROBADA', 'REVISION_RECHAZADA', 'OBSERVACION_TARDIA', 'PREGUNTA_PUBLICADA',
      'PREGUNTA_EN_REVISION', 'EVALUACION_RECIBIDA', 'RESULTADO_DE_REVISION'];
    notificaciones.forEach((t) => expect(NOTIFICACIONES[t].icono).toBeTruthy());
    const entradas: TipoEntrada[] = [
      'PREGUNTA_ENVIADA', 'PREGUNTA_REABIERTA', 'PREGUNTA_PUBLICADA', 'CAMBIO_DE_ESTADO', 'REVISORES_ASIGNADOS',
      'EVALUACION_ENVIADA', 'EVALUACION_TARDIA', 'REVISION_COMPLETADA', 'INICIO_DE_SESION', 'INICIO_DE_SESION_FALLIDO',
    ];
    entradas.forEach((t) => expect(ENTRADAS[t].texto).toBeTruthy());
  });

  it('el código corto sale del identificador', () => {
    expect(codigoCorto('dcde4437-7f0c-45ba-9b41-e89718501908')).toBe('SP-DCDE');
  });

  describe('fechas (hora de Colombia, UTC-5)', () => {
    const iso = '2026-10-08T21:10:00Z'; // 16:10 en Bogotá

    it('formatea día, día corto, hora y día con hora', () => {
      expect(fecha(iso)).toBe('8 oct 2026');
      expect(fechaCorta(iso)).toBe('8 oct');
      expect(hora(iso)).toBe('16:10');
      expect(fechaHora(iso)).toBe('8 oct · 16:10');
    });

    it('una fecha ausente se muestra como raya', () => {
      expect(fecha(null)).toBe('—');
      expect(fechaCorta(undefined)).toBe('—');
      expect(fechaHora('')).toBe('—');
    });

    it('agrupa por día relativo: hoy, ayer y fecha larga', () => {
      const ahora = new Date('2026-10-08T21:00:00Z');

      expect(diaRelativo('2026-10-08T13:00:00Z', ahora)).toBe('Hoy · 8 de octubre');
      expect(diaRelativo('2026-10-07T15:00:00Z', ahora)).toBe('Ayer · 7 de octubre');
      expect(diaRelativo('2026-10-06T15:00:00Z', ahora)).toBe('6 de octubre');
    });

    it('el día se cuenta en hora de Colombia, no en UTC', () => {
      const ahora = new Date('2026-10-09T03:00:00Z'); // 22:00 del 8 en Bogotá

      expect(diaRelativo('2026-10-08T14:00:00Z', ahora)).toBe('Hoy · 8 de octubre');
    });
  });
});
