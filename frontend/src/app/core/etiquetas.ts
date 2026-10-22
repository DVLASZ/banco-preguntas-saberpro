import { Competencia, Dificultad, EstadoPregunta, EstadoRonda, TipoEntrada, TipoNotificacion } from './models';

/** Tonos de color de la interfaz (ver `styles.scss`). */
export type Tono = 'rojo' | 'ambar' | 'azul' | 'verde' | 'gris' | 'turquesa';

export const ESTADOS: Record<EstadoPregunta, { texto: string; tono: Tono }> = {
  BORRADOR: { texto: 'Borrador', tono: 'gris' },
  PENDIENTE_REVISION: { texto: 'Pendiente de revisión', tono: 'ambar' },
  EN_REVISION: { texto: 'En revisión', tono: 'azul' },
  APROBADA: { texto: 'Aprobada', tono: 'verde' },
  RECHAZADA: { texto: 'Rechazada', tono: 'rojo' },
  PUBLICADA: { texto: 'Publicada', tono: 'turquesa' },
  ARCHIVADA: { texto: 'Archivada', tono: 'gris' },
};

export const COMPETENCIAS: Record<Competencia, string> = {
  LECTURA_CRITICA: 'Lectura crítica',
  RAZONAMIENTO_CUANTITATIVO: 'Razonamiento cuantitativo',
  COMPETENCIAS_CIUDADANAS: 'Competencias ciudadanas',
  COMUNICACION_ESCRITA: 'Comunicación escrita',
  INGLES: 'Inglés',
};

export const DIFICULTADES: Record<Dificultad, string> = {
  BASICO: 'Básico',
  INTERMEDIO: 'Intermedio',
  AVANZADO: 'Avanzado',
};

export const ESTADOS_RONDA: Record<EstadoRonda, { texto: string; tono: Tono }> = {
  PENDIENTE_ASIGNACION: { texto: 'Esperando revisores', tono: 'ambar' },
  EN_CURSO: { texto: 'En curso', tono: 'azul' },
  RECHAZADA: { texto: 'Rechazada', tono: 'rojo' },
  APROBADA: { texto: 'Aprobada', tono: 'verde' },
  CERRADA: { texto: 'Cerrada', tono: 'gris' },
};

export const OPCIONES_COMPETENCIA = Object.entries(COMPETENCIAS).map(([valor, texto]) => ({ valor: valor as Competencia, texto }));
export const OPCIONES_DIFICULTAD = Object.entries(DIFICULTADES).map(([valor, texto]) => ({ valor: valor as Dificultad, texto }));
export const OPCIONES_ESTADO = (Object.keys(ESTADOS) as EstadoPregunta[]).map((valor) => ({ valor, texto: ESTADOS[valor].texto }));

export function textoEstado(estado: string): string {
  return ESTADOS[estado as EstadoPregunta]?.texto ?? estado;
}

export function textoCompetencia(competencia: string): string {
  return COMPETENCIAS[competencia as Competencia] ?? competencia;
}

export function textoDificultad(dificultad: string): string {
  return DIFICULTADES[dificultad as Dificultad] ?? dificultad;
}

/** Código corto legible de una pregunta a partir de su identificador: «SP-1A2B». */
export function codigoCorto(id: string): string {
  return 'SP-' + id.replace(/-/g, '').slice(0, 4).toUpperCase();
}

// ---------------------------------------------------------------- notificaciones

export const NOTIFICACIONES: Record<TipoNotificacion, { icono: string; tono: Tono; grupo: 'revision' | 'sistema'; titulo: string }> = {
  REVISORES_ASIGNADOS: { icono: 'usuarios', tono: 'azul', grupo: 'revision', titulo: 'Revisores asignados' },
  REVISION_APROBADA: { icono: 'escudo-ok', tono: 'verde', grupo: 'revision', titulo: 'Pregunta aprobada' },
  REVISION_RECHAZADA: { icono: 'alerta', tono: 'rojo', grupo: 'revision', titulo: 'Pregunta rechazada' },
  OBSERVACION_TARDIA: { icono: 'mensaje', tono: 'ambar', grupo: 'revision', titulo: 'Nueva observación' },
  PREGUNTA_PUBLICADA: { icono: 'publicar', tono: 'turquesa', grupo: 'sistema', titulo: 'Pregunta publicada' },
  PREGUNTA_EN_REVISION: { icono: 'usuarios', tono: 'azul', grupo: 'revision', titulo: 'Pregunta en revisión' },
  EVALUACION_RECIBIDA: { icono: 'clip', tono: 'azul', grupo: 'revision', titulo: 'Evaluación recibida' },
  RESULTADO_DE_REVISION: { icono: 'escudo-ok', tono: 'verde', grupo: 'revision', titulo: 'Revisión terminada' },
};

// ---------------------------------------------------------------- historial

export const ENTRADAS: Record<TipoEntrada, { icono: string; tono: Tono; texto: string }> = {
  PREGUNTA_ENVIADA: { icono: 'enviar', tono: 'ambar', texto: 'Envío a revisión' },
  PREGUNTA_REABIERTA: { icono: 'editar', tono: 'azul', texto: 'Reapertura' },
  PREGUNTA_PUBLICADA: { icono: 'publicar', tono: 'turquesa', texto: 'Publicación' },
  CAMBIO_DE_ESTADO: { icono: 'flujo', tono: 'gris', texto: 'Cambio de estado' },
  REVISORES_ASIGNADOS: { icono: 'usuarios', tono: 'azul', texto: 'Asignación de revisores' },
  EVALUACION_ENVIADA: { icono: 'clip', tono: 'azul', texto: 'Evaluación' },
  EVALUACION_TARDIA: { icono: 'mensaje', tono: 'ambar', texto: 'Observación tardía' },
  REVISION_COMPLETADA: { icono: 'escudo-ok', tono: 'verde', texto: 'Resultado de la ronda' },
  INICIO_DE_SESION: { icono: 'entrar', tono: 'verde', texto: 'Inicio de sesión' },
  INICIO_DE_SESION_FALLIDO: { icono: 'alerta', tono: 'rojo', texto: 'Inicio de sesión fallido' },
};

export const OPCIONES_TIPO_ENTRADA = (Object.keys(ENTRADAS) as TipoEntrada[]).map((valor) => ({ valor, texto: ENTRADAS[valor].texto }));

// ---------------------------------------------------------------- fechas

const formatoDia = new Intl.DateTimeFormat('es-CO', { day: 'numeric', month: 'short', year: 'numeric', timeZone: 'America/Bogota' });
const formatoDiaCorto = new Intl.DateTimeFormat('es-CO', { day: 'numeric', month: 'short', timeZone: 'America/Bogota' });
const formatoHora = new Intl.DateTimeFormat('es-CO', { hour: '2-digit', minute: '2-digit', hour12: false, timeZone: 'America/Bogota' });
const formatoLargo = new Intl.DateTimeFormat('es-CO', { day: 'numeric', month: 'long', timeZone: 'America/Bogota' });

function parte(formato: Intl.DateTimeFormat, fecha: Date, tipo: Intl.DateTimeFormatPartTypes): string {
  return formato.formatToParts(fecha).find((p) => p.type === tipo)?.value.replace(/\./g, '') ?? '';
}

/** «8 oct 2026» */
export function fecha(iso: string | null | undefined): string {
  if (!iso) {
    return '—';
  }
  const d = new Date(iso);
  return `${parte(formatoDia, d, 'day')} ${parte(formatoDia, d, 'month')} ${parte(formatoDia, d, 'year')}`;
}

/** «8 oct» */
export function fechaCorta(iso: string | null | undefined): string {
  if (!iso) {
    return '—';
  }
  const d = new Date(iso);
  return `${parte(formatoDiaCorto, d, 'day')} ${parte(formatoDiaCorto, d, 'month')}`;
}

/** «8 oct · 16:10» */
export function fechaHora(iso: string | null | undefined): string {
  return iso ? `${fechaCorta(iso)} · ${formatoHora.format(new Date(iso))}` : '—';
}

/** «16:10» */
export function hora(iso: string): string {
  return formatoHora.format(new Date(iso));
}

/** Encabezado de grupo de notificaciones: «Hoy · 8 de octubre», «Ayer · 7 de octubre» o «6 de octubre». */
export function diaRelativo(iso: string, ahora: Date = new Date()): string {
  const clave = (d: Date) => new Intl.DateTimeFormat('en-CA', { timeZone: 'America/Bogota' }).format(d);
  const dia = new Date(iso);
  const largo = formatoLargo.format(dia).replace(/^(\d+) de /, '$1 de ');
  const ayer = new Date(ahora.getTime() - 86_400_000);
  if (clave(dia) === clave(ahora)) {
    return `Hoy · ${largo}`;
  }
  if (clave(dia) === clave(ayer)) {
    return `Ayer · ${largo}`;
  }
  return largo;
}
