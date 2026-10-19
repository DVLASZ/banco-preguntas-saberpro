import { bootstrapApplication } from '@angular/platform-browser';
import { App } from './app/app';
import { appConfig } from './app/app.config';
import { APP_CONFIG, cargarConfig } from './app/core/config';

// Las direcciones del sistema se leen de config.json antes de arrancar: la misma compilación sirve en cualquier entorno.
cargarConfig().then((config) =>
  bootstrapApplication(App, {
    ...appConfig,
    providers: [...appConfig.providers, { provide: APP_CONFIG, useValue: config }],
  }).catch((error) => console.error(error)),
);
