import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { APP_CONFIG } from '../config';
import { EntradaHistorial, FiltroAuditoria, Pagina } from '../models';
import { parametros } from './http-util';

/** Historial y auditoría (servicio history-service). Cada rol recibe solo la parte que le corresponde. */
@Injectable({ providedIn: 'root' })
export class HistoryService {
  private readonly http = inject(HttpClient);
  private readonly base = `${inject(APP_CONFIG).apiUrl}/api/history`;

  deUnaPregunta(preguntaId: string): Observable<EntradaHistorial[]> {
    return this.http.get<EntradaHistorial[]>(`${this.base}/questions/${preguntaId}`);
  }

  /** Todo lo que el usuario puede ver, lo más reciente primero. */
  mio(page = 0, size = 20): Observable<Pagina<EntradaHistorial>> {
    return this.http.get<Pagina<EntradaHistorial>>(`${this.base}/me`, { params: parametros({ page, size }) });
  }

  /** Auditoría completa, con inicios de sesión (rol ADMIN). */
  auditoria(filtro: FiltroAuditoria = {}): Observable<Pagina<EntradaHistorial>> {
    return this.http.get<Pagina<EntradaHistorial>>(`${this.base}/audit`, { params: parametros({ ...filtro }) });
  }
}
