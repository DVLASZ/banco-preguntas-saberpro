/** Tipos de los datos que intercambia el API. Los nombres coinciden con los de los servicios. */

export type Rol = 'ADMIN' | 'AUTHOR' | 'REVIEWER' | 'TEACHER' | 'STUDENT';

export type EstadoPregunta =
  | 'BORRADOR'
  | 'PENDIENTE_REVISION'
  | 'EN_REVISION'
  | 'APROBADA'
  | 'RECHAZADA'
  | 'PUBLICADA'
  | 'ARCHIVADA';

export type Competencia =
  | 'LECTURA_CRITICA'
  | 'RAZONAMIENTO_CUANTITATIVO'
  | 'COMPETENCIAS_CIUDADANAS'
  | 'COMUNICACION_ESCRITA'
  | 'INGLES';

export type Dificultad = 'BASICO' | 'INTERMEDIO' | 'AVANZADO';

export type Decision = 'APROBAR' | 'RECHAZAR';

export type EstadoRonda = 'PENDIENTE_ASIGNACION' | 'EN_CURSO' | 'RECHAZADA' | 'APROBADA' | 'CERRADA';

export interface Pagina<T> {
  contenido: T[];
  /** Cuenta desde 0. */
  pagina: number;
  tamano: number;
  totalElementos: number;
  totalPaginas: number;
}

export interface ApiErrorBody {
  codigo: string;
  mensaje: string;
  detalles?: string[];
}

// ---------------------------------------------------------------- preguntas

export interface CambioEstado {
  desde: EstadoPregunta | null;
  hacia: EstadoPregunta;
  cambiadoPor: string | null;
  fecha: string;
}

export interface Pregunta {
  id: string;
  autorId: string;
  nombre: string;
  contexto: string;
  enunciado: string;
  opcionA: string;
  opcionB: string;
  opcionC: string;
  opcionD: string;
  respuestaCorrecta: string;
  justificacion: string;
  bibliografia: string;
  competencia: Competencia;
  tema: string;
  subtema: string;
  dificultad: Dificultad;
  estado: EstadoPregunta;
  version: number;
  creadaEn: string;
  actualizadaEn: string;
  historialEstados?: CambioEstado[];
}

/** Lo que el autor escribe: el id, el autor y el estado los decide el servidor. */
export type ContenidoPregunta = Omit<
  Pregunta,
  'id' | 'autorId' | 'estado' | 'version' | 'creadaEn' | 'actualizadaEn' | 'historialEstados'
>;

export interface FiltroPreguntas {
  estado?: EstadoPregunta | '';
  competencia?: Competencia | '';
  texto?: string;
  autor?: string;
  page?: number;
  size?: number;
}

// ---------------------------------------------------------------- revisión

export interface CasoPendiente {
  preguntaId: string;
  titulo: string;
  competencia: string;
  autorId: string;
  ronda: number;
  enviadaEn: string;
}

export interface AsignacionRevisor {
  preguntaId: string;
  titulo: string;
  competencia: string;
  autorId: string;
  ronda: number;
  asignadaEn: string;
  estadoRonda: EstadoRonda;
  miDecision: Decision | null;
  pendiente: boolean;
}

export interface EvaluacionRevisor {
  decision: Decision;
  observacion: string | null;
  enviadaEn: string;
  tardia: boolean;
}

export interface RevisorDeRonda {
  revisorId: string;
  asignadoPor: string;
  asignadoEn: string;
  automatica: boolean;
  evaluacion: EvaluacionRevisor | null;
}

export interface RondaRevision {
  numero: number;
  versionDePregunta: number;
  estado: EstadoRonda;
  iniciadaEn: string;
  decididaEn: string | null;
  cerradaEn: string | null;
  totalDeRevisores: number;
  totalDeEvaluadas: number;
  detalleVisible: boolean;
  revisores: RevisorDeRonda[];
}

export interface Revision {
  preguntaId: string;
  autorId: string;
  titulo: string;
  competencia: string;
  rondas: RondaRevision[];
}

export interface ConfigRevision {
  minimoDeRevisores: number;
  maximoDeRevisores: number;
}

export interface ResultadoEvaluacion {
  resultadoDeLaRonda: 'PENDIENTE' | 'APROBADA' | 'RECHAZADA';
  tardia: boolean;
  ronda: number;
}

// ---------------------------------------------------------------- usuarios

export interface Usuario {
  id: string;
  nombre: string;
  correo: string | null;
  roles: string[];
  activo: boolean;
  ultimoInicioDeSesion: string | null;
}

export interface Yo {
  id: string;
  nombre: string;
  correo: string | null;
  roles: string[];
  sincronizado: boolean;
  ultimoInicioDeSesion: string | null;
}

// ---------------------------------------------------------------- notificaciones e historial

export type TipoNotificacion =
  | 'REVISORES_ASIGNADOS'
  | 'REVISION_APROBADA'
  | 'REVISION_RECHAZADA'
  | 'OBSERVACION_TARDIA'
  | 'PREGUNTA_PUBLICADA'
  | 'PREGUNTA_EN_REVISION'
  | 'EVALUACION_RECIBIDA'
  | 'RESULTADO_DE_REVISION';

export interface Notificacion {
  id: string;
  tipo: TipoNotificacion;
  preguntaId: string | null;
  asunto: string;
  mensaje: string;
  creadaEn: string;
  leida: boolean;
  leidaEn: string | null;
  correoEnviadoEn: string | null;
}

export interface CorreoSimulado {
  notificacionId: string;
  para: string;
  destinatarioId: string;
  asunto: string;
  cuerpo: string;
  enviadoEn: string;
}

export type TipoEntrada =
  | 'PREGUNTA_ENVIADA'
  | 'PREGUNTA_REABIERTA'
  | 'PREGUNTA_PUBLICADA'
  | 'CAMBIO_DE_ESTADO'
  | 'REVISORES_ASIGNADOS'
  | 'EVALUACION_ENVIADA'
  | 'EVALUACION_TARDIA'
  | 'REVISION_COMPLETADA'
  | 'INICIO_DE_SESION'
  | 'INICIO_DE_SESION_FALLIDO';

export interface EntradaHistorial {
  id: string;
  tipo: TipoEntrada;
  preguntaId: string | null;
  actorId: string | null;
  resumen: string;
  detalles: Record<string, unknown> | null;
  ocurridaEn: string;
}

export interface FiltroAuditoria {
  tipo?: TipoEntrada | '';
  actorId?: string;
  preguntaId?: string;
  page?: number;
  size?: number;
}

// ---------------------------------------------------------------- simulacros y seguimiento

/** Cuántas preguntas publicadas hay disponibles para armar simulacros (catalog-service). */
export interface ResumenCatalogo {
  total: number;
  porCompetencia: Record<string, number>;
  porDificultad: Record<string, number>;
}

export interface CriteriosSimulacro {
  competencias: Competencia[];
  dificultades: Dificultad[];
  cantidad: number;
  duracionMinutos: number;
}

export type EstadoSimulacro = 'EN_CURSO' | 'FINALIZADO' | 'EXPIRADO';

/** Una pregunta del simulacro; la respuesta correcta solo llega cuando el simulacro terminó. */
export interface ItemSimulacro {
  orden: number;
  preguntaId: string;
  competencia: string;
  tema: string;
  dificultad: string;
  contexto: string;
  enunciado: string;
  opcionA: string;
  opcionB: string;
  opcionC: string;
  opcionD: string;
  respuesta?: string;
  respuestaCorrecta?: string;
  acertada?: boolean;
}

export interface AciertosCompetencia {
  competencia: string;
  correctas: number;
  total: number;
  porcentaje: number;
}

export interface ResultadoSimulacro {
  total: number;
  correctas: number;
  incorrectas: number;
  sinResponder: number;
  puntaje: number;
  porCompetencia: AciertosCompetencia[];
}

export interface Simulacro {
  id: string;
  estudianteId: string;
  estado: EstadoSimulacro;
  iniciadoEn: string;
  venceEn: string;
  finalizadoEn?: string;
  segundosRestantes: number;
  duracionMinutos: number;
  totalPreguntas: number;
  respondidas: number;
  preguntas: ItemSimulacro[];
  resultado?: ResultadoSimulacro;
}

export interface ResumenSimulacro {
  id: string;
  estado: EstadoSimulacro;
  iniciadoEn: string;
  finalizadoEn?: string;
  totalPreguntas: number;
  respondidas: number;
  duracionMinutos: number;
  puntaje?: number;
}

export interface EntradaSeguimiento {
  simulacroId: string;
  iniciadoEn: string;
  finalizadoEn: string;
  duracionSegundos: number;
  expirado: boolean;
  total: number;
  correctas: number;
  incorrectas: number;
  sinResponder: number;
  puntaje: number;
  porCompetencia: AciertosCompetencia[];
}

export type Tendencia = 'MEJORANDO' | 'ESTABLE' | 'BAJANDO' | 'SIN_DATOS';

export interface Estadisticas {
  estudianteId: string;
  simulacros: number;
  expirados: number;
  puntajePromedio: number;
  mejorPuntaje: number;
  peorPuntaje: number;
  ultimoPuntaje?: number;
  ultimoSimulacro?: string;
  tiempoPromedioSegundos: number;
  tendencia: Tendencia;
  porCompetencia: AciertosCompetencia[];
}

export interface Fortalezas {
  estudianteId: string;
  umbralFortaleza: number;
  umbralDebilidad: number;
  minimoDePreguntas: number;
  fortalezas: AciertosCompetencia[];
  debilidades: AciertosCompetencia[];
  intermedias: AciertosCompetencia[];
  sinDatosSuficientes: AciertosCompetencia[];
}

export interface FilaEstudiante {
  estudianteId: string;
  simulacros: number;
  promedio: number;
  mejorPuntaje: number;
  ultimoPuntaje: number;
  tendencia: Tendencia;
  debilidades: string[];
}

export interface ReporteGrupo {
  generadoEn: string;
  estudiantes: number;
  simulacros: number;
  promedioGeneral: number;
  porCompetencia: AciertosCompetencia[];
  estudiantesDetalle: FilaEstudiante[];
}
