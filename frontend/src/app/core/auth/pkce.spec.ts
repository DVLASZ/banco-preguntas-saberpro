import { aleatorio, desafio } from './pkce';

describe('pkce', () => {
  it('el desafío coincide con el vector de ejemplo del RFC 7636', async () => {
    expect(await desafio('dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk')).toBe('E9Melhoa2OwvFrEMTJguCHaoeK1t8URWbuGJSstw-cM');
  });

  it('genera cadenas aleatorias aptas para una URL, distintas cada vez', () => {
    const a = aleatorio();
    const b = aleatorio();

    expect(a).not.toBe(b);
    expect(a).toMatch(/^[A-Za-z0-9_-]+$/);
    expect(a.length).toBeGreaterThanOrEqual(43);
  });

  it('respeta el tamaño pedido', () => {
    expect(aleatorio(16).length).toBeLessThan(aleatorio(48).length);
  });
});
