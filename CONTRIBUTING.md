# Cómo contribuir

1. Trabaja en una rama propia a partir de `corte-2` (o del corte en curso). No se trabaja sobre `main`.
2. Haz commits pequeños, con mensaje en español que diga qué cambia. Cada commit debe dejar el proyecto compilando.
3. Antes de abrir el Pull Request ejecuta las pruebas del módulo que tocaste:
   - Monolito: `cd monolito && mvn test`
   - Microservicios: `cd microservicios && ./mvnw test`
4. Abre el Pull Request con la plantilla y fusiónalo con «Create a merge commit» o «Rebase and merge» (no «Squash»), para
   que cada commit conserve a su autor.

Más detalle en [`docs/general/flujo-de-trabajo.md`](docs/general/flujo-de-trabajo.md) y
[`docs/general/convenciones.md`](docs/general/convenciones.md).
