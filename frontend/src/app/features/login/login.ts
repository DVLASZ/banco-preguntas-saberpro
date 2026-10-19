import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { AuthService, ErrorDeAutenticacion } from '../../core/auth/auth.service';
import { paginaDeInicio } from '../../core/auth/guards';
import { Icono } from '../../ui/icono';

/** Pantalla de acceso (V2-01): formulario de la aplicación o acceso institucional por Keycloak. */
@Component({
  selector: 'bp-login',
  imports: [Icono],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './login.html',
  styleUrl: './login.scss',
})
export class Login {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  private readonly ruta = inject(ActivatedRoute);

  protected readonly usuario = signal('');
  protected readonly clave = signal('');
  protected readonly enviando = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly intento = signal(false);

  protected readonly expirada = computed(() => this.ruta.snapshot.queryParamMap.get('expirada') === '1');
  protected readonly usuarioVacio = computed(() => this.intento() && this.usuario().trim() === '');
  protected readonly claveVacia = computed(() => this.intento() && this.clave() === '');

  protected async entrar(evento: Event): Promise<void> {
    evento.preventDefault();
    this.intento.set(true);
    this.error.set(null);
    if (this.usuario().trim() === '' || this.clave() === '') {
      return;
    }
    this.enviando.set(true);
    try {
      await this.auth.iniciarSesion(this.usuario(), this.clave());
      await this.irAlInicio();
    } catch (e) {
      this.error.set(e instanceof ErrorDeAutenticacion ? e.message : 'No se pudo iniciar sesión. Inténtalo de nuevo.');
      this.clave.set('');
    } finally {
      this.enviando.set(false);
    }
  }

  protected accesoInstitucional(): void {
    void this.auth.iniciarAccesoInstitucional();
  }

  private async irAlInicio(): Promise<void> {
    const destino = this.ruta.snapshot.queryParamMap.get('volverA');
    // Solo se acepta una ruta propia de la aplicación: nunca una dirección externa.
    if (destino && destino.startsWith('/') && !destino.startsWith('//')) {
      await this.router.navigateByUrl(destino);
    } else {
      await this.router.navigateByUrl(paginaDeInicio(this.auth.usuario()?.roles ?? []));
    }
  }
}
