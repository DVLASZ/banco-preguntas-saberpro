import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';

/** Trazos (24×24, línea) de los iconos de la aplicación. */
const TRAZOS: Record<string, string[]> = {
  casa: ['M3 11l9-8 9 8', 'M5 10v10h5v-6h4v6h5V10'],
  preguntas: ['M8 7V4h12v13h-3', 'M4 7h12v13H4z'],
  mas: ['M12 5v14', 'M5 12h14'],
  'mas-circulo': ['M12 8v8', 'M8 12h8', 'M12 21a9 9 0 100-18 9 9 0 000 18z'],
  usuarios: ['M9 11a4 4 0 100-8 4 4 0 000 8z', 'M2 21v-1a6 6 0 016-6h2a6 6 0 016 6v1', 'M17 3.5a4 4 0 010 7.5', 'M22 21v-1a6 6 0 00-4-5.6'],
  bandeja: ['M9 4h6v3H9z', 'M7 5H5a1 1 0 00-1 1v14a1 1 0 001 1h14a1 1 0 001-1V6a1 1 0 00-1-1h-2', 'M8.5 14l2.5 2.5 4.5-5'],
  campana: ['M6 8a6 6 0 1112 0c0 7 3 8 3 8H3s3-1 3-8', 'M10 21a2 2 0 004 0'],
  salir: ['M9 21H5a1 1 0 01-1-1V4a1 1 0 011-1h4', 'M16 17l5-5-5-5', 'M21 12H9'],
  entrar: ['M5 12h14', 'M13 6l6 6-6 6'],
  libro: ['M2 5h7a3 3 0 013 3v13a2 2 0 00-2-2H2z', 'M22 5h-7a3 3 0 00-3 3v13a2 2 0 012-2h8z'],
  lapiz: ['M4 20h4L19 9a2.8 2.8 0 00-4-4L4 16z', 'M13.5 6.5l4 4'],
  editar: ['M4 20h4L19 9a2.8 2.8 0 00-4-4L4 16z', 'M13.5 6.5l4 4'],
  reloj: ['M12 21a9 9 0 100-18 9 9 0 000 18z', 'M12 7v5l3 2'],
  'escudo-ok': ['M12 3l8 3v6c0 5-3.5 8-8 9-4.5-1-8-4-8-9V6z', 'M8.5 12l2.5 2.5 4.5-5'],
  escudo: ['M12 3l8 3v6c0 5-3.5 8-8 9-4.5-1-8-4-8-9V6z'],
  alerta: ['M12 21a9 9 0 100-18 9 9 0 000 18z', 'M12 8v5', 'M12 16h.01'],
  info: ['M12 21a9 9 0 100-18 9 9 0 000 18z', 'M12 11v6', 'M12 8h.01'],
  enviar: ['M21 3L3 10l7 3 3 7z', 'M10 13l11-10'],
  guardar: ['M5 3h11l4 4v13a1 1 0 01-1 1H5a1 1 0 01-1-1V4a1 1 0 011-1z', 'M8 3v6h8V3', 'M8 21v-7h8v7'],
  mensaje: ['M4 5h16v11H9l-5 4z', 'M8 9.5h8', 'M8 12.5h5'],
  publicar: ['M12 16V4', 'M7 9l5-5 5 5', 'M4 20h16'],
  flujo: ['M6 3v12', 'M6 15a3 3 0 103 3', 'M18 9a3 3 0 100-6 3 3 0 000 6z', 'M18 9a9 9 0 01-9 9'],
  clip: ['M9 4h6v3H9z', 'M7 5H5a1 1 0 00-1 1v14a1 1 0 001 1h14a1 1 0 001-1V6a1 1 0 00-1-1h-2', 'M8 12h8', 'M8 16h5'],
  check: ['M5 12.5l4.5 4.5L19 7.5'],
  'doble-check': ['M2 12.5l4.5 4.5L15 8', 'M12 15l1.5 1.5L22 8'],
  cerrar: ['M6 6l12 12', 'M18 6L6 18'],
  buscar: ['M11 18a7 7 0 100-14 7 7 0 000 14z', 'M21 21l-5-5'],
  'flecha-izq': ['M19 12H5', 'M11 6l-6 6 6 6'],
  'flecha-der': ['M5 12h14', 'M13 6l6 6-6 6'],
  'flecha-arriba-der': ['M7 17L17 7', 'M8 7h9v9'],
  'chevron-izq': ['M15 6l-6 6 6 6'],
  'chevron-der': ['M9 6l6 6-6 6'],
  'chevron-abajo': ['M6 9l6 6 6-6'],
  candado: ['M5 11h14v10H5z', 'M8 11V7a4 4 0 018 0v4'],
  edificio: ['M4 21V4a1 1 0 011-1h8a1 1 0 011 1v17', 'M14 9h5a1 1 0 011 1v11', 'M3 21h18', 'M8 7h2', 'M8 11h2', 'M8 15h2'],
  birrete: ['M2 9l10-5 10 5-10 5z', 'M6 11.5V16c0 1.5 3 3 6 3s6-1.5 6-3v-4.5'],
  bombilla: ['M9 18h6', 'M10 21h4', 'M12 3a6 6 0 00-3.5 10.9c.6.5 1 1.2 1 2.1h5c0-.9.4-1.6 1-2.1A6 6 0 0012 3z'],
  ojo: ['M2 12s3.5-7 10-7 10 7 10 7-3.5 7-10 7S2 12 2 12z', 'M12 15a3 3 0 100-6 3 3 0 000 6z'],
  correo: ['M3 6h18v12H3z', 'M3 7l9 6 9-6'],
  ordenar: ['M4 6h12', 'M4 12h8', 'M4 18h4', 'M18 8v10', 'M15 15l3 3 3-3'],
  puntos: ['M5 12h.01', 'M12 12h.01', 'M19 12h.01'],
  nube: ['M7 18a4 4 0 01-.6-8A6 6 0 0118 11a3.5 3.5 0 011 6.9', 'M9 18h9'],
};

/** Icono de línea en SVG, del color del texto que lo rodea. */
@Component({
  selector: 'bp-icono',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <svg
      [attr.width]="tamano()"
      [attr.height]="tamano()"
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      stroke-width="1.7"
      stroke-linecap="round"
      stroke-linejoin="round"
      aria-hidden="true"
    >
      @for (d of trazos(); track $index) {
        <path [attr.d]="d" />
      }
    </svg>
  `,
  styles: `
    :host {
      display: inline-flex;
      flex: none;
      line-height: 0;
    }
  `,
})
export class Icono {
  readonly nombre = input.required<string>();
  readonly tamano = input(18);
  protected readonly trazos = computed(() => TRAZOS[this.nombre()] ?? []);
}
