/** Ayudas del flujo «código de autorización con PKCE» (RFC 7636). */

function aBase64Url(bytes: Uint8Array): string {
  let texto = '';
  bytes.forEach((b) => (texto += String.fromCharCode(b)));
  return btoa(texto).replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/, '');
}

/** Cadena aleatoria segura, apta como `code_verifier` o `state`. */
export function aleatorio(bytes = 48): string {
  return aBase64Url(crypto.getRandomValues(new Uint8Array(bytes)));
}

/** `code_challenge` = BASE64URL(SHA-256(verifier)). */
export async function desafio(verificador: string): Promise<string> {
  const resumen = await crypto.subtle.digest('SHA-256', new TextEncoder().encode(verificador));
  return aBase64Url(new Uint8Array(resumen));
}
