import { Injectable } from '@angular/core';
import { Subject } from 'rxjs';

/** Las pantallas avisan aquí cuando algo cambió (se asignó, se evaluó…) para que el menú actualice sus contadores. */
@Injectable({ providedIn: 'root' })
export class RefrescoService {
  private readonly _contadores = new Subject<void>();
  readonly contadores$ = this._contadores.asObservable();

  avisarCambioDeContadores(): void {
    this._contadores.next();
  }
}
