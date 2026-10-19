/** Un JWT sin firma real (el cliente solo lee las reclamaciones; la validación la hacen el gateway y los servicios). */
export function tokenDePrueba(reclamaciones: Record<string, unknown>): string {
  const codificar = (valor: unknown) =>
    btoa(String.fromCharCode(...new TextEncoder().encode(JSON.stringify(valor))))
      .replace(/\+/g, '-')
      .replace(/\//g, '_')
      .replace(/=+$/, '');
  return `${codificar({ alg: 'none' })}.${codificar(reclamaciones)}.firma`;
}
