import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { APP_CONFIG } from '../config';
import { CriteriosSimulacro, Pagina, ResumenCatalogo, ResumenSimulacro, Simulacro } from '../models';
import { parametros } from './http-util';

/** Simulacros (simulation-service) y resumen del catálogo de preguntas publicadas (catalog-service). */
@Injectable({ providedIn: 'root' })
export class SimulationsService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = inject(APP_CONFIG).apiUrl;
  private readonly base = `${this.apiUrl}/api/simulations`;

  /** Cuántas preguntas publicadas hay, por competencia y dificultad. */
  catalogo(): Observable<ResumenCatalogo> {
    return this.http.get<ResumenCatalogo>(`${this.apiUrl}/api/simulations/catalog/summary`);
  }

  crear(criterios: CriteriosSimulacro): Observable<Simulacro> {
    return this.http.post<Simulacro>(this.base, criterios);
  }

  mios(page = 0, size = 10): Observable<Pagina<ResumenSimulacro>> {
    return this.http.get<Pagina<ResumenSimulacro>>(this.base, { params: parametros({ page, size }) });
  }

  obtener(id: string): Observable<Simulacro> {
    return this.http.get<Simulacro>(`${this.base}/${id}`);
  }

  responder(id: string, preguntaId: string, opcion: string): Observable<Simulacro> {
    return this.http.post<Simulacro>(`${this.base}/${id}/answers`, { preguntaId, opcion });
  }

  finalizar(id: string): Observable<Simulacro> {
    return this.http.post<Simulacro>(`${this.base}/${id}/finish`, null);
  }
}
