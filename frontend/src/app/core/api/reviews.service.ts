import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { APP_CONFIG } from '../config';
import {
  AsignacionRevisor,
  CasoPendiente,
  ConfigRevision,
  Decision,
  Pagina,
  ResultadoEvaluacion,
  Revision,
} from '../models';
import { parametros } from './http-util';

/** Revisión por pares (servicio review-service, a través del gateway). */
@Injectable({ providedIn: 'root' })
export class ReviewsService {
  private readonly http = inject(HttpClient);
  private readonly base = `${inject(APP_CONFIG).apiUrl}/api/reviews`;

  /** Mínimo y máximo de revisores por pregunta. */
  configuracion(): Observable<ConfigRevision> {
    return this.http.get<ConfigRevision>(`${this.base}/config`);
  }

  /** Preguntas que esperan revisores (rol ADMIN). */
  pendientesDeAsignacion(page = 0, size = 10): Observable<Pagina<CasoPendiente>> {
    return this.http.get<Pagina<CasoPendiente>>(`${this.base}/pending-assignment`, { params: parametros({ page, size }) });
  }

  /** Asigna revisores a la ronda vigente, o agrega más a una en curso (rol ADMIN). */
  asignar(preguntaId: string, reviewerIds: string[]): Observable<Revision> {
    return this.http.post<Revision>(`${this.base}/${preguntaId}/assignments`, { reviewerIds });
  }

  /** La bandeja del revisor autenticado. */
  bandeja(estado: 'PENDIENTES' | 'EVALUADAS' | 'TODAS' = 'PENDIENTES', page = 0, size = 10): Observable<Pagina<AsignacionRevisor>> {
    return this.http.get<Pagina<AsignacionRevisor>>(`${this.base}/assigned`, { params: parametros({ estado, page, size }) });
  }

  /** La revisión de una pregunta tal como la ve quien consulta (autor, revisor o administrador). */
  revision(preguntaId: string): Observable<Revision> {
    return this.http.get<Revision>(`${this.base}/${preguntaId}`);
  }

  evaluar(preguntaId: string, decision: Decision, observacion: string | null): Observable<ResultadoEvaluacion> {
    return this.http.post<ResultadoEvaluacion>(`${this.base}/${preguntaId}/evaluations`, {
      decision,
      observacion: observacion && observacion.trim() ? observacion.trim() : null,
    });
  }
}
