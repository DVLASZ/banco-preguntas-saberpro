import { DOCUMENT } from '@angular/common';
import { Injectable, inject } from '@angular/core';

/** Cambia de página del navegador. Se aísla para poder sustituirlo en las pruebas. */
@Injectable({ providedIn: 'root' })
export class Redirector {
  private readonly ventana = inject(DOCUMENT).defaultView;

  ir(url: string): void {
    this.ventana?.location.assign(url);
  }

  get origen(): string {
    return this.ventana?.location.origin ?? '';
  }
}
