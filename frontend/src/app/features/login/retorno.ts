import { ChangeDetectionStrategy, Component, OnInit, inject, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { AuthService, ErrorDeAutenticacion } from '../../core/auth/auth.service';
import { paginaDeInicio } from '../../core/auth/guards';

/** Recibe a la persona de vuelta de Keycloak (acceso institucional) y completa el inicio de sesión. */
@Component({
  selector: 'bp-retorno',
  imports: [RouterLink],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="centro">
      @if (error(); as mensaje) {
        <div class="aviso error" role="alert"><p>{{ mensaje }}</p></div>
        <a class="boton primario" routerLink="/login">Volver al acceso</a>
      } @else {
        <p>Completando tu acceso…</p>
      }
    </div>
  `,
  styles: `
    .centro {
      min-height: 100vh;
      display: flex;
      flex-direction: column;
      gap: 16px;
      align-items: center;
      justify-content: center;
      padding: 24px;
      color: var(--texto-suave);
    }
  `,
})
export class Retorno implements OnInit {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  private readonly ruta = inject(ActivatedRoute);

  protected readonly error = signal<string | null>(null);

  async ngOnInit(): Promise<void> {
    const parametros = this.ruta.snapshot.queryParamMap;
    const codigo = parametros.get('code');
    const estado = parametros.get('state');
    if (parametros.get('error') || !codigo || !estado) {
      this.error.set('No se completó el acceso institucional. Inténtalo de nuevo.');
      return;
    }
    try {
      await this.auth.completarAccesoInstitucional(codigo, estado);
      await this.router.navigateByUrl(paginaDeInicio(this.auth.usuario()?.roles ?? []), { replaceUrl: true });
    } catch (e) {
      this.error.set(e instanceof ErrorDeAutenticacion ? e.message : 'No se pudo completar el acceso.');
    }
  }
}
