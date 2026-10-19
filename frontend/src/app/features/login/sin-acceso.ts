import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from '../../core/auth/auth.service';
import { Icono } from '../../ui/icono';

/** La cuenta existe pero no tiene un rol del banco de preguntas. */
@Component({
  selector: 'bp-sin-acceso',
  imports: [Icono],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="centro">
      <bp-icono nombre="candado" [tamano]="40" />
      <h1>Tu cuenta aún no tiene acceso</h1>
      <p>Iniciaste sesión, pero tu usuario no tiene un rol asignado en el banco de preguntas. Pide a un administrador que te asigne uno.</p>
      <button type="button" class="boton" (click)="salir()">Cerrar sesión</button>
    </div>
  `,
  styles: `
    .centro {
      min-height: 100vh;
      display: flex;
      flex-direction: column;
      gap: 14px;
      align-items: center;
      justify-content: center;
      text-align: center;
      padding: 24px;
      color: var(--texto-suave);
      bp-icono { color: var(--ambar); }
      h1 { color: var(--texto); font-weight: 300; }
      p { max-width: 460px; }
    }
  `,
})
export class SinAcceso {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  protected async salir(): Promise<void> {
    await this.auth.cerrarSesion();
    await this.router.navigate(['/login']);
  }
}
