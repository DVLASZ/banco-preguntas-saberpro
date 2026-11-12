import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { APP_CONFIG } from '../config';
import { EntradaSeguimiento, Estadisticas, Fortalezas, Pagina } from '../models';
import { parametros } from './http-util';

/** Seguimiento académico (statistics-service): lo propio del estudiante y, para docentes, el de cualquier estudiante. */
@Injectable({ providedIn: 'root' })
export class StatisticsService {
  private readonly http = inject(HttpClient);
  private readonly base = `${inject(APP_CONFIG).apiUrl}/api/tracking`;

  miHistorial(page = 0, size = 10): Observable<Pagina<EntradaSeguimiento>> {
    return this.http.get<Pagina<EntradaSeguimiento>>(`${this.base}/me/history`, { params: parametros({ page, size }) });
  }

  misEstadisticas(): Observable<Estadisticas> {
    return this.http.get<Estadisticas>(`${this.base}/me/stats`);
  }

  misCompetencias(): Observable<Fortalezas> {
    return this.http.get<Fortalezas>(`${this.base}/me/competencies`);
  }

  historialDe(estudianteId: string, page = 0, size = 10): Observable<Pagina<EntradaSeguimiento>> {
    return this.http.get<Pagina<EntradaSeguimiento>>(`${this.base}/students/${encodeURIComponent(estudianteId)}/history`, {
      params: parametros({ page, size }),
    });
  }

  estadisticasDe(estudianteId: string): Observable<Estadisticas> {
    return this.http.get<Estadisticas>(`${this.base}/students/${encodeURIComponent(estudianteId)}/stats`);
  }

  competenciasDe(estudianteId: string): Observable<Fortalezas> {
    return this.http.get<Fortalezas>(`${this.base}/students/${encodeURIComponent(estudianteId)}/competencies`);
  }
}
