# Frontend · Banco de Preguntas Saber Pro

Aplicación web en **Angular 22** (componentes independientes, señales, sin zone.js) que habla solo con el API
Gateway y se autentica con Keycloak. Las pantallas siguen los prototipos de
[`docs/segundo-corte/06-prototipos.md`](../docs/corte-2/06-prototipos.md).

## Estructura

```
src/app/core/        Configuración en tiempo de ejecución, modelos, servicios de API y autenticación (PKCE, guardias, interceptor)
src/app/layout/      Marco de la aplicación (menú por rol, avisos)
src/app/ui/          Componentes visuales reutilizables (icono, insignia, avatar, paginación, avisos)
src/app/features/    Pantallas: login, preguntas, revisión, administración, historial, notificaciones
public/config.json   Direcciones del API y de Keycloak (se lee al arrancar; no hace falta recompilar para cambiarlas)
```

## Autenticación

*Authorization Code con PKCE* contra el realm `bancopreguntas` de Keycloak. El token se envía al gateway en
cada petición; las rutas y el menú dependen del rol (`ADMIN`, `AUTHOR`, `REVIEWER`) que trae el token, y el
servidor vuelve a comprobarlo.

## Ejecutar

Con toda la pila (recomendado), desde `microservicios/`: `docker compose up -d --build` y abrir
http://localhost:4200.

En desarrollo, con el resto de la pila en Docker:

```bash
npm install
npx ng serve        # http://localhost:4200
```

## Pruebas

```bash
npx ng test --watch=false    # 162 pruebas con Vitest
npx ng build                 # compilación de producción
```

Las pruebas cubren los servicios de API, la autenticación (PKCE, guardias, interceptor), los componentes de
interfaz y las pantallas de cada rol.
