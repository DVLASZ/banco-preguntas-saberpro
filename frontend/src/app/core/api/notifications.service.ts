import { HttpClient } from '@angular/common/http';
import { Injectable, inject, signal } from '@angular/core';
import { Observable, tap } from 'rxjs';
import { APP_CONFIG } from '../config';
import { CorreoSimulado, Notificacion, Pagina } from '../models';
import { parametros } from './http-util';

/** Notificaciones (servicio notification-service). Mantiene el contador de no leídas que muestra el menú. */
@Injectable({ providedIn: 'root' })
export class NotificationsService {
  private readonly http = inject(HttpClient);
  private readonly base = `${inject(APP_CONFIG).apiUrl}/api/notifications`;

  private readonly _noLeidas = signal(0);
  readonly noLeidas = this._noLeidas.asReadonly();

  bandeja(soloNoLeidas = false, page = 0, size = 20): Observable<Pagina<Notificacion>> {
    return this.http.get<Pagina<Notificacion>>(this.base, { params: parametros({ soloNoLeidas, page, size }) });
  }

  /** Consulta cuántas hay sin leer y actualiza el contador. */
  refrescarNoLeidas(): Observable<{ noLeidas: number }> {
    return this.http
      .get<{ noLeidas: number }>(`${this.base}/unread-count`)
      .pipe(tap((r) => this._noLeidas.set(r.noLeidas)));
  }

  marcarLeida(id: string): Observable<Notificacion> {
    return this.http
      .post<Notificacion>(`${this.base}/${id}/read`, null)
      .pipe(tap(() => this._noLeidas.update((n) => Math.max(0, n - 1))));
  }

  marcarTodasLeidas(): Observable<{ marcadas: number }> {
    return this.http.post<{ marcadas: number }>(`${this.base}/read-all`, null).pipe(tap(() => this._noLeidas.set(0)));
  }

  /** Correos simulados enviados (rol ADMIN). */
  correos(page = 0, size = 10): Observable<Pagina<CorreoSimulado>> {
    return this.http.get<Pagina<CorreoSimulado>>(`${this.base}/emails`, { params: parametros({ page, size }) });
  }
}
