# Pruebas de carga

`escalabilidad.js` es un script de [k6](https://k6.io/) que simula usuarios de los tres roles consultando y
escribiendo a través del gateway, con tokens reales de Keycloak. Se ejecuta con la imagen de Docker de k6, sin
instalar nada. El comando y la interpretación de los resultados están en
[`docs/corte-2/04-escalabilidad.md`](../../docs/corte-2/04-escalabilidad.md).
