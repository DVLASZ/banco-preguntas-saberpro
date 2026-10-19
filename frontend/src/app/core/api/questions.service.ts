import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { APP_CONFIG } from '../config';
import { ContenidoPregunta, FiltroPreguntas, Pagina, Pregunta } from '../models';
import { parametros } from './http-util';

/** Preguntas (servicio question-service, a través del gateway). */
@Injectable({ providedIn: 'root' })
export class QuestionsService {
  private readonly http = inject(HttpClient);
  private readonly base = `${inject(APP_CONFIG).apiUrl}/api/questions`;

  /** Las preguntas del usuario autenticado (rol AUTHOR). */
  listarMias(filtro: FiltroPreguntas = {}): Observable<Pagina<Pregunta>> {
    return this.http.get<Pagina<Pregunta>>(this.base, { params: parametros({ ...filtro }) });
  }

  /** Las preguntas de todos los autores (rol ADMIN). */
  listarTodas(filtro: FiltroPreguntas = {}): Observable<Pagina<Pregunta>> {
    return this.http.get<Pagina<Pregunta>>(`${this.base}/all`, { params: parametros({ ...filtro }) });
  }

  obtener(id: string): Observable<Pregunta> {
    return this.http.get<Pregunta>(`${this.base}/${id}`);
  }

  crear(contenido: ContenidoPregunta): Observable<Pregunta> {
    return this.http.post<Pregunta>(this.base, contenido);
  }

  actualizar(id: string, contenido: ContenidoPregunta): Observable<Pregunta> {
    return this.http.put<Pregunta>(`${this.base}/${id}`, contenido);
  }

  enviarARevision(id: string): Observable<Pregunta> {
    return this.http.post<Pregunta>(`${this.base}/${id}/submit`, null);
  }

  /** Reabre una pregunta rechazada para corregirla: vuelve a borrador como una versión nueva. */
  reabrir(id: string): Observable<Pregunta> {
    return this.http.post<Pregunta>(`${this.base}/${id}/reopen`, null);
  }

  publicar(id: string): Observable<Pregunta> {
    return this.http.post<Pregunta>(`${this.base}/${id}/publish`, null);
  }
}
