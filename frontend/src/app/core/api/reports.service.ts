import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { APP_CONFIG } from '../config';
import { ReporteGrupo } from '../models';

/** Reportes del grupo para docentes y administradores (report-service). */
@Injectable({ providedIn: 'root' })
export class ReportsService {
  private readonly http = inject(HttpClient);
  private readonly base = `${inject(APP_CONFIG).apiUrl}/api/tracking/reports`;

  grupo(): Observable<ReporteGrupo> {
    return this.http.get<ReporteGrupo>(`${this.base}/group`);
  }

  /** El mismo reporte como archivo CSV. */
  grupoCsv(): Observable<Blob> {
    return this.http.get(`${this.base}/group.csv`, { responseType: 'blob' });
  }
}
