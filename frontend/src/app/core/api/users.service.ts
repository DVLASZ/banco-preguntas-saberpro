import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { APP_CONFIG } from '../config';
import { Pagina, Usuario, Yo } from '../models';
import { parametros } from './http-util';

/** Directorio de usuarios (servicio user-service, sincronizado con Keycloak). */
@Injectable({ providedIn: 'root' })
export class UsersService {
  private readonly http = inject(HttpClient);
  private readonly base = `${inject(APP_CONFIG).apiUrl}/api/users`;

  yo(): Observable<Yo> {
    return this.http.get<Yo>(`${this.base}/me`);
  }

  /** Lista usuarios (rol ADMIN), por ejemplo los candidatos a revisor con `rol: 'REVIEWER'`. */
  listar(filtro: { rol?: string; texto?: string; soloActivos?: boolean; page?: number; size?: number } = {}): Observable<Pagina<Usuario>> {
    return this.http.get<Pagina<Usuario>>(this.base, { params: parametros({ ...filtro }) });
  }
}
